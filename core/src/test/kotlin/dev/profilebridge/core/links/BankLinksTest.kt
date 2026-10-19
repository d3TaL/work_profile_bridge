package dev.profilebridge.core.links

import org.junit.Assert.*
import org.junit.Test

class BankLinksTest {
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
