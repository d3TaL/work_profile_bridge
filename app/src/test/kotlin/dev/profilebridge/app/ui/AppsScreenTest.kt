package dev.profilebridge.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.AppMode
import dev.profilebridge.app.platform.AppRules
import dev.profilebridge.core.links.BankLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppsScreenTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivity()).around(compose)

    private val applied = mutableListOf<Pair<Set<String>, AppMode>>()
    private var closed = 0
    private val bank = AppRules.apps.first { BankLinks.isPackage(it.packageName) }
    private val shop = AppRules.apps.first { !BankLinks.isPackage(it.packageName) }

    private fun text(id: Int) = compose.activity.getString(id)

    private fun show() = compose.setContent {
        AppsScreen(modes = emptyMap(), apply = { packages, mode -> applied += packages to mode },
            close = { closed++ }, selectTab = {})
    }

    private fun openMode(label: String) = compose.onNodeWithText(label).performScrollTo().performClick()

    private fun chooseInDialog(id: Int) =
        compose.onNode(hasText(text(id)) and hasAnyAncestor(isDialog())).performClick()

    @Test
    fun bankAutomaticModeWaitsForSeparateConsent() {
        show()
        openMode(bank.label)
        chooseInDialog(R.string.apps_auto)

        compose.onNodeWithText(text(R.string.bank_auto_title)).assertIsDisplayed()
        assertTrue("Nothing is saved before consent", applied.isEmpty())

        chooseInDialog(R.string.cancel)
        compose.onNodeWithText(text(R.string.bank_auto_title)).assertDoesNotExist()
        assertTrue("Cancelled consent saves nothing", applied.isEmpty())
    }

    @Test
    fun acceptedBankConsentAppliesOnlyTheChosenBank() {
        show()
        openMode(bank.label)
        chooseInDialog(R.string.apps_auto)
        chooseInDialog(R.string.bank_auto_accept)

        assertEquals(listOf(setOf(bank.packageName) to AppMode.AUTOMATIC), applied)
    }

    @Test
    fun shopModeIsAppliedWithoutBankConsent() {
        show()
        openMode(shop.label)
        chooseInDialog(R.string.apps_disabled)

        compose.onNodeWithText(text(R.string.bank_auto_title)).assertDoesNotExist()
        assertEquals(listOf(setOf(shop.packageName) to AppMode.DISABLED), applied)
    }

    @Test
    fun backLeavesSelectionModeBeforeLeavingTheScreen() {
        show()
        compose.onNodeWithText(text(R.string.apps_select)).performClick()
        compose.onNodeWithText(text(R.string.apps_edit)).assertExists()

        compose.pressBack()
        compose.onNodeWithText(text(R.string.apps_edit)).assertDoesNotExist()
        assertEquals(0, closed)

        compose.pressBack()
        assertEquals(1, closed)
    }
}
