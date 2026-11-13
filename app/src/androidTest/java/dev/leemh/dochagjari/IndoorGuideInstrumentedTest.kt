package dev.leemh.dochagjari

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class IndoorGuideInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private fun prepare(): VisitRecord {
        val repo = AppServices.get(ui.activity.application).guidance!!
        ui.waitUntil(10_000) { !repo.state.value.loading && repo.state.value.catalogVersion.isNotBlank() }
        ui.onNodeWithText("매장 방문").performClick()
        ui.onNodeWithText("저장된 매장 찾기").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.matches.isNotEmpty() }
        ui.onNodeWithText("방문 준비 · 저장").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.visit != null && !repo.state.value.saving }
        return SqliteGuidanceStorage(ui.activity).use { it.currentVisit()!! }
    }
    private fun progress() = ui.onNodeWithTag("route-progress").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
    @Test fun nativeRouteMovesPausesChangesPerspectiveAndClosesWithoutLosingVisit() {
        val original = prepare()
        val repo = AppServices.get(ui.activity.application).guidance!!
        ui.onNodeWithText("5층 동선 보기").performScrollTo().performClick()
        ui.onNodeWithTag("indoor-guide").assertIsDisplayed()
        ui.waitForIdle()
        // Disable automatic clock advancement to inspect intermediate frames, not only the final image.
        if (ui.onAllNodesWithText("일시정지").fetchSemanticsNodes().isNotEmpty()) ui.onNodeWithText("일시정지").performClick()
        ui.mainClock.autoAdvance = false
        ui.onNodeWithText("재생").performClick(); ui.mainClock.advanceTimeBy(1500)
        val moving = Regex("(\\d+)%").find(progress())!!.groupValues[1].toInt()
        assertTrue("Intermediate path progress expected: $moving", moving in 1..99)
        ui.onNodeWithText("일시정지").performClick(); ui.mainClock.advanceTimeByFrame()
        val paused = progress(); ui.mainClock.advanceTimeBy(1000); assertEquals(paused, progress())
        ui.onNodeWithText("평면 보기").performClick(); ui.mainClock.advanceTimeByFrame()
        ui.onNodeWithTag("indoor-floor-map").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "평면 개략도"))
        ui.onNodeWithText("기울여 보기").performClick(); ui.mainClock.advanceTimeByFrame()
        ui.onNodeWithTag("indoor-floor-map").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "기울인 개략도"))
        ui.mainClock.autoAdvance = true
        ui.onNodeWithText("닫기").performClick()
        ui.waitUntil(10_000) { repo.state.value.visit?.visibility == GuideVisibility.HIDDEN && !repo.state.value.saving }
        SqliteGuidanceStorage(ui.activity).use { assertEquals(original.copy(visibility = GuideVisibility.HIDDEN), it.currentVisit()) }
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("indoor-guide").assertDoesNotExist()
        ui.onNodeWithText("매장 방문").performClick()
        ui.onNodeWithText("방문 ID · ${original.id}").performScrollTo().assertIsDisplayed()
    }
    @Test fun movingOrUnknownVehicleStateClosesAndCannotReopenDetailedMap() {
        val original = prepare()
        ui.onNodeWithText("5층 동선 보기").performScrollTo().performClick()
        ui.onNodeWithTag("indoor-guide").assertIsDisplayed()
        lateinit var model: AppModel
        ui.runOnIdle { model = ViewModelProvider(ui.activity)[AppModel::class.java]; model.testStopped(false) }
        ui.waitUntil(5000) { !model.state.value.showIndoorGuide && model.state.value.vehicle.stopped == false }
        ui.onNodeWithTag("indoor-guide").assertDoesNotExist()
        ui.runOnIdle { model.openIndoorGuide(); model.testStopped(null) }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == null }
        ui.runOnIdle { model.openIndoorGuide() }
        assertFalse(model.state.value.showIndoorGuide)
        assertEquals(original.id, model.state.value.guidance.visit!!.id)
    }
}
