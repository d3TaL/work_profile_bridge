package dev.profilebridge.core.tls

import java.io.IOException
import java.net.InetSocketAddress
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class TlsProbeTest {
    private fun identity(): TlsIdentity {
        val store = KeyStore.getInstance("PKCS12")
        javaClass.getResourceAsStream("/test-only.p12")!!.use { store.load(it, "fixture-only".toCharArray()) }
        return TlsContexts.server(store.getKey("probe-test", "fixture-only".toCharArray()) as PrivateKey,
            store.getCertificate("probe-test") as X509Certificate)
    }
    private fun exercise(check: suspend (TlsProbe, TlsProbeSession) -> Unit) = runBlocking {
        withTimeout(10_000) {
            val probe = TlsProbe()
            val session = probe.listen(identity())
            val server = async(Dispatchers.IO) { session.serve() }
            try { check(probe, session) } finally { session.close(); server.cancelAndJoin() }
        }
    }
    @Test fun `real pinned TLS connection succeeds`() = exercise { probe, session ->
        assertEquals(ProbeResult.REACHABLE, probe.ping(session.code))
    }
    @Test fun `wrong certificate pin is rejected before token delivery`() = exercise { probe, session ->
        assertEquals(ProbeResult.TLS_REJECTED, probe.ping(PairingCode.create(session.code.port, ByteArray(32))))
        assertEquals(ProbeResult.REACHABLE, probe.ping(session.code))
    }
    @Test fun `correct pin but wrong token has no acknowledgement`() = exercise { probe, session ->
        assertEquals(ProbeResult.UNAVAILABLE, probe.ping(PairingCode.create(session.code.port, session.code.certificatePin())))
        assertEquals(ProbeResult.REACHABLE, probe.ping(session.code))
    }
    @Test fun `TLS twelve downgrade is refused`() = exercise { probe, session ->
        withContext(Dispatchers.IO) {
            (TlsContexts.client(session.code.certificatePin()).socketFactory.createSocket() as SSLSocket).use { socket ->
                socket.enabledProtocols = arrayOf("TLSv1.2")
                socket.soTimeout = 2000
                socket.connect(InetSocketAddress("127.0.0.1", session.code.port), 2000)
                try { socket.startHandshake(); fail("Unexpected protocol downgrade") } catch (_: IOException) { }
            }
        }
        assertEquals(ProbeResult.REACHABLE, probe.ping(session.code))
    }
    @Test fun `closed session cannot be contacted`() = runBlocking {
        val probe = TlsProbe(); val session = probe.listen(identity())
        val code = PairingCode.parse(session.code.encodeForManualTransfer())!!
        session.close(); session.close()
        assertEquals(ProbeResult.UNAVAILABLE, probe.ping(code))
    }
    @Test fun `wrong pin diagnostics never reach token write`() = exercise { probe, session ->
        val events = mutableListOf<DiagnosticEvent>()
        val wrong = PairingCode.create(session.code.port, ByteArray(32))
        try {
            assertEquals(ProbeResult.TLS_REJECTED, probe.ping(wrong, DiagnosticAttempt(DiagnosticSide.CLIENT, events::add)))
            assertTrue(events.any { it.reason == DiagnosticReason.PIN_MISMATCH })
            assertFalse(events.any { it.stage == DiagnosticStage.PROBE_WRITE })
        } finally { wrong.destroy() }
    }
    @Test fun `self check uses pinned fixed ping and closes listener`() = runBlocking {
        withTimeout(10_000) {
            val events = java.util.Collections.synchronizedList(mutableListOf<DiagnosticEvent>())
            val probe = TlsProbe()
            assertEquals(ProbeResult.REACHABLE, probe.selfCheck(identity(),
                DiagnosticAttempt(DiagnosticSide.SELF_CHECK, events::add), DiagnosticAttempt(DiagnosticSide.SERVER, events::add)))
            assertTrue(events.any { it.stage == DiagnosticStage.COMPLETE && it.side == DiagnosticSide.SELF_CHECK })
            assertTrue(events.any { it.stage == DiagnosticStage.STOPPED })
        }
    }
    @Test fun `cancelling listener interrupts blocked peer handshake`() = runBlocking {
        withTimeout(5000) {
            val probe = TlsProbe()
            val accepted = kotlinx.coroutines.CompletableDeferred<Unit>()
            val diagnostic = DiagnosticAttempt(DiagnosticSide.SERVER) {
                if (it.stage == DiagnosticStage.TLS_HANDSHAKE && it.status == DiagnosticStatus.STARTED) accepted.complete(Unit)
            }
            val session = probe.listen(identity(), diagnostic)
            val server = async(Dispatchers.IO) { session.serve() }
            try {
                withContext(Dispatchers.IO) {
                    java.net.Socket("127.0.0.1", session.code.port).use { socket ->
                        // No TLS bytes: the listener blocks in handshake until cancellation.
                        accepted.await()
                        server.cancelAndJoin()
                        socket.soTimeout = 1000
                        // The provider may send a TLS close alert before EOF.
                        while (socket.getInputStream().read() != -1) { }
                    }
                }
            } finally { session.close(); server.cancelAndJoin() }
        }
    }
}
