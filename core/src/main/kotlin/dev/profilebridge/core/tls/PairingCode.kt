package dev.profilebridge.core.tls

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** A short-lived diagnostic invitation, not persistent pairing or a URL transport credential. */
class PairingCode private constructor(
    val port: Int,
    private val pin: ByteArray,
    private val secret: ByteArray,
) {
    fun encodeForManualTransfer(): String = "PLB2:$port:${encode(pin)}:${encode(secret)}"
    fun certificatePin(): ByteArray = pin.copyOf()
    internal fun tokenCopy(): ByteArray = secret.copyOf()
    internal fun accepts(candidate: ByteArray): Boolean = MessageDigest.isEqual(secret, candidate)
    fun destroy() { secret.fill(0) }
    override fun toString(): String = "PairingCode([REDACTED])"

    companion object {
        const val MAX_LENGTH = 112
        internal const val SECRET_BYTES = 32
        fun create(port: Int, pin: ByteArray): PairingCode {
            require(port in 1..65535 && pin.size == 32)
            return PairingCode(port, pin.copyOf(), ByteArray(32).also(SecureRandom()::nextBytes))
        }
        fun parse(value: String): PairingCode? {
            if (value.length > MAX_LENGTH) return null
            val parts = value.split(':')
            if (parts.size != 4 || parts[0] != "PLB2" || !parts[1].matches(Regex("[1-9][0-9]{0,4}"))) return null
            val port = parts[1].toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            val pin = decode(parts[2]) ?: return null
            val secret = decode(parts[3]) ?: return null
            return PairingCode(port, pin, secret).takeIf { it.encodeForManualTransfer() == value }
        }
        private fun encode(value: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value)
        private fun decode(value: String): ByteArray? {
            if (!value.matches(Regex("[A-Za-z0-9_-]{43}"))) return null
            return try { Base64.getUrlDecoder().decode(value).takeIf { it.size == 32 } }
            catch (_: IllegalArgumentException) { null }
        }
    }
}
