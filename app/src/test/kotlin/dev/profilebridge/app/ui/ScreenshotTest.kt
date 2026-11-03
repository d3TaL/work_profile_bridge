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
import dev.profilebridge.app.platform.LocalHandler
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
 * run skips the capture. Light/dark for every screen; font 150 %, half of a split screen and
 * landscape for the text-heavy screens.
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
    fun homeWithoutLink() = capture("home_empty", themes) { HomeFixture() }

    @Test
    fun homeWithLink() = capture("home_link", Variant.entries) {
        HomeFixture(url = PUBLIC_LINK, targets = listOf(workCopy, secureFolderCopy),
            handlers = listOf(LocalHandler("ru.ozon.app.android")))
    }

    @Test
    fun settings() = capture("settings", themes) { HomeFixture(tab = 2, targets = listOf(workCopy, secureFolderCopy)) }

    @Test
    fun apps() = capture("apps", themes + Variant.FONT150 + Variant.SPLIT) {
        AppsScreen(modes = sampleModes, apply = { _, _ -> }, close = {}, selectTab = {})
    }

    @Test
    fun setupRoutes() = capture("setup_routes", themes) { SetupFixture() }

    @Test
    fun setupAddCopy() = capture("setup_add_copy", themes + Variant.FONT150) { SetupFixture(initialStep = 3) }

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
