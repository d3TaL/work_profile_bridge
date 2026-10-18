package dev.profilebridge.core.tls

import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.*
import org.junit.Test

class DiagnosticsTest {
    @Test fun `error categories do not infer pin mismatch from text`() {
        assertEquals(DiagnosticReason.HANDSHAKE_FAILED, diagnosticReason(SSLHandshakeException("PIN_MISMATCH secret")))
        assertEquals(DiagnosticReason.TIMEOUT, diagnosticReason(SocketTimeoutException("private address")))
        assertEquals(DiagnosticReason.CONNECTION_FAILED, diagnosticReason(ConnectException("refused")))
        assertEquals(DiagnosticReason.INVALID_CONFIGURATION, diagnosticReason(IllegalArgumentException("secret")))
    }
    @Test fun `journal is bounded and excludes arbitrary exception payload`() {
        val journal = DiagnosticJournal()
        repeat(12) {
            val attempt = DiagnosticAttempt(DiagnosticSide.CLIENT, journal::add)
            repeat(70) { attempt.failure(IllegalArgumentException("https://secret.invalid/?token=hidden")) }
        }
        val report = journal.report()
        assertEquals(640, report.lineSequence().count { it.contains("INVALID_CONFIGURATION") })
        assertFalse(report.contains("secret.invalid"))
        assertFalse(report.contains("hidden"))
    }
    @Test fun `wrapped signing failure exports only allowlisted categories`() {
        val journal = DiagnosticJournal()
        val attempt = DiagnosticAttempt(DiagnosticSide.SERVER, journal::add)
        val error = SSLHandshakeException("secret payload")
        error.initCause(java.security.SignatureException("private alias", java.security.InvalidKeyException("private key")))
        attempt.failure(error)
        val report = journal.report()
        assertTrue(report.contains("causes=[HANDSHAKE, SIGNATURE, INVALID_KEY]"))
        assertFalse(report.contains("secret payload"))
        assertFalse(report.contains("private alias"))
        assertFalse(report.contains("private key"))
    }
}
