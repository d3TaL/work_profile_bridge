package dev.profilebridge.core.links

import org.junit.Assert.*
import org.junit.Test

class StoreLinksTest {
    private fun target(url: String): String? = (SafeLink.parse(url) as? LinkParseResult.Accepted)?.let { StoreLinks.target(it.link) }
    @Test fun `exact shops and product paths are routed`() {
        assertEquals("ru.ozon.app.android", target("https://www.ozon.ru/product/test-123/?x=1#frag"))
        assertEquals("com.wildberries.ru", target("https://www.wildberries.ru/catalog/123/detail.aspx"))
    }
    @Test fun `lookalikes account checkout and unexpected paths never auto open`() {
        listOf("https://ozon.ru.evil.org/", "https://checkout.ozon.ru/", "https://ozon.ru/pay/",
            "https://wildberries.ru/account", "https://ozon.ru/%70roduct/test",
            "https://ozon.test-app.link/test").forEach { assertNull(target(it)) }
    }
    @Test fun `ozon paths are not restricted to a short link prefix`() {
        val raw = "https://ozon.ru/t/AbCd123"
        assertEquals("ru.ozon.app.android", target(raw))
        assertEquals(raw, StoreLinks.fromShareText("Товар\n$raw"))
        listOf("https://www.ozon.ru/t/AbCd123", "https://ozon.ru/other/AbCd123",
            "https://ozon.ru/t/AbCd123?x=%2F#part").forEach {
            assertEquals("ru.ozon.app.android", target(it))
        }
        assertNull(target("https://ozon.ru.evil.org/t/AbCd123"))
    }
    @Test fun `all registered ozon hosts are recognized with manual exceptions`() {
        assertEquals(21, StoreLinks.ozonHosts.size)
        StoreLinks.ozonHosts.forEach { host ->
            val link = (SafeLink.parse("https://$host/example") as LinkParseResult.Accepted).link
            assertEquals("ru.ozon.app.android", StoreLinks.packageFor(link))
            if (host == "checkout.ozon.ru" || host.contains("test-app.link")) assertNull(StoreLinks.target(link))
            else assertEquals("ru.ozon.app.android", StoreLinks.target(link))
        }
        listOf("https://evil.ozon.ru/example", "https://ozon.app.link.evil.org/example",
            "https://not-ozon.app.link/example").forEach { assertNull(target(it)) }
    }
    @Test fun `share title preserves original url and rejects ambiguity`() {
        val raw = "https://ozon.ru/product/test-123/?a=%2F&b=2#f"
        assertEquals(raw, StoreLinks.fromShareText("Название товара\n$raw"))
        assertNull(StoreLinks.fromShareText("$raw https://wildberries.ru/"))
        assertNull(StoreLinks.fromShareText("Название https://bank.example/token"))
        assertNull(StoreLinks.fromShareText("x".repeat(SafeLink.MAX_BYTES + 1)))
    }
}
