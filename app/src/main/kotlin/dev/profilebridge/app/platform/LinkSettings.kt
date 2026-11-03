package dev.profilebridge.app.platform

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How many of the declared web link domains the user allowed this copy to open. */
data class LinkHandling(val selected: Int, val total: Int)

/** Reads only this package's own "Open by default" state; needs no permission. */
object LinkSettings {
    suspend fun read(context: Context): LinkHandling? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@withContext null
        try {
            val state = context.getSystemService(DomainVerificationManager::class.java)
                ?.getDomainVerificationUserState(context.packageName) ?: return@withContext null
            val hosts = state.hostToStateMap
            val selected = if (!state.isLinkHandlingAllowed) 0
                else hosts.values.count { it != DomainVerificationUserState.DOMAIN_STATE_NONE }
            LinkHandling(selected, hosts.size)
        } catch (_: PackageManager.NameNotFoundException) { null }
        catch (_: SecurityException) { null }
    }
}
