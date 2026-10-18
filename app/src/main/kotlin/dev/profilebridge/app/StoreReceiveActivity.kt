package dev.profilebridge.app

/** Signature-protected entry used only by the sending copy after a user's send gesture. */
class StoreReceiveActivity : MainActivity() {
    override val trustedStoreEntry: Boolean = true
}
