package dev.profilebridge.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.profilebridge.app.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransferScreenTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivity()).around(compose)

    private fun text(id: Int) = compose.activity.getString(id)

    @Test
    fun expiredTransferCannotBeRetried() {
        compose.setContent {
            TransferScreen("Ozon", failed = true, expired = true, retryEnabled = false, retry = {}, close = {})
        }
        compose.onNodeWithText(text(R.string.transfer_expired)).assertExists()
        compose.onNodeWithText(text(R.string.transfer_retry)).assertIsNotEnabled()
    }

    @Test
    fun backClosesWithoutRetrying() {
        var retries = 0
        var closes = 0
        compose.setContent {
            TransferScreen("Ozon", failed = false, expired = false, retryEnabled = true,
                retry = { retries++ }, close = { closes++ })
        }
        compose.pressBack()
        assertEquals(1, closes)
        assertEquals(0, retries)
    }
}
