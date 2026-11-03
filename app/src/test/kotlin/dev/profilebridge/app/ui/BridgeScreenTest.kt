package dev.profilebridge.app.ui

import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.profilebridge.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BridgeScreenTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivity()).around(compose)

    private fun text(id: Int) = compose.activity.getString(id)
    private fun sendTo(copy: Int) = compose.activity.getString(R.string.profile_send, copy)

    @Test
    fun withoutLinkThereIsNothingToSend() {
        compose.setContent { HomeFixture(targets = listOf(workCopy, secureFolderCopy)) }
        compose.onNodeWithText(sendTo(1)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.home_share_hint)).assertExists()
    }

    @Test
    fun linkOffersOneSendButtonPerCopy() {
        val sent = mutableListOf<UserHandle>()
        compose.setContent {
            HomeFixture(url = PUBLIC_LINK, targets = listOf(workCopy, secureFolderCopy), sendToCopy = { sent += it })
        }
        compose.onNodeWithText(sendTo(1)).assertExists()
        compose.onNodeWithText(sendTo(2)).performScrollTo().performClick()
        assertEquals(listOf(secureFolderCopy), sent)
    }

    @Test
    fun knoxRouteOffersOnlyTheSystemShareSheet() {
        var shared = 0
        compose.setContent { HomeFixture(url = PUBLIC_LINK, shareRoute = true, shareLink = { shared++ }) }
        compose.onNodeWithText(sendTo(1)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.share_link_title)).performScrollTo().performClick()
        assertEquals(1, shared)
    }

    @Test
    fun backOnSettingsReturnsHome() {
        val selected = mutableListOf<Int>()
        compose.setContent { HomeFixture(tab = 2, selectTab = { selected += it }) }
        compose.pressBack()
        assertEquals(listOf(0), selected)
        assertFalse(compose.activity.isFinishing)
    }

    @Test
    fun backOnHomeIsLeftToTheSystem() {
        val selected = mutableListOf<Int>()
        compose.setContent { HomeFixture(tab = 0, selectTab = { selected += it }) }
        compose.pressBack()
        assertTrue(selected.isEmpty())
        assertTrue("Home does not swallow back", compose.activity.isFinishing)
    }
}
