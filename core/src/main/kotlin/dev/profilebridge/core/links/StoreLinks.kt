package dev.profilebridge.core.links

/** Exact public shop hosts only. No arbitrary subdomains, payments or redirect resolution. */
object StoreLinks {
    private val packages = mapOf(
        "ozon.ru" to "ru.ozon.app.android",
        "www.ozon.ru" to "ru.ozon.app.android",
        "m.ozon.ru" to "ru.ozon.app.android",
        "wildberries.ru" to "com.wildberries.ru",
        "www.wildberries.ru" to "com.wildberries.ru",
    )

    fun target(link: SafeLink): String? = packages[link.host]?.takeIf {
        // Automatic opening is for browsing products, not account/payment actions.
        (link.rawPath.isEmpty() || link.rawPath == "/") &&
            !link.original.contains('?') && !link.original.contains('#') ||
            link.host.endsWith("ozon.ru") && link.rawPath.startsWith("/product/") ||
            link.host.endsWith("wildberries.ru") &&
            link.rawPath.matches(Regex("/catalog/[0-9]+/detail\\.aspx"))
    }

    /** Share text may contain a title, but never silently pick among multiple URLs. */
    fun fromShareText(text: String): String? {
        if (text.length > SafeLink.MAX_BYTES || text.toByteArray(Charsets.UTF_8).size > SafeLink.MAX_BYTES) return null
        if (SafeLink.parse(text) is LinkParseResult.Accepted) return text
        val candidates = Regex("https://[^\\s<>\"']+", RegexOption.IGNORE_CASE).findAll(text).take(2).toList()
        if (candidates.size != 1) return null
        val value = candidates.single().value
        val parsed = SafeLink.parse(value) as? LinkParseResult.Accepted ?: return null
        return value.takeIf { target(parsed.link) != null }
    }
}
