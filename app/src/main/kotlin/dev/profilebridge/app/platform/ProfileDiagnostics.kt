package dev.profilebridge.app.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.pm.CrossProfileApps
import android.os.UserHandle
import dev.profilebridge.app.MainActivity

object ProfileDiagnostics {
    fun targets(activity: Activity): List<UserHandle> = try {
        activity.getSystemService(CrossProfileApps::class.java).targetUserProfiles.toList()
    } catch (_: SecurityException) { emptyList() }

    fun open(activity: Activity, target: UserHandle): Boolean = try {
        val api = activity.getSystemService(CrossProfileApps::class.java)
        if (target !in api.targetUserProfiles) false
        else {
            api.startMainActivity(ComponentName(activity, MainActivity::class.java), target)
            true
        }
    } catch (_: SecurityException) { false }
    catch (_: ActivityNotFoundException) { false }
}
