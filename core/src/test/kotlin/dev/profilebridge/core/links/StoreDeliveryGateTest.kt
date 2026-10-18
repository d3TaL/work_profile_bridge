package dev.profilebridge.core.links

import org.junit.Assert.*
import org.junit.Test

class StoreDeliveryGateTest {
    @Test fun `expired future malformed and replayed deliveries do not auto open`() {
        val gate = StoreDeliveryGate()
        val id = "12345678-1234-1234-1234-123456789abc"
        assertFalse(gate.consume(id, 10, 30_011))
        assertFalse(gate.consume(id, 10, 9))
        assertFalse(gate.consume("other", 10, 10))
        assertTrue(gate.consume(id, 10, 11))
        assertFalse(gate.consume(id, 10, 12))
    }
}
