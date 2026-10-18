package dev.profilebridge.app.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.IntentSanitizer
import dev.profilebridge.app.StoreReceiveActivity
import dev.profilebridge.core.links.StoreDeliveryGate
import java.util.UUID

object StoreDelivery {
    private const val ID = "dev.profilebridge.delivery.ID"
    private const val SENT = "dev.profilebridge.delivery.SENT"
    private val gate = StoreDeliveryGate()

    fun stamp(intent: Intent) {
        intent.putExtra(ID, UUID.randomUUID().toString())
        intent.putExtra(SENT, SystemClock.elapsedRealtime())
    }

    fun consume(context: Context, incoming: Intent): Boolean = try {
        val safe = IntentSanitizer.Builder()
            .allowComponent(ComponentName(context, StoreReceiveActivity::class.java))
            .allowAction(Intent.ACTION_SEND).allowType("text/plain")
            .allowExtra(ID, String::class.java)
            .allowExtra(SENT, java.lang.Long::class.java)
            .build().sanitizeByFiltering(incoming)
        safe.action == Intent.ACTION_SEND && gate.consume(safe.getStringExtra(ID),
            safe.getLongExtra(SENT, -1), SystemClock.elapsedRealtime())
    } catch (_: RuntimeException) { false }
}
