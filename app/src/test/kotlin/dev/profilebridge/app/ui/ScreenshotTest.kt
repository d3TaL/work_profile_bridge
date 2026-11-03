package dev.profilebridge.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screen references. They run only in Roborazzi record/verify/compare mode; a plain unit test
 * run skips the capture. Light/dark for the screens; font 150 %, half of a split screen and
 * landscape where text or layout may break.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    private enum class Variant(val qualifiers: String, val fontScale: Float = 1f) {
        LIGHT("w400dp-h900dp-port-notnight-hdpi"),
        DARK("w400dp-h900dp-port-night-hdpi"),
        FONT150("w400dp-h900dp-port-notnight-hdpi", fontScale = 1.5f),
        SPLIT("w400dp-h500dp-port-notnight-hdpi"),
        LANDSCAPE("w900dp-h400dp-land-notnight-hdpi"),
    }

    private val themes = listOf(Variant.LIGHT, Variant.DARK)

    private fun capture(name: String, variants: List<Variant>, content: @Composable () -> Unit) {
        for (variant in variants) {
            RuntimeEnvironment.setQualifiers(variant.qualifiers)
            captureRoboImage("${name}_${variant.name.lowercase()}.png") {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, variant.fontScale), content = content)
            }
        }
    }

    @Test
    fun homeReady() = capture("home_ready", Variant.entries) { MainScreen(0, readyState, RecordingActions()) }

    @Test
    fun homeNeedsSteps() = capture("home_steps", themes) { MainScreen(0, freshState, RecordingActions()) }

    @Test
    fun homeLinkAsk() = capture("home_link_ask", themes + Variant.FONT150) {
        MainScreen(0, readyState.copy(destination = Destination.ASK, url = PUBLIC_LINK, linkApp = "Ozon",
            handlers = listOf(ozonHandler)), RecordingActions())
    }

    @Test
    fun settingsWorkProfile() = capture("settings_work", themes + Variant.FONT150) {
        MainScreen(2, freshState, RecordingActions())
    }

    @Test
    fun settingsSecureFolder() = capture("settings_secure", themes) {
        MainScreen(2, readyState.copy(destination = Destination.SECURE_FOLDER), RecordingActions())
    }

    @Test
    fun receiverHome() = capture("receiver_home", themes) {
        MainScreen(0, readyState.copy(space = Space.SECURE_FOLDER, openImmediately = true), RecordingActions())
    }

    @Test
    fun receiverSettings() = capture("receiver_settings", themes) {
        MainScreen(2, readyState.copy(space = Space.WORK_PROFILE, openImmediately = true), RecordingActions())
    }

    @Test
    fun apps() = capture("apps", themes + Variant.FONT150 + Variant.SPLIT) {
        AppsScreen(modes = sampleModes, apply = { _, _ -> }, close = {}, selectTab = {})
    }

    @Test
    fun setupWhere() = capture("setup_where", themes + Variant.FONT150) {
        SetupScreen(readyState, RecordingActions(), close = {}, done = {}, tryLink = {})
    }

    @Test
    fun setupPrepare() = capture("setup_prepare", themes) {
        SetupScreen(freshState.copy(destination = Destination.ASK), RecordingActions(), close = {}, done = {},
            tryLink = {}, initialStep = 1)
    }

    @Test
    fun setupDone() = capture("setup_done", listOf(Variant.LIGHT)) {
        SetupScreen(readyState, RecordingActions(), close = {}, done = {}, tryLink = {}, initialStep = 2)
    }

    @Test
    fun setupReceiver() = capture("setup_receiver", listOf(Variant.LIGHT)) {
        SetupScreen(readyState.copy(space = Space.SECURE_FOLDER), RecordingActions(), close = {}, done = {},
            tryLink = {}, initialStep = 1)
    }

    @Test
    fun quickSend() = capture("quick_send", listOf(Variant.SPLIT, Variant.DARK)) {
        QuickSendScreen(readyState.copy(destination = Destination.ASK, url = PUBLIC_LINK, linkApp = "Ozon"),
            RecordingActions())
    }

    @Test
    fun transferExpired() = capture("transfer_expired", themes + Variant.FONT150) {
        TransferScreen("Ozon", failed = true, expired = true, retryEnabled = false, retry = {}, close = {})
    }
}

/** The predictive back frame: a cancelled gesture must leave the screen in place. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-port-notnight-hdpi")
class PredictiveBackScreenshotTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivity()).around(compose)

    @Test
    fun appsScreenFollowsBackGesture() {
        var closed = 0
        compose.setContent { AppsScreen(modes = sampleModes, apply = { _, _ -> }, close = { closed++ }, selectTab = {}) }
        val input = DirectNavigationEventInput()
        compose.runOnUiThread {
            compose.activity.navigationEventDispatcher.addInput(input)
            input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT))
            input.backProgressed(NavigationEvent(progress = 0.6f, swipeEdge = NavigationEvent.EDGE_LEFT))
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("apps_back_gesture.png")

        compose.runOnUiThread { input.backCancelled() }
        compose.waitForIdle()
        assertEquals(0, closed)
    }
}
