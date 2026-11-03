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

    /** A supported app with exact hosts it handles. Wildcards and infra/staging hosts are excluded. */
    data class Shop(val label: String, val packageName: String, val hosts: Set<String>)

    /** Non-bank apps added from owner screenshots. General path rule, not Ozon/WB specific. */
    val extraShops: List<Shop> = listOf(
        Shop("AliExpress", "ru.aliexpress.buyer", setOf(
            "aliexpress.ru", "www.aliexpress.ru", "m.aliexpress.ru", "sl.aliexpress.ru",
            "aliexpress.com", "www.aliexpress.com", "m.aliexpress.com",
            "ali.click", "aliclick.shop", "aliexprus.ru")),
        Shop("Avito", "com.avito.android", setOf("m.avito.ru", "www.avito.ru")),
        Shop("Яндекс Карты", "ru.yandex.yandexmaps", setOf(
            "maps.yandex.ru", "maps.yandex.by", "maps.yandex.kz", "maps.ya.ru")),
        Shop("Яндекс Диск", "ru.yandex.disk", setOf(
            "disk.yandex.ru", "disk.yandex.com", "disk.yandex.com.tr", "disk.360.yandex.ru", "yadi.sk")),
    )
    private val extraHosts: Map<String, String> =
        extraShops.flatMap { shop -> shop.hosts.map { it to shop.packageName } }.toMap()

    /** First path segment that marks a sensitive flow; such links need a manual tap, not auto-open. */
    private val sensitiveSegments = setOf(
        "pay", "payment", "payments", "checkout", "bank", "account", "login", "signin", "oauth", "passport")

    /** Recognition is independent of permission to launch automatically. */
    fun packageFor(link: SafeLink): String? =
        packages[link.host] ?: extraHosts[link.host] ?: BankLinks.target(link)

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
        if (link.host in extraHosts) {
            // Best-effort exclusion of sensitive flows; query/fragment preserved, never rewritten.
            val segment = link.rawPath.trimStart('/').substringBefore('/').lowercase()
            return pkg.takeIf { '%' !in link.rawPath && segment !in sensitiveSegments }
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
        if (SafeLink.parseRouting(text) is LinkParseResult.Accepted) return text
        val candidates = Regex("https://[^\\s<>\"']+", RegexOption.IGNORE_CASE).findAll(text).take(2).toList()
        if (candidates.size != 1) return null
        val value = candidates.single().value
        val parsed = SafeLink.parse(value) as? LinkParseResult.Accepted ?: return null
        return value.takeIf { packageFor(parsed.link) != null }
    }
}
