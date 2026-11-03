package dev.profilebridge.app.ui

import dev.profilebridge.app.platform.LinkHandling
import dev.profilebridge.app.platform.ProfileAccess
import dev.profilebridge.app.ui.Check.LINKS
import dev.profilebridge.app.ui.Check.PROFILE_ACCESS
import dev.profilebridge.app.ui.Check.SECOND_COPY
import dev.profilebridge.app.ui.Check.SECURE_FOLDER_COPY
import dev.profilebridge.app.ui.CheckStatus.DONE
import dev.profilebridge.app.ui.CheckStatus.MANUAL
import dev.profilebridge.app.ui.CheckStatus.TODO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChecksTest {
    @Test
    fun workProfileNeedsCopyAccessAndLinks() {
        assertEquals(listOf(SECOND_COPY to TODO, PROFILE_ACCESS to TODO, LINKS to TODO), freshState.checks())
        assertEquals(listOf(SECOND_COPY to DONE, PROFILE_ACCESS to DONE, LINKS to DONE), readyState.checks())
    }

    @Test
    fun secureFolderCopyIsLeftToTheUser() {
        val checks = readyState.copy(destination = Destination.SECURE_FOLDER).checks()
        assertEquals(listOf(SECURE_FOLDER_COPY to MANUAL, LINKS to DONE), checks)
        assertEquals("A manual check does not block Ready", 0, checks.remaining)
    }

    @Test
    fun askNeedsBothDestinations() {
        val checks = freshState.copy(destination = Destination.ASK).checks().map { it.first }
        assertEquals(listOf(SECOND_COPY, PROFILE_ACCESS, SECURE_FOLDER_COPY, LINKS), checks)
    }

    @Test
    fun linkDomainsAreUnknownOnAndroid11AndPartialSelectionCounts() {
        assertEquals(LINKS to MANUAL, readyState.copy(links = null).checks().last())
        assertEquals(LINKS to DONE, readyState.copy(links = LinkHandling(5, 31)).checks().last())
    }

    @Test
    fun accessMustBeGranted() {
        assertEquals(PROFILE_ACCESS to TODO, readyState.copy(access = ProfileAccess.REQUESTABLE).checks()[1])
    }

    @Test
    fun receivingCopiesHaveNoSendChecks() {
        assertTrue(readyState.copy(space = Space.WORK_PROFILE).checks().isEmpty())
        assertTrue(readyState.copy(space = Space.SECURE_FOLDER).checks().isEmpty())
    }
}
