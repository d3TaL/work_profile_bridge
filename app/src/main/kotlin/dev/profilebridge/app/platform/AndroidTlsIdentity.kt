package dev.profilebridge.app.platform

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dev.profilebridge.core.tls.TlsContexts
import dev.profilebridge.core.tls.TlsIdentity
import dev.profilebridge.core.tls.DiagnosticAttempt
import dev.profilebridge.core.tls.DiagnosticStage
import dev.profilebridge.core.tls.DiagnosticStatus
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import javax.security.auth.x500.X500Principal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AndroidTlsIdentity {
    // v1 permits SHA256 only. Conscrypt delegates EC TLS digest signing via NONEwithECDSA.
    // Keep v1 untouched; v2 is a new diagnostic identity, not a persistent pairing migration.
    private const val ALIAS = "profile_bridge_tls_probe_v2"

    suspend fun load(diagnostic: DiagnosticAttempt): TlsIdentity = withContext(Dispatchers.IO) {
        diagnostic.record(DiagnosticStage.IDENTITY_LOAD)
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(ALIAS)) {
            val now = System.currentTimeMillis()
            val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_NONE)
                .setCertificateSubject(X500Principal("CN=Profile Bridge Local Probe"))
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(Date(now - 86_400_000L))
                .setCertificateNotAfter(Date(now + 31_536_000_000L))
                .setUserAuthenticationRequired(false)
                .build()
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(spec)
                generateKeyPair()
            }
        }
        val key = store.getKey(ALIAS, null) as PrivateKey
        val cert = store.getCertificate(ALIAS) as X509Certificate
        cert.checkValidity()
        diagnostic.record(DiagnosticStage.IDENTITY_LOAD, DiagnosticStatus.SUCCESS)
        diagnostic.operation(DiagnosticStage.KEY_SIGN_CHECK) {
            checkSignature(key, cert, "SHA256withECDSA")
        }
        diagnostic.operation(DiagnosticStage.KEY_RAW_SIGN_CHECK) {
            checkSignature(key, cert, "NONEwithECDSA")
        }
        diagnostic.operation(DiagnosticStage.TLS_CONTEXT_INIT) { TlsContexts.server(key, cert, diagnostic) }
    }

    private fun checkSignature(key: PrivateKey, cert: X509Certificate, algorithm: String) {
        val challenge = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
        var signature: ByteArray? = null
        try {
            signature = java.security.Signature.getInstance(algorithm).run {
                initSign(key); update(challenge); sign()
            }
            val valid = java.security.Signature.getInstance(algorithm).run {
                initVerify(cert.publicKey); update(challenge); verify(signature)
            }
            if (!valid) throw java.security.SignatureException()
        } finally { challenge.fill(0); signature?.fill(0) }
    }
}
