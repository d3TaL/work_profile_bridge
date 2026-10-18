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
    private const val ALIAS = "profile_bridge_tls_probe_v1"

    suspend fun load(diagnostic: DiagnosticAttempt): TlsIdentity = withContext(Dispatchers.IO) {
        diagnostic.record(DiagnosticStage.IDENTITY_LOAD)
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(ALIAS)) {
            val now = System.currentTimeMillis()
            val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
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
        diagnostic.operation(DiagnosticStage.TLS_CONTEXT_INIT) { TlsContexts.server(key, cert) }
    }
}
