package dev.profilebridge.app.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

enum class ProfileManager(val label: String, val packageName: String) {
    SHELTER("Shelter", "net.typeblog.shelter"),
    ISLAND("Island", "com.oasisfeng.island"),
}

/** Local installation discovery only; it does not identify the profile owner. */
object ProfileManagers {
    suspend fun installed(context: Context): List<ProfileManager> = withContext(Dispatchers.IO) {
        ProfileManager.entries.filter { manager ->
            try {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getApplicationInfo(manager.packageName, 0)
                info.enabled
            } catch (_: PackageManager.NameNotFoundException) { false }
            catch (_: SecurityException) { false }
        }
    }

    suspend fun open(activity: Activity, manager: ProfileManager): Boolean {
        val component = withContext(Dispatchers.IO) {
            try {
                val target = activity.packageManager.getLaunchIntentForPackage(manager.packageName)?.component
                    ?: return@withContext null
                @Suppress("DEPRECATION")
                val info = activity.packageManager.getActivityInfo(target, 0)
                target.takeIf { it.packageName == manager.packageName && info.enabled && info.exported &&
                    info.applicationInfo.enabled && info.permission == null }
            } catch (_: PackageManager.NameNotFoundException) { null }
            catch (_: SecurityException) { null }
        } ?: return false
        if ((activity as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return false
        return try {
            // Construct our own launcher request; never forward an incoming Intent.
            activity.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component))
            true
        } catch (_: ActivityNotFoundException) { false }
        catch (_: SecurityException) { false }
    }
}
