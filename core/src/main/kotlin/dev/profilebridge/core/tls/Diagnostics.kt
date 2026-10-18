package dev.profilebridge.core.tls

import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.util.UUID
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException

enum class DiagnosticSide { CLIENT, SERVER, SELF_CHECK }
enum class DiagnosticStage {
    IDENTITY_LOAD, KEY_SIGN_CHECK, KEY_RAW_SIGN_CHECK, TLS_CONTEXT_INIT, BIND, LISTEN, TCP_CONNECT, TCP_ACCEPT,
    TLS_HANDSHAKE, PEER_VALIDATE, PROBE_WRITE, PROBE_READ, COMPLETE, SESSION_EXPIRED, STOPPED,
}
enum class DiagnosticStatus { STARTED, SUCCESS, FAILURE, UNKNOWN }
enum class DiagnosticReason {
    NONE, CONNECTION_FAILED, TIMEOUT, ACCESS_DENIED, IDENTITY_OR_CONTEXT_FAILURE,
    PIN_MISMATCH, CERT_EXPIRED, CERT_NOT_YET_VALID, CERT_REJECTED, HANDSHAKE_FAILED,
    TLS_PROTOCOL_FAILURE, INVALID_CONFIGURATION, PEER_CLOSED, CONNECTION_INTERRUPTED,
    UNEXPECTED_RESPONSE, AUTH_REJECTED, CANCELLED, UNKNOWN_FAILURE, EXPIRED, USER_STOP, LIMIT_REACHED,
}
enum class ListenerState { NONE, STARTING, LISTENING, CLOSED, EXPIRED }
enum class TlsProvider { CONSCRYPT, ANDROID_OPENSSL, SUN_JSSE, OTHER }
enum class TlsVersion { TLS_1_3, OTHER }
enum class KeyRequest { NONE, EC, RSA, OTHER }
enum class ExceptionCategory { HANDSHAKE, TLS, INVALID_KEY, SIGNATURE, PROVIDER, SECURITY, CERTIFICATE, IO, CONFIGURATION, OTHER }

/** Only closed enum fields and a locally generated ID can enter an export. */
data class DiagnosticEvent(
    val attemptId: String,
    val side: DiagnosticSide,
    val stage: DiagnosticStage,
    val elapsedMillis: Long,
    val status: DiagnosticStatus,
    val reason: DiagnosticReason,
    val listener: ListenerState,
    val provider: TlsProvider?,
    val protocol: TlsVersion?,
    val keyRequest: KeyRequest = KeyRequest.NONE,
    val keySelected: Boolean = false,
    val privateKeyRequested: Boolean = false,
    val chainRequested: Boolean = false,
    val causes: List<ExceptionCategory> = emptyList(),
)

class DiagnosticAttempt(
    val side: DiagnosticSide,
    private val emit: (DiagnosticEvent) -> Unit = {},
) {
    private val id = UUID.randomUUID().toString()
    private val started = System.nanoTime()
    var stage: DiagnosticStage = DiagnosticStage.TLS_CONTEXT_INIT
        private set
    @Volatile var listener: ListenerState = ListenerState.NONE
    var provider: TlsProvider? = null
    var protocol: TlsVersion? = null
    private var keyRequest = KeyRequest.NONE
    private var keySelected = false
    private var privateKeyRequested = false
    private var chainRequested = false
    private var causes: List<ExceptionCategory> = emptyList()

    @Synchronized fun record(next: DiagnosticStage, status: DiagnosticStatus = DiagnosticStatus.STARTED,
               reason: DiagnosticReason = DiagnosticReason.NONE) {
        stage = next
        emit(DiagnosticEvent(id, side, next, (System.nanoTime() - started) / 1_000_000,
            status, reason, listener, provider, protocol, keyRequest, keySelected,
            privateKeyRequested, chainRequested, causes.toList()))
    }

    @Synchronized fun failure(error: Exception) {
        causes = exceptionCategories(error)
        record(stage, DiagnosticStatus.FAILURE, diagnosticReason(error))
    }

    @Synchronized fun keySelection(type: String?, selected: Boolean) {
        keyRequest = when (type) { "EC" -> KeyRequest.EC; "RSA" -> KeyRequest.RSA; else -> KeyRequest.OTHER }
        keySelected = keySelected || selected
        record(stage)
    }
    @Synchronized fun keyAccess(chain: Boolean) {
        if (chain) chainRequested = true else privateKeyRequested = true
        record(stage)
    }

    inline fun <T> operation(next: DiagnosticStage, action: () -> T): T {
        record(next)
        return action().also { record(next, DiagnosticStatus.SUCCESS) }
    }

    fun setProvider(name: String) {
        provider = when (name) {
            "Conscrypt" -> TlsProvider.CONSCRYPT
            "AndroidOpenSSL" -> TlsProvider.ANDROID_OPENSSL
            "SunJSSE" -> TlsProvider.SUN_JSSE
            else -> TlsProvider.OTHER
        }
    }
}

