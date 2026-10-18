package dev.profilebridge.app.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentSanitizer
import dev.profilebridge.app.MainActivity
import dev.profilebridge.app.StoreReceiveActivity
import dev.profilebridge.core.links.SafeLink

object IncomingLinkReader {
    fun read(context: Context, incoming: Intent): String? = try {
        val safe = IntentSanitizer.Builder()
            .allowComponent(ComponentName(context, MainActivity::class.java))
            .allowComponent(ComponentName(context, StoreReceiveActivity::class.java))
            .allowAction(Intent.ACTION_MAIN).allowAction(Intent.ACTION_SEND).allowAction(Intent.ACTION_VIEW)
            .allowCategory(Intent.CATEGORY_LAUNCHER).allowCategory(Intent.CATEGORY_DEFAULT)
            .allowCategory(Intent.CATEGORY_BROWSABLE).allowType("text/plain")
            .allowData { it.scheme.equals("https", true) }
            .allowExtra(Intent.EXTRA_TEXT, CharSequence::class.java)
            .build().sanitizeByFiltering(incoming)
        when (safe.action) {
            Intent.ACTION_VIEW -> safe.dataString?.takeIf { it.length <= SafeLink.MAX_BYTES }
            Intent.ACTION_SEND -> safe.getCharSequenceExtra(Intent.EXTRA_TEXT)?.takeIf { it.length <= SafeLink.MAX_BYTES }?.toString()
            else -> null
        }
    } catch (_: RuntimeException) { null }
}
