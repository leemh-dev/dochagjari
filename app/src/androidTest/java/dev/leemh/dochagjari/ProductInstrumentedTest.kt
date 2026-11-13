package dev.leemh.dochagjari

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProductInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun productMapCloseMovementRecreateKeepPersistedVisitAndSettings() {
        lateinit var model: AppModel
        ui.runOnIdle { model = ViewModelProvider(ui.activity)[AppModel::class.java] }
        val services = AppServices.get(ui.activity.application)
        val repo = requireNotNull(services.guidance)
        ui.waitUntil(10_000) { !repo.state.value.loading && model.state.value.vehicle.stopped == true && services.profiles.state.value.status == ProfileSaveStatus.SAVED }
        ui.onNodeWithTag("product-home").assertIsDisplayed()
        ui.onNodeWithText("탑승 설정").performClick()
        ui.onNodeWithText("탑승 역할 · 선택 사항").assertIsDisplayed()
        ui.onNodeWithText("닫기").performClick()
        if (repo.state.value.visit == null) ui.onNodeWithText("방문 준비").performClick()
        ui.waitUntil(10_000) { repo.state.value.visit != null && !repo.state.value.saving }
        val visit = repo.state.value.visit!!
        val profile = services.profiles.state.value.persisted
        val notification = services.outbox.state.value.record
        ui.onNodeWithText("5층 동선 보기").performScrollTo().performClick()
        ui.onNodeWithTag("product-map").assertIsDisplayed()
        ui.onNodeWithText("닫기").performClick()
        ui.onNodeWithTag("product-floor").assertDoesNotExist()
        ui.runOnIdle { model.testStopped(false) }
        ui.onNodeWithText("안전하게 이동하세요").assertIsDisplayed()
        ui.onNodeWithText("탑승 설정").assertIsNotEnabled()
        ui.runOnIdle { model.testStopped(true) }
        ui.onNodeWithTag("product-floor").assertDoesNotExist()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("product-home").assertIsDisplayed()
        ui.onNodeWithTag("product-floor").assertDoesNotExist()
        val saved = SqliteGuidanceStorage(ui.activity).use { it.currentVisit()!! }
        assertEquals(visit.id, saved.id); assertEquals(visit.catalogVersion, saved.catalogVersion)
        assertEquals(visit.journey.entry, saved.journey.entry)
        assertEquals(profile, services.profiles.state.value.persisted)
        assertEquals(notification, services.outbox.state.value.record)
    }
}
