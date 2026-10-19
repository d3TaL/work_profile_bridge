package dev.profilebridge.app.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentSanitizer
import dev.profilebridge.app.MainActivity
import dev.profilebridge.app.StoreReceiveActivity
import dev.profilebridge.core.links.SafeLink
import dev.profilebridge.core.links.BankLinks

object IncomingLinkReader {
    const val SHARE_ONCE = "dev.profilebridge.app.SHARE_ONCE"
    /** Untrusted opt-out only. Never used to authorize local opening. */
    fun isSharedOnce(context: Context, incoming: Intent): Boolean = try {
        IntentSanitizer.Builder()
            .allowComponent(ComponentName(context, MainActivity::class.java))
            .allowComponent(ComponentName(context, StoreReceiveActivity::class.java))
            .allowExtra(SHARE_ONCE, Boolean::class.javaObjectType)
            .build().sanitizeByFiltering(incoming).getBooleanExtra(SHARE_ONCE, false)
    } catch (_: RuntimeException) { true }
    fun read(context: Context, incoming: Intent): String? = try {
        val safe = IntentSanitizer.Builder()
            .allowComponent(ComponentName(context, MainActivity::class.java))
            .allowComponent(ComponentName(context, StoreReceiveActivity::class.java))
            .allowAction(Intent.ACTION_MAIN).allowAction(Intent.ACTION_SEND).allowAction(Intent.ACTION_VIEW)
            .allowCategory(Intent.CATEGORY_LAUNCHER).allowCategory(Intent.CATEGORY_DEFAULT)
            .allowCategory(Intent.CATEGORY_BROWSABLE).allowType("text/plain")
            .allowData { it.scheme.equals("https", true) || BankLinks.isScheme(it.scheme) }
            .allowExtra(Intent.EXTRA_TEXT, CharSequence::class.java)
            .build().sanitizeByFiltering(incoming)
        when (safe.action) {
            Intent.ACTION_VIEW -> safe.dataString?.takeIf { it.length <= SafeLink.MAX_BYTES }
            Intent.ACTION_SEND -> safe.getCharSequenceExtra(Intent.EXTRA_TEXT)?.takeIf { it.length <= SafeLink.MAX_BYTES }?.toString()
            else -> null
        }
    } catch (_: RuntimeException) { null }
}
