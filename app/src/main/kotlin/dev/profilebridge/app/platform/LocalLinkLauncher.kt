package dev.profilebridge.app.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import dev.profilebridge.core.links.SafeLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalHandler(val packageName: String)

object LocalLinkLauncher {
    fun handlers(activity: Activity, link: SafeLink, targetPackage: String? = null): List<LocalHandler> = try {
        @Suppress("DEPRECATION")
        val candidates = activity.packageManager.queryIntentActivities(view(link).setPackage(targetPackage), PackageManager.MATCH_DEFAULT_ONLY)
        candidates.asSequence().filter {
            it.activityInfo.enabled && it.activityInfo.exported && it.activityInfo.permission == null &&
                it.activityInfo.packageName != activity.packageName
        }.map { LocalHandler(it.activityInfo.packageName) }.distinctBy { it.packageName }
            .sortedBy { it.packageName }.take(32).toList()
    } catch (_: SecurityException) { emptyList() }

    suspend fun open(activity: Activity, link: SafeLink, handler: LocalHandler, authorized: () -> Boolean): Boolean {
        if (withContext(Dispatchers.IO) { handler !in handlers(activity, link, handler.packageName) }) return false
        return withContext(Dispatchers.Main.immediate) {
            val owner = activity as? LifecycleOwner
            if (!authorized() || owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return@withContext false
            try {
                activity.startActivity(view(link).setPackage(handler.packageName))
                true
            } catch (_: ActivityNotFoundException) { false }
            catch (_: SecurityException) { false }
        }
    }
    private fun view(link: SafeLink) = Intent(Intent.ACTION_VIEW, link.original.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
}
