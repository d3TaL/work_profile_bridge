package dev.profilebridge.core.tls

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class ProbeResult { REACHABLE, UNAVAILABLE, TLS_REJECTED, NOT_PERMITTED }

class TlsProbe {
    suspend fun listen(identity: TlsIdentity, diagnostic: DiagnosticAttempt = DiagnosticAttempt(DiagnosticSide.SERVER),
                       connectionLimit: Int = 32): TlsProbeSession {
        var opened: TlsProbeSession? = null
        try {
            return withContext(Dispatchers.IO) {
                diagnostic.listener = ListenerState.STARTING
                diagnostic.setProvider(identity.context.provider.name)
                val server = identity.context.serverSocketFactory.createServerSocket() as SSLServerSocket
                try {
                    server.enabledProtocols = arrayOf("TLSv1.3")
                    server.needClientAuth = false
                    server.soTimeout = 500
                    diagnostic.operation(DiagnosticStage.BIND) { server.bind(InetSocketAddress(LOOPBACK, 0), 1) }
                    TlsProbeSession(server, PairingCode.create(server.localPort, identity.certificatePin()),
                        diagnostic, connectionLimit).also {
                        opened = it
                        diagnostic.listener = ListenerState.LISTENING
                        diagnostic.record(DiagnosticStage.LISTEN, DiagnosticStatus.SUCCESS)
                    }
                } catch (error: Exception) { server.close(); throw error }
            }
        } catch (error: Exception) {
            opened?.close()
            diagnostic.failure(error)
            throw error
        }
    }

    suspend fun ping(code: PairingCode, diagnostic: DiagnosticAttempt = DiagnosticAttempt(DiagnosticSide.CLIENT)):
        ProbeResult = withContext(Dispatchers.IO) {
        try {
            val context = diagnostic.operation(DiagnosticStage.TLS_CONTEXT_INIT) {
                TlsContexts.client(code.certificatePin(), diagnostic)
            }
            diagnostic.setProvider(context.provider.name)
            (context.socketFactory.createSocket() as SSLSocket).use { socket ->
                // Closing the socket on cancellation interrupts blocking connect/handshake/read immediately.
                withSocketCancellation({ socket.close() }) {
                    socket.enabledProtocols = arrayOf("TLSv1.3")
                    socket.soTimeout = 2000
                    diagnostic.operation(DiagnosticStage.TCP_CONNECT) { socket.connect(InetSocketAddress(LOOPBACK, code.port), 2000) }
                    diagnostic.operation(DiagnosticStage.TLS_HANDSHAKE) { socket.startHandshake() }
                    diagnostic.protocol = if (socket.session.protocol == "TLSv1.3") TlsVersion.TLS_1_3 else TlsVersion.OTHER
                    currentCoroutineContext().ensureActive()
                    diagnostic.operation(DiagnosticStage.PROBE_WRITE) {
                        val token = code.tokenCopy()
                        try { DataOutputStream(socket.outputStream).apply { writeInt(REQUEST); write(token); flush() } }
                        finally { token.fill(0) }
                    }
                    val response = diagnostic.operation(DiagnosticStage.PROBE_READ) { DataInputStream(socket.inputStream).readInt() }
                    if (response == RESPONSE) {
                        diagnostic.record(DiagnosticStage.COMPLETE, DiagnosticStatus.SUCCESS)
                        ProbeResult.REACHABLE
                    } else {
                        diagnostic.record(DiagnosticStage.PROBE_READ, DiagnosticStatus.FAILURE, DiagnosticReason.UNEXPECTED_RESPONSE)
                        ProbeResult.UNAVAILABLE
                    }
                }
            }
        } catch (error: Exception) {
            try { currentCoroutineContext().ensureActive() }
            catch (cancelled: CancellationException) { diagnostic.failure(cancelled); throw cancelled }
            diagnostic.failure(error)
            when (diagnosticReason(error)) {
                DiagnosticReason.ACCESS_DENIED -> ProbeResult.NOT_PERMITTED
                DiagnosticReason.PIN_MISMATCH, DiagnosticReason.CERT_EXPIRED, DiagnosticReason.CERT_NOT_YET_VALID,
                DiagnosticReason.CERT_REJECTED, DiagnosticReason.HANDSHAKE_FAILED,
                DiagnosticReason.TLS_PROTOCOL_FAILURE, DiagnosticReason.INVALID_CONFIGURATION,
                DiagnosticReason.IDENTITY_OR_CONTEXT_FAILURE -> ProbeResult.TLS_REJECTED
                else -> ProbeResult.UNAVAILABLE
            }
        }
    }

