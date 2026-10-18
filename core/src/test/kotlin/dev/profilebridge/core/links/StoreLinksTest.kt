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
            "https://ozon.ru/?action=pay").forEach { assertNull(target(it)) }
    }
    @Test fun `share title preserves original url and rejects ambiguity`() {
        val raw = "https://ozon.ru/product/test-123/?a=%2F&b=2#f"
        assertEquals(raw, StoreLinks.fromShareText("Название товара\n$raw"))
        assertNull(StoreLinks.fromShareText("$raw https://wildberries.ru/"))
        assertNull(StoreLinks.fromShareText("Название https://bank.example/token"))
        assertNull(StoreLinks.fromShareText("x".repeat(SafeLink.MAX_BYTES + 1)))
    }
}
