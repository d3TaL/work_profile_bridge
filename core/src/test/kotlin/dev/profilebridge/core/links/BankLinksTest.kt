package dev.profilebridge.core.links

import org.junit.Assert.*
import org.junit.Test

class BankLinksTest {
    @Test fun `ozon payment allows bank and shop without changing payment policy`() {
        val link = (SafeLink.parseRouting("bank100000000273://qr.nspk.ru/TEST_ONLY") as LinkParseResult.Accepted).link
        assertEquals(listOf("ru.ozon.app.android", "ru.ozon.fintech.finance"), BankLinks.handlerPackages(link))
        assertEquals("ru.ozon.fintech.finance", StoreLinks.packageFor(link))
        assertNull(StoreLinks.target(link))
        val other = (SafeLink.parseRouting("bank100000000004://qr.nspk.ru/TEST_ONLY") as LinkParseResult.Accepted).link
        assertEquals(listOf(BankLinks.T_BANK), BankLinks.handlerPackages(other))
    }
    @Test fun `payment scheme is explicit raw preserving and never automatic`() {
        val raw = "bank100000000004://qr.nspk.ru/TEST_ONLY?type=02&x=%2F"
        assertTrue(SafeLink.parse(raw) is LinkParseResult.Rejected)
        val link = (SafeLink.parseRouting(raw) as LinkParseResult.Accepted).link
        assertEquals(raw, link.original)
        assertEquals(BankLinks.T_BANK, BankLinks.target(link))
        assertNull(StoreLinks.target(link))
        assertEquals(raw, StoreLinks.fromShareText(raw))
        assertFalse(link.toString().contains("TEST_ONLY"))
        listOf("bank999999999999://qr.nspk.ru/x", "intent://qr.nspk.ru/x",
            "bank100000000004:opaque", "bank100000000004://user@qr.nspk.ru/x",
            "bank100000000004://qr.nspk.ru:443/x", "bank100000000004://qr.nspk.ru/x#frag",
            "bank100000000004://qr.nspk.ru/x\n", "bank100000000004://qr.nspk.ru/%zz").forEach {
            assertTrue(SafeLink.parseRouting(it) is LinkParseResult.Rejected)
        }
    }
    @Test fun `every observed bank maps only to its package and never shop auto open`() {
        assertEquals(14, BankLinks.banks.size)
        assertEquals(14, BankLinks.banks.map { it.scheme }.toSet().size)
        BankLinks.banks.forEach { bank ->
            val raw = "${bank.scheme}://qr.nspk.ru/TEST_ONLY?x=%2F"
            val parsed = (SafeLink.parseRouting(raw) as LinkParseResult.Accepted).link
            assertEquals(bank.packageName, BankLinks.target(parsed))
            assertEquals(raw, parsed.original)
            assertNull(StoreLinks.target(parsed))
        }
    }
    @Test fun `bank domains recognized but never shop auto opened`() {
        BankLinks.hosts.forEach {
            val link = (SafeLink.parse("https://$it/example?x=%2F#part") as LinkParseResult.Accepted).link
            assertEquals(BankLinks.T_BANK, StoreLinks.packageFor(link))
            assertNull(StoreLinks.target(link))
        }
        val fake = (SafeLink.parse("https://tbank.ru.evil.org/") as LinkParseResult.Accepted).link
        assertNull(BankLinks.target(fake))
        assertEquals("https://tbank.ru/example", StoreLinks.fromShareText("Банк https://tbank.ru/example"))
    }
}