    suspend fun selfCheck(identity: TlsIdentity, client: DiagnosticAttempt, server: DiagnosticAttempt): ProbeResult =
        coroutineScope {
            val session = listen(identity, server, connectionLimit = 1)
            val serving = async { session.serve() }
            try { withTimeout(8000) { ping(session.code, client) } }
            finally { session.close(); serving.cancelAndJoin() }
        }

    companion object {
        internal const val REQUEST = 0x504C4232
        internal const val RESPONSE = 0x504C424F
        private val LOOPBACK = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
    }
}

class TlsProbeSession internal constructor(
    private val server: SSLServerSocket,
    val code: PairingCode,
    private val diagnostic: DiagnosticAttempt,
    private val connectionLimit: Int,
) : AutoCloseable {
    private val closed = AtomicBoolean()
    private val activeClient = AtomicReference<SSLSocket?>()
    private val createdNanos = System.nanoTime()
    val remainingSeconds: Long get() = ((120_000_000_000L - (System.nanoTime() - createdNanos))
        .coerceAtLeast(0) + 999_999_999) / 1_000_000_000
    private fun valid(): Boolean = !closed.get() && System.nanoTime() - createdNanos < 120_000_000_000L

    /** Deliberately only a fixed ping: never accept URL, package or commands here. */
    suspend fun serve(): Unit = withContext(Dispatchers.IO) {
        var attempts = 0
        withSocketCancellation({ close(DiagnosticReason.CANCELLED) }) {
          try {
            diagnostic.record(DiagnosticStage.TCP_ACCEPT)
            while (valid() && attempts < connectionLimit) {
                currentCoroutineContext().ensureActive()
                val socket = try { server.accept() as SSLSocket }
                catch (_: SocketTimeoutException) { continue }
                activeClient.set(socket)
                socket.use {
                    attempts++
                    try {
                        diagnostic.record(DiagnosticStage.TCP_ACCEPT, DiagnosticStatus.SUCCESS)
                        if (!valid()) return@use
                        socket.soTimeout = 2000
                        diagnostic.operation(DiagnosticStage.TLS_HANDSHAKE) { socket.startHandshake() }
                        diagnostic.protocol = if (socket.session.protocol == "TLSv1.3") TlsVersion.TLS_1_3 else TlsVersion.OTHER
                        val allowed = diagnostic.operation(DiagnosticStage.PROBE_READ) {
                            val input = DataInputStream(socket.inputStream)
                            if (input.readInt() != TlsProbe.REQUEST) false else {
                                val token = ByteArray(PairingCode.SECRET_BYTES)
                                try { input.readFully(token); code.accepts(token) } finally { token.fill(0) }
                            }
                        }
                        currentCoroutineContext().ensureActive()
                        if (allowed && valid()) {
                            diagnostic.operation(DiagnosticStage.PROBE_WRITE) {
                                DataOutputStream(socket.outputStream).apply { writeInt(TlsProbe.RESPONSE); flush() }
                            }
                            diagnostic.record(DiagnosticStage.COMPLETE, DiagnosticStatus.SUCCESS)
                        } else diagnostic.record(DiagnosticStage.PROBE_READ, DiagnosticStatus.FAILURE, DiagnosticReason.AUTH_REJECTED)
                    } catch (error: Exception) {
                        currentCoroutineContext().ensureActive()
                        diagnostic.failure(error)
                    } finally { activeClient.compareAndSet(socket, null) }
                }
                if (valid() && attempts < connectionLimit) diagnostic.record(DiagnosticStage.TCP_ACCEPT)
            }
            if (!closed.get()) close(if (remainingSeconds == 0L) DiagnosticReason.EXPIRED else DiagnosticReason.LIMIT_REACHED)
        } catch (error: Exception) {
            if (!closed.get()) diagnostic.failure(error)
            currentCoroutineContext().ensureActive()
          } finally { close() }
        }
    }

    override fun close() = close(DiagnosticReason.USER_STOP)

    private fun close(reason: DiagnosticReason) {
        if (closed.compareAndSet(false, true)) {
            diagnostic.listener = if (reason == DiagnosticReason.EXPIRED) ListenerState.EXPIRED else ListenerState.CLOSED
            diagnostic.record(if (reason == DiagnosticReason.EXPIRED) DiagnosticStage.SESSION_EXPIRED else DiagnosticStage.STOPPED,
                DiagnosticStatus.SUCCESS, reason)
            try { activeClient.getAndSet(null)?.close() } catch (_: IOException) { }
            try { server.close() } catch (_: IOException) { }
            finally { code.destroy() }
        }
    }
}

/** Public coroutine APIs only; cancellation closes blocking sockets from a separate coroutine. */
private suspend fun <T> withSocketCancellation(close: () -> Unit, action: suspend () -> T): T = coroutineScope {
    val guard = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() } finally { runCatching(close) }
    }
    try { action() } finally { guard.cancel() }
}
