package dev.profilebridge.core.links

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

class SafeLink private constructor(
    val original: String,
    val scheme: String,
    val host: String,
    val rawPath: String,
) {
    override fun toString(): String = "SafeLink([REDACTED])"

    companion object {
        const val MAX_BYTES = 16 * 1024
        /** Explicit routing boundary: HTTPS or the single supported T-Bank payment scheme. */
        fun parseRouting(input: String): LinkParseResult {
            if (!input.startsWith("${BankLinks.T_BANK_SCHEME}:", ignoreCase = true)) return parse(input)
            if (input.length > MAX_BYTES || input.toByteArray(Charsets.UTF_8).size > MAX_BYTES)
                return LinkParseResult.Rejected(LinkError.TOO_LARGE)
            if (input.any { it.isWhitespace() || it.isISOControl() || it == '\\' || it.isSurrogate() })
                return LinkParseResult.Rejected(LinkError.INVALID_FORMAT)
            val uri = try { URI(input) } catch (_: URISyntaxException) {
                return LinkParseResult.Rejected(LinkError.INVALID_FORMAT)
            }
            if (uri.isOpaque || uri.rawAuthority.isNullOrEmpty() || uri.host == null ||
                uri.rawUserInfo != null || uri.port != -1 || uri.rawFragment != null)
                return LinkParseResult.Rejected(LinkError.INVALID_AUTHORITY)
            return LinkParseResult.Accepted(SafeLink(input, BankLinks.T_BANK_SCHEME,
                uri.host.lowercase(Locale.ROOT), uri.rawPath.orEmpty()))
        }
        fun parse(input: String): LinkParseResult {
            if (input.length > MAX_BYTES || input.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
                return LinkParseResult.Rejected(LinkError.TOO_LARGE)
            }
            if (input.any { it.isWhitespace() || it.isISOControl() || it == '\\' || it.isSurrogate() }) {
                return LinkParseResult.Rejected(LinkError.INVALID_FORMAT)
            }
            val uri = try { URI(input) } catch (_: URISyntaxException) {
                return LinkParseResult.Rejected(LinkError.INVALID_FORMAT)
            }
            if (!uri.scheme.equals("https", true)) return LinkParseResult.Rejected(LinkError.UNSUPPORTED_SCHEME)
            val host = uri.host?.lowercase(Locale.ROOT)
            if (uri.isOpaque || uri.rawUserInfo != null || host == null || !isDnsHost(host) ||
                uri.port != -1 && uri.port != 443
            ) return LinkParseResult.Rejected(LinkError.INVALID_AUTHORITY)
            return LinkParseResult.Accepted(SafeLink(input, "https", host, uri.rawPath.orEmpty()))
        }

        private fun isDnsHost(host: String): Boolean =
            host.length <= 253 && '.' in host && !host.all { it.isDigit() || it == '.' } &&
                host.split('.').all { label ->
                    label.length in 1..63 && label.first().isAscii() && label.last().isAscii() &&
                        label.all { it.isAscii() || it == '-' }
                }
        private fun Char.isAscii(): Boolean = this in 'a'..'z' || this in '0'..'9'
    }
}
enum class LinkError { TOO_LARGE, INVALID_FORMAT, UNSUPPORTED_SCHEME, INVALID_AUTHORITY }
sealed interface LinkParseResult {
    data class Accepted(val link: SafeLink) : LinkParseResult
    data class Rejected(val error: LinkError) : LinkParseResult
}
