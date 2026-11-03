package dev.profilebridge.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.profilebridge.app.R
import org.junit.Assert.assertEquals
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
    private fun step(number: Int) = compose.activity.getString(R.string.wizard_step, number, 9)
    private fun next() = compose.onNodeWithText(text(R.string.setup_continue)).performScrollTo().performClick()

    @Test
    fun backReturnsToPreviousStepThenLeavesWizard() {
        var closed = 0
        compose.setContent { SetupFixture(close = { closed++ }) }
        next()
        compose.onNodeWithText(text(R.string.setup_modes_title)).assertExists()

        compose.pressBack()
        compose.onNodeWithText(text(R.string.setup_routes_title)).assertExists()
        assertEquals(0, closed)

        compose.pressBack()
        assertEquals(1, closed)
    }

    @Test
    fun currentStepSurvivesSavedStateRestore() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { SetupFixture() }
        next()
        compose.onNodeWithText(step(2)).assertExists()

        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(step(2)).assertExists()
        compose.onNodeWithText(text(R.string.setup_modes_title)).assertExists()
    }

    @Test
    fun lastStepFinishesWizardOnce() {
        var done = 0
        compose.setContent { SetupFixture(initialStep = 8, done = { done++ }) }
        compose.onNodeWithText(step(9)).assertExists()
        compose.onNodeWithText(text(R.string.wizard_done)).performScrollTo().performClick()
        assertEquals(1, done)
    }
}
