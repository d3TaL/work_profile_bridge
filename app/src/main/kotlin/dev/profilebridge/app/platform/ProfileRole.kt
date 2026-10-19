package dev.profilebridge.app.platform

import android.content.Context
import android.os.UserManager

enum class ProfileRole { MAIN, MANAGED, OTHER, UNKNOWN }

object ProfileRoles {
    fun detect(context: Context): ProfileRole = try {
        val users = context.getSystemService(UserManager::class.java)
        when {
            users.isSystemUser -> ProfileRole.MAIN
            users.isManagedProfile -> ProfileRole.MANAGED
            else -> ProfileRole.OTHER
        }
    } catch (_: SecurityException) { ProfileRole.UNKNOWN }
}
