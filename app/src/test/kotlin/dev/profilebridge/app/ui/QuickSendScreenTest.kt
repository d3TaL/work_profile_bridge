package dev.profilebridge.app.ui

import androidx.activity.ComponentActivity
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
class QuickSendScreenTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivity()).around(compose)

    private fun text(id: Int) = compose.activity.getString(id)

    @Test
    fun askModeSendsWithOneTap() {
        val actions = RecordingActions()
        compose.setContent {
            QuickSendScreen(readyState.copy(destination = Destination.ASK, url = PUBLIC_LINK, linkApp = "Ozon"), actions)
        }
        compose.onNodeWithText("Ozon").assertExists()
        compose.onNodeWithText(text(R.string.send_work)).performScrollTo().performClick()
        assertEquals(listOf(workCopy), actions.sentTo)
    }

    @Test
    fun cancelAndBackCloseWithoutSending() {
        val actions = RecordingActions()
        compose.setContent { QuickSendScreen(readyState.copy(destination = Destination.ASK, url = PUBLIC_LINK), actions) }
        compose.onNodeWithText(text(R.string.cancel)).performScrollTo().performClick()
        compose.pressBack()
        assertEquals(listOf("close", "close"), actions.calls)
        assertEquals(emptyList<Any>(), actions.sentTo)
    }
}
