package dev.profilebridge.core.links

/** Same-device monotonic deadline, bounded in-memory replay guard. No persistent queue. */
class StoreDeliveryGate {
    private val seen = linkedSetOf<String>()
    @Synchronized fun consume(id: String?, sentAtMillis: Long, nowMillis: Long): Boolean {
        if (id == null || !id.matches(Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) return false
        if (sentAtMillis < 0 || nowMillis < sentAtMillis || nowMillis - sentAtMillis > TTL_MILLIS) return false
        if (!seen.add(id)) return false
        if (seen.size > 64) seen.remove(seen.first())
        return true
    }
    companion object { const val TTL_MILLIS = 30_000L }
}
