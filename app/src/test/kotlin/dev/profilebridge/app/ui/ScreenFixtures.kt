package dev.profilebridge.app.ui

import android.app.Application
import android.content.ComponentName
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.navigationevent.DirectNavigationEventInput
import androidx.test.core.app.ApplicationProvider
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.AppMode
import dev.profilebridge.app.platform.LocalHandler
import dev.profilebridge.app.platform.ProfileAccess
import dev.profilebridge.app.platform.ProfileManager
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.Shadows

/** Synthetic fixtures only: a public store root, never a bank or tokenized link. */
const val PUBLIC_LINK = "https://www.ozon.ru/"

val workCopy: UserHandle = UserHandle.getUserHandleForUid(10 * 100_000)
val secureFolderCopy: UserHandle = UserHandle.getUserHandleForUid(150 * 100_000)

/** Robolectric knows only manifest activities; register the Compose test host without shipping it. */
class RegisterComponentActivity : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val app = ApplicationProvider.getApplicationContext<Application>()
            Shadows.shadowOf(app.packageManager)
                .addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
            base.evaluate()
        }
    }
}

/** A completed system back (button or finished gesture) through the activity's dispatcher. */
fun <A : ComponentActivity> AndroidComposeTestRule<*, A>.pressBack() {
    runOnUiThread {
        val input = DirectNavigationEventInput()
        activity.navigationEventDispatcher.addInput(input)
        input.backCompleted()
        activity.navigationEventDispatcher.removeInput(input)
    }
    waitForIdle()
}

@Composable
fun HomeFixture(
    tab: Int = 0,
    selectTab: (Int) -> Unit = {},
    url: String = "",
    targets: List<UserHandle> = listOf(workCopy),
    selectedTarget: UserHandle? = targets.firstOrNull(),
    shareRoute: Boolean = false,
    handlers: List<LocalHandler> = emptyList(),
    message: Int? = null,
    sendToCopy: (UserHandle) -> Unit = {},
    shareLink: () -> Unit = {},
    changeUrl: (String) -> Unit = {},
) {
    BridgeScreen(
        tab = tab, selectTab = selectTab, openSetup = {}, openApps = {},
        targets = targets, message = message, device = "Android 17 · API 37 · target 37",
        profileRole = stringResource(R.string.role_main),
        url = url, linkError = null, handlers = handlers, refresh = {},
        changeUrl = changeUrl, inspect = {}, clear = {},
        openCopy = {}, openHandler = {}, shareReport = {}, saveReport = {},
        profileAccess = ProfileAccess.GRANTED, requestProfileAccess = {}, sendToCopy = sendToCopy,
        autoOpenStores = false, setAutoOpenStores = {},
        autoOpenShares = false, setAutoOpenShares = {},
        debugEnabled = false, setDebugEnabled = {}, debugReport = null,
        previewDebug = {}, dismissDebug = {},
        autoForward = true, setAutoForward = {},
        shareRoute = shareRoute, setShareRoute = {}, shareLink = shareLink,
        compactMode = false, setCompactMode = {},
        selectedTarget = selectedTarget, selectTarget = {},
    )
}

val sampleModes: Map<String, AppMode> = mapOf(
    "ru.ozon.app.android" to AppMode.AUTOMATIC,
    "com.wildberries.ru" to AppMode.CONFIRM,
    "ru.sberbankmobile" to AppMode.DISABLED,
)

@Composable
fun SetupFixture(
    initialStep: Int = 0,
    managers: List<ProfileManager>? = listOf(ProfileManager.SHELTER),
    close: () -> Unit = {},
    done: () -> Unit = {},
) {
    SetupScreen(
        role = stringResource(R.string.role_main), targets = listOf(workCopy), samsung = true,
        access = ProfileAccess.REQUESTABLE, managers = managers, openManager = {},
        main = true, managed = false, selectedTarget = workCopy, shareRoute = false,
        applyPreset = { _, _ -> }, refresh = {}, requestAccess = {}, openLinkSettings = {},
        prepareTest = {}, message = null, close = close, done = done, initialStep = initialStep,
    )
}
