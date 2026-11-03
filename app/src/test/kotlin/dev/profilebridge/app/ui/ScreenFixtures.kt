package dev.profilebridge.app.ui

import android.app.Application
import android.content.ComponentName
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.navigationevent.DirectNavigationEventInput
import androidx.test.core.app.ApplicationProvider
import dev.profilebridge.app.platform.AppMode
import dev.profilebridge.app.platform.LinkHandling
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
val secondWorkCopy: UserHandle = UserHandle.getUserHandleForUid(11 * 100_000)

/** Main copy with everything prepared for the work profile. */
val readyState = BridgeState(
    space = Space.MAIN, destination = Destination.WORK_PROFILE, copies = listOf(workCopy),
    access = ProfileAccess.GRANTED, managers = listOf(ProfileManager.SHELTER), links = LinkHandling(31, 31),
    samsung = true, version = "test",
)

/** Main copy right after installation: no second copy, no access, no link domains. */
val freshState = readyState.copy(copies = emptyList(), access = ProfileAccess.NOT_AVAILABLE,
    links = LinkHandling(0, 31))

val sampleModes: Map<String, AppMode> = mapOf(
    "ru.ozon.app.android" to AppMode.AUTOMATIC,
    "com.wildberries.ru" to AppMode.CONFIRM,
    "ru.sberbankmobile" to AppMode.DISABLED,
)

val ozonHandler = LocalHandler("ru.ozon.app.android")

/** Records what the screens ask for; optionally applies it to [state] like the activity does. */
class RecordingActions(private val update: ((BridgeState) -> BridgeState) -> Unit = {}) : BridgeActions {
    val calls = mutableListOf<String>()
    val tabs = mutableListOf<Int>()
    val sentTo = mutableListOf<UserHandle>()

    override fun selectTab(tab: Int) { tabs += tab }
    override fun openSetup() { calls += "setup" }
    override fun setDestination(destination: Destination) {
        calls += "destination:$destination"
        update { it.copy(destination = destination) }
    }
    override fun requestAccess() { calls += "access" }
    override fun openManager(manager: ProfileManager) { calls += "manager:$manager" }
    override fun openLinkSettings() { calls += "links" }
    override fun setSpace(space: Space) {
        calls += "space:$space"
        update { it.copy(space = space) }
    }
    override fun setOpenImmediately(enabled: Boolean) {
        calls += "open:$enabled"
        update { it.copy(openImmediately = enabled) }
    }
    override fun sendToCopy(copy: UserHandle) { sentTo += copy }
    override fun sendToSecureFolder() { calls += "secure" }
    override fun closeQuickSend() { calls += "close" }
}

/** Holds the state in Compose so a tapped option changes what the screen shows. */
@Composable
fun Stateful(initial: BridgeState, actions: (((BridgeState) -> BridgeState) -> Unit) -> RecordingActions,
             content: @Composable (BridgeState, BridgeActions) -> Unit) {
    var state by remember { mutableStateOf(initial) }
    val recording = remember { actions { update -> state = update(state) } }
    content(state, recording)
}

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
