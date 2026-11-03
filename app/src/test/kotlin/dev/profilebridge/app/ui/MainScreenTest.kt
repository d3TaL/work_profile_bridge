package dev.profilebridge.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.ProfileAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainScreenTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivity()).around(compose)

    private fun text(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun show(tab: Int, state: BridgeState, actions: RecordingActions = RecordingActions()): RecordingActions {
        compose.setContent { MainScreen(tab, state, actions) }
        return actions
    }

    private fun showStateful(tab: Int, state: BridgeState): () -> RecordingActions {
        var recorded: RecordingActions? = null
        compose.setContent {
            Stateful(state, { update -> RecordingActions(update).also { recorded = it } }) { current, actions ->
                MainScreen(tab, current, actions)
            }
        }
        return { checkNotNull(recorded) }
    }

    @Test
    fun statusCountsRemainingStepsUntilReady() {
        show(0, freshState)
        compose.onNodeWithText(text(R.string.status_left, 3)).assertExists()
        compose.onNodeWithText(text(R.string.status_finish)).assertExists()
    }

    @Test
    fun preparedWorkProfileIsReady() {
        show(0, readyState)
        compose.onNodeWithText(text(R.string.status_ready)).assertExists()
        compose.onNodeWithText(text(R.string.summary_work)).assertExists()
    }

    @Test
    fun workModeOffersOnlyTheWorkProfile() {
        val actions = show(0, readyState.copy(url = PUBLIC_LINK, linkApp = "Ozon"))
        compose.onNodeWithText(text(R.string.send_secure)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.send_work)).performScrollTo().performClick()
        assertEquals(listOf(workCopy), actions.sentTo)
    }

    @Test
    fun askModeOffersBothDestinations() {
        val actions = show(0, readyState.copy(destination = Destination.ASK, url = PUBLIC_LINK))
        compose.onNodeWithText(text(R.string.send_work)).assertExists()
        compose.onNodeWithText(text(R.string.send_secure)).performScrollTo().performClick()
        assertEquals(listOf("secure"), actions.calls)
    }

    @Test
    fun missingCopyIsExplainedInsteadOfADisabledButton() {
        show(0, freshState.copy(url = PUBLIC_LINK))
        compose.onNodeWithText(text(R.string.send_work)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.send_no_copy)).assertExists()
    }

    @Test
    fun switchingDestinationShowsOnlyItsChecks() {
        val actions = showStateful(2, freshState)
        compose.onNodeWithText(text(R.string.check_access)).assertExists()
        compose.onNodeWithText(text(R.string.check_secure)).assertDoesNotExist()

        compose.onNodeWithText(text(R.string.dest_secure)).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf("destination:${Destination.SECURE_FOLDER}"), actions().calls)
        compose.onNodeWithText(text(R.string.check_secure)).assertExists()
        compose.onNodeWithText(text(R.string.check_access)).assertDoesNotExist()
    }

    @Test
    fun requestableAccessOffersAWorkingAllowButton() {
        val actions = show(2, freshState.copy(access = ProfileAccess.REQUESTABLE))
        compose.onNodeWithText(text(R.string.allow)).performScrollTo().performClick()
        assertTrue("access" in actions.calls)
    }

    @Test
    fun secureFolderOpenImmediatelyNeedsConsent() {
        val actions = showStateful(2, readyState.copy(space = Space.SECURE_FOLDER))
        compose.onNodeWithText(text(R.string.open_now)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.open_now_consent_title)).assertExists()
        assertTrue("Nothing is enabled before consent", actions().calls.isEmpty())

        compose.onNode(hasText(text(R.string.enable)) and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("open:true"), actions().calls)
    }

    @Test
    fun workProfileOpenImmediatelyNeedsNoDialog() {
        val actions = showStateful(2, readyState.copy(space = Space.WORK_PROFILE))
        compose.onNodeWithText(text(R.string.open_now)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.open_now_consent_title)).assertDoesNotExist()
        assertEquals(listOf("open:true"), actions().calls)
    }

    @Test
    fun backOnSettingsReturnsHome() {
        val actions = show(2, readyState)
        compose.pressBack()
        assertEquals(listOf(0), actions.tabs)
        assertFalse(compose.activity.isFinishing)
    }

    @Test
    fun backOnHomeIsLeftToTheSystem() {
        val actions = show(0, readyState)
        compose.pressBack()
        assertTrue(actions.tabs.isEmpty())
        assertTrue("Home does not swallow back", compose.activity.isFinishing)
    }
}
