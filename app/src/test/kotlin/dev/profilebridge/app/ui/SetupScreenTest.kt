package dev.profilebridge.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.profilebridge.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupScreenTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivity()).around(compose)

    private fun text(id: Int) = compose.activity.getString(id)

    private class Calls {
        var closed = 0
        var done = 0
        var tried = 0
    }

    private fun show(state: BridgeState, initialStep: Int = 0): Pair<Calls, () -> RecordingActions> {
        val calls = Calls()
        var recorded: RecordingActions? = null
        compose.setContent {
            Stateful(state, { update -> RecordingActions(update).also { recorded = it } }) { current, actions ->
                SetupScreen(current, actions, close = { calls.closed++ }, done = { calls.done++ },
                    tryLink = { calls.tried++ }, initialStep = initialStep)
            }
        }
        return calls to { checkNotNull(recorded) }
    }

    @Test
    fun choosingWhereLinksGoIsTheStepAction() {
        val (_, actions) = show(freshState)
        compose.onNodeWithText(text(R.string.setup_where)).assertExists()
        compose.onNodeWithText(text(R.string.dest_ask)).performScrollTo().performClick()

        assertEquals(listOf("destination:${Destination.ASK}"), actions().calls)
        compose.onNodeWithText(text(R.string.prepare_title)).assertExists()
        compose.onNodeWithText(text(R.string.check_secure)).assertExists()
    }

    @Test
    fun backReturnsToPreviousStepThenLeavesSetup() {
        val (calls, _) = show(freshState, initialStep = 1)
        compose.onNodeWithText(text(R.string.prepare_title)).assertExists()

        compose.pressBack()
        compose.onNodeWithText(text(R.string.setup_where)).assertExists()
        assertEquals(0, calls.closed)

        compose.pressBack()
        assertEquals(1, calls.closed)
    }

    @Test
    fun currentStepSurvivesSavedStateRestore() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SetupScreen(freshState, RecordingActions(), close = {}, done = {}, tryLink = {})
        }
        compose.onNodeWithText(text(R.string.dest_work)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.prepare_title)).assertExists()

        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(text(R.string.prepare_title)).assertExists()
    }

    @Test
    fun lastStepOffersTryAndFinish() {
        val (calls, _) = show(readyState, initialStep = 2)
        compose.onNodeWithText(text(R.string.skip)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.setup_try)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.finish)).performScrollTo().performClick()
        assertEquals(1, calls.tried)
        assertEquals(1, calls.done)
    }

    @Test
    fun skipFinishesSetup() {
        val (calls, _) = show(freshState)
        compose.onNodeWithText(text(R.string.skip)).performClick()
        assertEquals(1, calls.done)
    }

    @Test
    fun receivingCopyAsksSpaceThenOpening() {
        val (calls, actions) = show(readyState.copy(space = Space.WORK_PROFILE))
        compose.onNodeWithText(text(R.string.setup_space)).assertExists()
        compose.onNodeWithText(text(R.string.dest_work)).performScrollTo().performClick()

        compose.onNodeWithText(text(R.string.setup_open)).assertExists()
        compose.onNodeWithText(text(R.string.open_now)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.finish)).performScrollTo().performClick()

        assertEquals(listOf("space:${Space.WORK_PROFILE}", "open:true"), actions().calls)
        assertTrue(calls.done == 1)
    }
}
