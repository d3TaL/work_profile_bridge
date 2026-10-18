package dev.profilebridge.core.tls

import org.junit.Assert.*
import org.junit.Test

class PairingCodeTest {
    @Test fun `round trip preserves pin and secret without logging`() {
        val code = PairingCode.create(12345, ByteArray(32) { 1 })
        val raw = code.encodeForManualTransfer()
        val decoded = PairingCode.parse(raw)!!
        assertEquals(raw, decoded.encodeForManualTransfer())
        assertTrue(code.accepts(decoded.tokenCopy()))
        assertArrayEquals(code.certificatePin(), decoded.certificatePin())
        assertFalse(code.toString().contains(raw))
    }
    @Test fun `new session gets fresh entropy`() {
        assertNotEquals(PairingCode.create(12345, ByteArray(32)).encodeForManualTransfer(), PairingCode.create(12345, ByteArray(32)).encodeForManualTransfer())
    }
    @Test fun `invalid versions ports and noncanonical codes are rejected`() {
        val raw = PairingCode.create(12345, ByteArray(32)).encodeForManualTransfer()
        listOf("", "x".repeat(1000), raw.replace("PLB2", "PLB1"), raw.replace(":12345:", ":0:"), raw.replace(":12345:", ":65536:"), raw.replace(":12345:", ":012345:"), "$raw=", "$raw\n")
            .forEach { assertNull(PairingCode.parse(it)) }
    }
    @Test fun `caller cannot mutate pin and wrong token is refused`() {
        val pin = ByteArray(32) { 1 }
        val code = PairingCode.create(12345, pin)
        pin.fill(0)
        val returned = code.certificatePin(); returned.fill(0)
        assertEquals(1.toByte(), code.certificatePin()[0])
        assertFalse(code.accepts(ByteArray(32)))
        code.destroy()
    }
}