internal fun exceptionCategories(error: Throwable): List<ExceptionCategory> {
    val result = mutableListOf<ExceptionCategory>()
    var cause: Throwable? = error
    repeat(8) {
        val current = cause ?: return result.toList()
        result.add(when (current) {
            is SSLHandshakeException -> ExceptionCategory.HANDSHAKE
            is SSLException -> ExceptionCategory.TLS
            is java.security.InvalidKeyException -> ExceptionCategory.INVALID_KEY
            is java.security.SignatureException -> ExceptionCategory.SIGNATURE
            is ProviderException -> ExceptionCategory.PROVIDER
            is java.security.cert.CertificateException -> ExceptionCategory.CERTIFICATE
            is GeneralSecurityException, is SecurityException -> ExceptionCategory.SECURITY
            is IOException -> ExceptionCategory.IO
            is IllegalArgumentException -> ExceptionCategory.CONFIGURATION
            else -> ExceptionCategory.OTHER
        })
        cause = current.cause
    }
    return result.toList()
}

/** Certificate verifier failures are typed, never recovered by parsing an exception message. */
internal class PeerValidationException(val reason: DiagnosticReason) : java.security.cert.CertificateException()

fun diagnosticReason(error: Exception): DiagnosticReason {
    var cause: Throwable? = error
    repeat(8) {
        if (cause is PeerValidationException) return cause.reason
        cause = cause?.cause
    }
    return when (error) {
        is CancellationException -> DiagnosticReason.CANCELLED
        is SocketTimeoutException -> DiagnosticReason.TIMEOUT
        is SecurityException -> DiagnosticReason.ACCESS_DENIED
        is ConnectException -> DiagnosticReason.CONNECTION_FAILED
        is SSLHandshakeException -> DiagnosticReason.HANDSHAKE_FAILED
        is SSLException -> DiagnosticReason.TLS_PROTOCOL_FAILURE
        is GeneralSecurityException, is ProviderException -> DiagnosticReason.IDENTITY_OR_CONTEXT_FAILURE
        is IllegalArgumentException -> DiagnosticReason.INVALID_CONFIGURATION
        is EOFException -> DiagnosticReason.PEER_CLOSED
        is SocketException -> DiagnosticReason.CONNECTION_INTERRUPTED
        is IOException -> DiagnosticReason.UNKNOWN_FAILURE
        else -> DiagnosticReason.UNKNOWN_FAILURE
    }
}

/** In-memory bounded journal. Neither secrets nor arbitrary platform strings are accepted. */
class DiagnosticJournal {
    private val attempts = linkedMapOf<String, MutableList<DiagnosticEvent>>()
    @Synchronized fun add(event: DiagnosticEvent) {
        val events = attempts.getOrPut(event.attemptId) { mutableListOf() }
        events.add(event)
        if (events.size > 64) events.removeAt(0)
        while (attempts.size > 10) attempts.remove(attempts.keys.first())
    }
    @Synchronized fun report(): String = buildString {
        appendLine("Profile Link Bridge diagnostic report v1")
        appendLine("Attempt IDs are local; correlate reports manually by direction.")
        attempts.values.forEach { events -> events.forEach { event ->
            appendLine("${event.attemptId} ${event.side} ${event.elapsedMillis}ms ${event.stage} " +
                "${event.status} ${event.reason} listener=${event.listener} " +
                "provider=${event.provider ?: "UNKNOWN"} tls=${event.protocol ?: "UNKNOWN"} " +
                "keyRequest=${event.keyRequest} selected=${event.keySelected} " +
                "keyAccess=${event.privateKeyRequested} chainAccess=${event.chainRequested} causes=${event.causes}")
        } }
    }
}
