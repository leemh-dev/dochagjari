package dev.leemh.dochagjari

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class StoreVisitInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Before fun openDeveloperTools() {
        ui.waitUntil(10_000) { androidx.lifecycle.ViewModelProvider(ui.activity)[AppModel::class.java].state.value.vehicle.stopped == true }
        ui.onNodeWithText("개발자 도구").performClick()
    }
    @Test fun storeLookupAndPreparedVisitAreReallyStoredAndSurviveActivityRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = AppServices.get(ui.activity.application).guidance!!
        ui.waitUntil(10_000) { !repo.state.value.loading && repo.state.value.catalogVersion.isNotBlank() }
        ui.onNodeWithText("매장 방문", useUnmergedTree = true).performClick()
        ui.onNodeWithText("저장된 매장 찾기").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.matches.isNotEmpty() }
        ui.onNodeWithText("현대백화점 판교점 · 5F 에뜨와").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("방문 준비 · 저장").performScrollTo().performClick()
        ui.waitUntil(10_000) { !repo.state.value.saving && repo.state.value.visit != null }
        val before = SqliteGuidanceStorage(context).use { it.currentVisit()!! }
        assertEquals(repo.state.value.visit, before)
        assertEquals("hyundai-pangyo-ettoi", before.destinationId)
        assertNull(before.parking)
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("매장 방문", useUnmergedTree = true).performClick()
        ui.onNodeWithText("방문 ID · ${before.id}").performScrollTo().assertIsDisplayed()
        SqliteGuidanceStorage(context).use { assertEquals(before, it.currentVisit()) }
    }
}
