package dev.profilebridge.core.links

import org.junit.Assert.*
import org.junit.Test

class StoreEntryPolicyTest {
    @Test fun `public opt in cannot bypass protected ticket or route checks`() {
        assertFalse(StoreEntryPolicy.mayOpen(true, false, true, true, true, true))
        assertFalse(StoreEntryPolicy.mayOpen(false, false, true, true, false, true))
        assertFalse(StoreEntryPolicy.mayOpen(false, false, false, true, true, true))
        assertFalse(StoreEntryPolicy.mayOpen(false, false, true, true, true, false))
        assertTrue(StoreEntryPolicy.mayOpen(false, false, true, false, true, true))
        assertTrue(StoreEntryPolicy.mayOpen(true, true, true, true, false, true))
    }
}
