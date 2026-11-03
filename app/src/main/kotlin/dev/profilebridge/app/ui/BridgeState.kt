package dev.profilebridge.app.ui

import android.os.UserHandle
import dev.profilebridge.app.platform.LinkHandling
import dev.profilebridge.app.platform.LocalHandler
import dev.profilebridge.app.platform.ProfileAccess
import dev.profilebridge.app.platform.ProfileManager

/** Where the main copy sends links. Chosen by the user; never derived from incoming text. */
enum class Destination { WORK_PROFILE, SECURE_FOLDER, ASK }

/** Which space this copy of Bridge runs in, as shown to the user. */
enum class Space { MAIN, WORK_PROFILE, SECURE_FOLDER }

/** Immutable screen state. Holds no secrets; the link lives only in memory. */
data class BridgeState(
    val space: Space = Space.MAIN,
    val destination: Destination = Destination.WORK_PROFILE,
    val copies: List<UserHandle> = emptyList(),
    /** The copy chosen for direct sends when there are several. */
    val chosenCopy: UserHandle? = null,
    val access: ProfileAccess = ProfileAccess.UNKNOWN,
    /** Shelter/Island installed in this profile; null while loading. */
    val managers: List<ProfileManager>? = null,
    /** Supported web links this copy may open: null when Android does not tell (Android 11). */
    val links: LinkHandling? = null,
    val openImmediately: Boolean = false,
    val compactWindow: Boolean = false,
    val diagnostics: Boolean = false,
    val report: String? = null,
    val samsung: Boolean = false,
    val url: String = "",
    val handlers: List<LocalHandler> = emptyList(),
    val message: Int? = null,
    val linkMessage: Int? = null,
    /** Fixed registry name of the app the current link belongs to, never text from the link. */
    val linkApp: String? = null,
    val version: String = "",
) {
    /** The copy that receives direct sends: the selected one, or the only one. */
    val target: UserHandle? get() = chosenCopy?.takeIf { it in copies } ?: copies.singleOrNull()
}

interface BridgeActions {
    fun selectTab(tab: Int) {}
    fun openSetup() {}
    fun setDestination(destination: Destination) {}
    fun selectCopy(copy: UserHandle) {}
    fun requestAccess() {}
    fun openManager(manager: ProfileManager) {}
    fun openLinkSettings() {}
    fun setSpace(space: Space) {}
    /** For Secure Folder the caller has already shown the separate consent. */
    fun setOpenImmediately(enabled: Boolean) {}
    fun setCompactWindow(enabled: Boolean) {}
    fun setDiagnostics(enabled: Boolean) {}
    fun previewReport() {}
    fun dismissReport() {}
    fun saveReport(report: String) {}
    fun shareReport(report: String) {}
    fun changeUrl(value: String) {}
    fun findHandlers() {}
    fun openHandler(handler: LocalHandler) {}
    fun sendToCopy(copy: UserHandle) {}
    fun sendToSecureFolder() {}
    fun closeQuickSend() {}
}

/** One thing that must be true before links can go to the chosen destination. */
enum class Check { SECOND_COPY, PROFILE_ACCESS, SECURE_FOLDER_COPY, LINKS }

enum class CheckStatus {
    DONE,
    TODO,
    /** Android does not let Bridge verify it; the user checks it once. */
    MANUAL,
}

fun BridgeState.checks(): List<Pair<Check, CheckStatus>> {
    if (space != Space.MAIN) return emptyList()
    return buildList {
        if (destination != Destination.SECURE_FOLDER) {
            add(Check.SECOND_COPY to if (copies.isNotEmpty()) CheckStatus.DONE else CheckStatus.TODO)
            add(Check.PROFILE_ACCESS to if (access == ProfileAccess.GRANTED) CheckStatus.DONE else CheckStatus.TODO)
        }
        if (destination != Destination.WORK_PROFILE) add(Check.SECURE_FOLDER_COPY to CheckStatus.MANUAL)
        add(Check.LINKS to when {
            links == null -> CheckStatus.MANUAL
            // Any selection counts: the owner may leave some domains to other apps on purpose.
            links.selected > 0 -> CheckStatus.DONE
            else -> CheckStatus.TODO
        })
    }
}

val List<Pair<Check, CheckStatus>>.remaining: Int get() = count { it.second == CheckStatus.TODO }
