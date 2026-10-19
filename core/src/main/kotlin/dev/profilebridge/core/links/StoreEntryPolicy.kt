package dev.profilebridge.core.links

/** A public SEND opt-in grants local launching only, never trust in its sender. */
object StoreEntryPolicy {
    fun mayOpen(protectedEntry: Boolean, validDelivery: Boolean, isSend: Boolean,
                protectedOptIn: Boolean, shareOptIn: Boolean, supported: Boolean): Boolean =
        supported && isSend && if (protectedEntry) validDelivery && protectedOptIn else shareOptIn
}
