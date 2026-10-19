package dev.profilebridge.app.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.CrossProfileApps
import android.os.UserHandle
import dev.profilebridge.app.MainActivity
import dev.profilebridge.app.StoreReceiveActivity
import dev.profilebridge.core.links.LinkParseResult
import dev.profilebridge.core.links.SafeLink
import dev.profilebridge.core.links.StoreLinks

enum class ProfileAccess { GRANTED, REQUESTABLE, NOT_AVAILABLE, UNKNOWN }
enum class ProfileSendResult { SUBMITTED_UNKNOWN_OUTCOME, INVALID_LINK, NO_TARGET, ACCESS_DENIED, UNAVAILABLE, UNKNOWN_OUTCOME }

object ProfileDiagnostics {
    fun access(activity: Activity): ProfileAccess = try {
        val api = activity.getSystemService(CrossProfileApps::class.java)
        when {
            api.canInteractAcrossProfiles() -> ProfileAccess.GRANTED
            api.canRequestInteractAcrossProfiles() -> ProfileAccess.REQUESTABLE
            else -> ProfileAccess.NOT_AVAILABLE
        }
    } catch (_: SecurityException) { ProfileAccess.UNKNOWN }

    /** Settings decides whether DPC/OEM allowlisting and user consent permit access. */
    fun requestAccess(activity: Activity): Boolean = try {
        val api = activity.getSystemService(CrossProfileApps::class.java)
        if (!api.canRequestInteractAcrossProfiles()) false else {
            activity.startActivity(api.createRequestInteractAcrossProfilesIntent())
            true
        }
    } catch (_: SecurityException) { false }
    catch (_: ActivityNotFoundException) { false }

    /** One user-triggered attempt. Return from framework is not a receiver ACK. No retries. */
    fun send(activity: Activity, target: UserHandle, text: String): ProfileSendResult {
        val parsed = SafeLink.parseRouting(text)
        if (parsed !is LinkParseResult.Accepted) return ProfileSendResult.INVALID_LINK
        return try {
            val api = activity.getSystemService(CrossProfileApps::class.java)
            if (target !in api.targetUserProfiles) return ProfileSendResult.NO_TARGET
            if (!api.canInteractAcrossProfiles()) return ProfileSendResult.ACCESS_DENIED
            // Never forward the incoming Intent, flags, ClipData, grants or extra fields.
            val outgoing = Intent(Intent.ACTION_SEND).apply {
                component = ComponentName(activity, if (StoreLinks.packageFor(parsed.link) != null)
                    StoreReceiveActivity::class.java else MainActivity::class.java)
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, parsed.link.original)
                if (component?.className == StoreReceiveActivity::class.java.name) StoreDelivery.stamp(this)
            }
            api.startActivity(outgoing, target, activity)
            ProfileSendResult.SUBMITTED_UNKNOWN_OUTCOME
        } catch (_: SecurityException) { ProfileSendResult.ACCESS_DENIED }
        catch (_: ActivityNotFoundException) { ProfileSendResult.UNAVAILABLE }
        catch (_: RuntimeException) { ProfileSendResult.UNKNOWN_OUTCOME }
    }

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
