package dev.leemh.dochagjari

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class SeatRolesInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Before fun openDeveloperTools() {
        ui.waitUntil(10_000) { androidx.lifecycle.ViewModelProvider(ui.activity)[AppModel::class.java].state.value.vehicle.stopped == true }
        ui.onNodeWithText("개발자 도구").performClick()
    }
    @Test fun optionalRolesAreDurableAndSkipPreservesVisitAndLegacyOptions() {
        val services = AppServices.get(ui.activity.application)
        val repository = services.profiles
        val guidance = requireNotNull(services.guidance)
        lateinit var model: AppModel
        ui.runOnIdle { model = ViewModelProvider(ui.activity)[AppModel::class.java] }
        ui.waitUntil(10_000) { model.state.value.vehicle.stopped == true && repository.state.value.status == ProfileSaveStatus.SAVED }
        ui.onNodeWithText("매장 방문").performClick()
        ui.onNodeWithText("저장된 매장 찾기").performScrollTo().performClick()
        ui.waitUntil(10_000) { guidance.state.value.matches.isNotEmpty() }
        ui.onNodeWithText("방문 준비 · 저장").performScrollTo().performClick()
        ui.waitUntil(10_000) { guidance.state.value.visit != null && !guidance.state.value.saving }
        val visit = guidance.state.value.visit
        val sessionId = model.state.value.session.id
        val notification = services.outbox.state.value.record
        val legacy = Profile(true, true, true, true, true, 2.3)
        ui.runOnIdle { model.profile(legacy) }
        ui.waitUntil(10_000) { repository.state.value.status == ProfileSaveStatus.SAVED && repository.state.value.persisted == legacy }
        ui.onNodeWithText("좌석·조건").performClick()
        ui.onNodeWithText("왼쪽 영유아").performScrollTo().performClick()
        ui.onNodeWithText("왼쪽 카시트 사용").performScrollTo().performClick()
        ui.onNodeWithText("오른쪽 성인").performScrollTo().performClick()
        ui.waitUntil(10_000) { repository.state.value.status == ProfileSaveStatus.SAVED && repository.state.value.persisted?.seats?.right?.role == SeatRole.ADULT }
        val configured = runBlocking { repository.readStoredProfile() }
        assertEquals(SeatRole.INFANT, configured.seats.left.role)
        assertTrue(configured.seats.left.childSeat)
        assertEquals(SeatRole.ADULT, configured.seats.right.role)
        assertEquals(legacy, configured.copy(seats = SeatPreferences()))
        ui.onNodeWithText("건너뛰기 · 설정 없이 안내").performScrollTo().performClick()
        ui.waitUntil(10_000) { repository.state.value.status == ProfileSaveStatus.SAVED && repository.state.value.persisted?.seats?.setup == SeatSetup.SKIPPED }
        val skipped = runBlocking { repository.readStoredProfile() }
        assertEquals(configured.seats.left, skipped.seats.left); assertEquals(configured.seats.right, skipped.seats.right)
        assertEquals(visit, guidance.state.value.visit); assertEquals(sessionId, model.state.value.session.id)
        assertEquals(notification, services.outbox.state.value.record)
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("매장 방문").performClick()
        ui.onNodeWithText("5층 동선 보기").performScrollTo().performClick()
        ui.onNodeWithTag("indoor-guide").assertIsDisplayed()
        ui.onNodeWithText("닫기").performClick()
        assertEquals(skipped, runBlocking { repository.readStoredProfile() })
    }
    @Test fun occupancyNeverChangesRolesAndDrivingBlocksRoleWrites() {
        lateinit var model: AppModel
        ui.runOnIdle { model = ViewModelProvider(ui.activity)[AppModel::class.java] }
        ui.waitUntil(10_000) { model.state.value.vehicle.stopped == true && model.state.value.profileSave.status == ProfileSaveStatus.SAVED }
        val expected = model.state.value.profile.copy(seats = SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.INFANT, true)))
        ui.runOnIdle { model.profile(expected) }
        ui.waitUntil(10_000) { model.state.value.profileSave.persisted == expected && model.state.value.profileSave.status == ProfileSaveStatus.SAVED }
        ui.runOnIdle { model.testSeat(true, Occupancy.OCCUPIED) }
        ui.waitUntil(5000) { model.state.value.vehicle.left == Occupancy.OCCUPIED }
        assertEquals(expected, model.state.value.profile)
        ui.runOnIdle { model.testStopped(false) }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == false }
        ui.runOnIdle { model.profile(expected.copy(seats = SeatPreferences(setup = SeatSetup.SKIPPED))) }
        assertEquals(expected, model.state.value.profile)
        ui.runOnIdle { model.testStopped(null) }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == null }
        ui.runOnIdle { model.profile(expected.copy(seats = SeatPreferences())) }
        assertEquals(expected, runBlocking { AppServices.get(ui.activity.application).profiles.readStoredProfile() })
        ui.runOnIdle { model.testStopped(true) }
    }
}
