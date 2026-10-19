package dev.profilebridge.core.links

/** Exact public shop hosts only. No arbitrary subdomains, payments or redirect resolution. */
object StoreLinks {
    val ozonHosts: Set<String> = setOf(
        "ozon.ru", "www.ozon.ru", "m.ozon.ru", "f.ozon.ru", "checkout.ozon.ru",
        "ozon.by", "www.ozon.by", "ozon.kz", "www.ozon.kz", "ozon.tm", "www.ozon.tm",
        "www.ozon.uz", "ozon.com", "www.ozon.com", "mn.ozon.com", "am.ozon.com",
        "ozon.app.link", "ozon-alternate.app.link", "ozon.test-app.link",
        "ozon-alternate.test-app.link", "ozon.onelink.me",
    )
    private val manualOzonHosts = setOf(
        "checkout.ozon.ru", "ozon.test-app.link", "ozon-alternate.test-app.link",
    )
    private val packages = ozonHosts.associateWith { "ru.ozon.app.android" } + mapOf(
        "wildberries.ru" to "com.wildberries.ru",
        "www.wildberries.ru" to "com.wildberries.ru",
    )

    /** Recognition is independent of permission to launch automatically. */
    fun packageFor(link: SafeLink): String? = packages[link.host] ?: BankLinks.target(link)

    fun target(link: SafeLink): String? {
        if (BankLinks.target(link) != null) return null // Never shop auto-open for banks.
        val pkg = packageFor(link) ?: return null
        if (link.host in ozonHosts) {
            // Best-effort direct payment/account exclusion, not destination verification.
            // Opaque short links and query parameters are interpreted only by Ozon.
            val segment = link.rawPath.trimStart('/').substringBefore('/').lowercase()
            if (link.host in manualOzonHosts || '%' in link.rawPath ||
                segment in setOf("pay", "payment", "payments", "checkout", "bank", "account")) return null
            return pkg
        }
        return pkg.takeIf {
            (link.rawPath.isEmpty() || link.rawPath == "/") &&
                !link.original.contains('?') && !link.original.contains('#') ||
                link.rawPath.matches(Regex("/catalog/[0-9]+/detail\\.aspx"))
        }
    }

    /** Share text may contain a title, but never silently pick among multiple URLs. */
    fun fromShareText(text: String): String? {
        if (text.length > SafeLink.MAX_BYTES || text.toByteArray(Charsets.UTF_8).size > SafeLink.MAX_BYTES) return null
        if (SafeLink.parse(text) is LinkParseResult.Accepted) return text
        val candidates = Regex("https://[^\\s<>\"']+", RegexOption.IGNORE_CASE).findAll(text).take(2).toList()
        if (candidates.size != 1) return null
        val value = candidates.single().value
        val parsed = SafeLink.parse(value) as? LinkParseResult.Accepted ?: return null
        return value.takeIf { packageFor(parsed.link) != null }
    }
}
