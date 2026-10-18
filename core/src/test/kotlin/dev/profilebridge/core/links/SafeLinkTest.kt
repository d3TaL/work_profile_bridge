package dev.profilebridge.core.links

import org.junit.Assert.*
import org.junit.Test

class SafeLinkTest {
    @Test fun `preserves signed representation and redacts toString`() {
        val raw = "https://shop.example.invalid/a%2Fb?q=a%2Bb&x=1&x=2#Synthetic"
        val value = SafeLink.parse(raw) as LinkParseResult.Accepted
        assertEquals(raw, value.link.original)
        assertEquals("/a%2Fb", value.link.rawPath)
        assertFalse(value.toString().contains(raw))
    }
    @Test fun `rejects executable schemes`() {
        listOf("file:///x", "content://x/y", "javascript:alert(1)", "intent://x", "http://example.invalid", "//example.invalid")
            .forEach { assertTrue(SafeLink.parse(it) is LinkParseResult.Rejected) }
    }
    @Test fun `rejects misleading authorities and numeric addresses`() {
        listOf("https://good.invalid@evil.invalid", "https://example.invalid:8443", "https://127.0.0.1/",
            "https://[::1]/", "https://localhost/", "https://example.invalid./", "https://a..invalid/", "https://example.invalid\\@evil.invalid/")
            .forEach { assertTrue(SafeLink.parse(it) is LinkParseResult.Rejected) }
    }
    @Test fun `rejects malformed and oversized input`() {
        listOf("https://example.invalid/\nvalue", "https://example.invalid/a b", "https://example.invalid/%GG", "https://example.invalid/" + "a".repeat(SafeLink.MAX_BYTES))
            .forEach { assertTrue(SafeLink.parse(it) is LinkParseResult.Rejected) }
    }
    @Test fun `normalizes comparison fields only`() {
        val raw = "HTTPS://Shop.Example.Invalid:443/Case?X=Y"
        val link = (SafeLink.parse(raw) as LinkParseResult.Accepted).link
        assertEquals("shop.example.invalid", link.host)
        assertEquals(raw, link.original)
    }
}
