package dev.leemh.dochagjari

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ScenarioInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun scenarioRecommendationsDoNotReplaceStoreVisitOrNotificationAndCloseOnMovement() {
        lateinit var model: AppModel
        ui.runOnIdle { model = ViewModelProvider(ui.activity)[AppModel::class.java]; model.testStopped(true) }
        val services = AppServices.get(ui.activity.application)
        val occupants = requireNotNull(services.occupants)
        ui.waitUntil(10_000) { !services.guidance!!.state.value.loading && occupants.state.value.status == ProfileSaveStatus.SAVED }
        ui.runOnIdle { model.endOccupantTrip() }
        ui.waitUntil(10_000) { occupants.state.value.status == ProfileSaveStatus.SAVED && occupants.state.value.active == null }
        val visit = services.guidance!!.state.value.visit
        val outbox = services.outbox.state.value.record
        ui.onNodeWithText("학원 픽업").performClick()
        ui.onNodeWithTag("arrival-road-map").assertIsDisplayed()
        ui.onNodeWithText("서쪽 승하차점 (시연 좌표)").assertIsDisplayed()
        ui.onNodeWithText("이용 불가 · TEST").performClick()
        ui.onNodeWithText("뒤편 주차장 입구 (시연 좌표)").assertIsDisplayed()
        ui.onNodeWithText("이용 불가 · TEST").performClick()
        ui.onNodeWithText("조건에 맞는 후보가 없습니다").assertIsDisplayed()
        ui.onNodeWithText("모의 안내").assertIsNotEnabled()
        ui.onNodeWithText("닫기").performClick()
        ui.onNodeWithText("병원 동행").performClick()
        ui.onNodeWithText("함께 주차").performClick()
        ui.onNodeWithText("동반 주차 구역 입구").assertIsDisplayed()
        ui.onNodeWithText("모의 안내").performClick()
        assertTrue(model.state.value.naviStatus.startsWith("모의 내비 요청:"))
        ui.runOnIdle { model.testStopped(null) }
        ui.onNodeWithTag("scenario-guide").assertDoesNotExist()
        ui.runOnIdle { model.testStopped(true) }
        ui.onNodeWithTag("scenario-guide").assertDoesNotExist()
        assertEquals(visit, services.guidance!!.state.value.visit)
        assertEquals(outbox, services.outbox.state.value.record)
    }
}
