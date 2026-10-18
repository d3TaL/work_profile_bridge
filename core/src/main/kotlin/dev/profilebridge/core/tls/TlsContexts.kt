package dev.profilebridge.core.tls

import java.net.Socket
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

class TlsIdentity(val context: SSLContext, certificatePin: ByteArray) {
    private val pin = certificatePin.copyOf()
    init { require(pin.size == 32) }
    fun certificatePin(): ByteArray = pin.copyOf()
    override fun toString(): String = "TlsIdentity([REDACTED])"
}

object TlsContexts {
    fun server(key: PrivateKey, certificate: X509Certificate, diagnostic: DiagnosticAttempt? = null): TlsIdentity {
        certificate.checkValidity()
        val context = SSLContext.getInstance("TLS")
        context.init(arrayOf(SingleKeyManager(key, certificate, diagnostic)), null, null)
        return TlsIdentity(context, fingerprint(certificate))
    }

    fun client(pin: ByteArray, diagnostic: DiagnosticAttempt? = null): SSLContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(PinnedTrustManager(pin, diagnostic)), null)
    }

    fun fingerprint(certificate: X509Certificate): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
}

/** Pin-only trust for the explicitly selected local peer, never for arbitrary HTTPS browsing. */
private class PinnedTrustManager(pin: ByteArray, private val diagnostic: DiagnosticAttempt?) : X509TrustManager {
    private val expected = pin.copyOf()
    init { require(expected.size == 32) }

    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {
        diagnostic?.record(DiagnosticStage.PEER_VALIDATE)
        try {
            if (chain.isNullOrEmpty() || chain.size > 4 || authType.isNullOrBlank()) {
                throw PeerValidationException(DiagnosticReason.CERT_REJECTED)
            }
            val leaf = chain[0]
            try { leaf.checkValidity() }
            catch (_: CertificateExpiredException) { throw PeerValidationException(DiagnosticReason.CERT_EXPIRED) }
            catch (_: CertificateNotYetValidException) { throw PeerValidationException(DiagnosticReason.CERT_NOT_YET_VALID) }
            if (!MessageDigest.isEqual(expected, TlsContexts.fingerprint(leaf))) {
                throw PeerValidationException(DiagnosticReason.PIN_MISMATCH)
            }
            diagnostic?.record(DiagnosticStage.PEER_VALIDATE, DiagnosticStatus.SUCCESS)
        } catch (error: CertificateException) {
            diagnostic?.record(DiagnosticStage.PEER_VALIDATE, DiagnosticStatus.FAILURE,
                (error as? PeerValidationException)?.reason ?: DiagnosticReason.CERT_REJECTED)
            throw error
        }
    }
    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {
        throw CertificateException("UNSUPPORTED_CLIENT_CERTIFICATE")
    }
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

private class SingleKeyManager(
    private val key: PrivateKey,
    private val certificate: X509Certificate,
    private val diagnostic: DiagnosticAttempt?,
) : X509ExtendedKeyManager() {
    private fun alias(keyType: String?): String? {
        val selected = keyType == certificate.publicKey.algorithm
        diagnostic?.keySelection(keyType, selected)
        return if (selected) "identity" else null
    }
    override fun getPrivateKey(alias: String?): PrivateKey? {
        if (alias != "identity") return null
        diagnostic?.keyAccess(chain = false)
        return key
    }
    override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
        if (alias != "identity") return null
        diagnostic?.keyAccess(chain = true)
        return arrayOf(certificate)
    }
    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? =
        alias(keyType)?.let { arrayOf(it) }
    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = alias(keyType)
    override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String? = alias(keyType)
    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null
    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String? = null
}
