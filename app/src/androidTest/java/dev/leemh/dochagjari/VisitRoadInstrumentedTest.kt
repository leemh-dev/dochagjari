package dev.leemh.dochagjari

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VisitRoadInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private fun prepare(): AppModel {
        lateinit var model: AppModel
        ui.runOnIdle { model = ViewModelProvider(ui.activity)[AppModel::class.java] }
        val repo = requireNotNull(AppServices.get(ui.activity.application).guidance)
        ui.waitUntil(10_000) { !repo.state.value.loading && model.state.value.vehicle.stopped == true }
        ui.onNodeWithText("매장 방문").performClick()
        ui.onNodeWithText("저장된 매장 찾기").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.matches.isNotEmpty() }
        ui.onNodeWithText("방문 준비 · 저장").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.visit != null && !repo.state.value.saving }
        return model
    }
    @Test fun roadResultCancelDrivingExplicitEntryAndRestartKeepSameVisit() {
        val model = prepare(); val services = AppServices.get(ui.activity.application)
        val repo = requireNotNull(services.guidance)
        val original = repo.state.value.visit!!; val profile = model.state.value.profile; val notification = services.outbox.state.value.record
        ui.onNodeWithText("시설 목적지로 내비 요청").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.visit?.journey?.latest?.status == RoadStatus.MOCK_COMPLETED }
        assertNull(repo.state.value.visit!!.journey.entry)
        val first = repo.state.value.visit!!.journey.latest!!.id
        ui.onNodeWithText("앱 내비 연결 취소").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.visit?.phase == VisitPhase.PLANNED }
        repo.roadEvent(original.id, first, RoadStatus.MOCK_COMPLETED, "LATE", "ignored")
        ui.onNodeWithText("시설 목적지로 내비 요청").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.visit?.journey?.attempts?.size == 2 && repo.state.value.visit?.journey?.latest?.status == RoadStatus.MOCK_COMPLETED }
        ui.runOnIdle { model.testStopped(false) }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == false }
        ui.runOnIdle { model.confirmVisitEntry(true); model.requestVisitRoad() }
        assertNull(repo.state.value.visit!!.journey.entry); assertEquals(2, repo.state.value.visit!!.journey.attempts.size)
        ui.runOnIdle { model.testStopped(null) }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == null }
        ui.runOnIdle { model.confirmVisitEntry(false) }
        assertNull(repo.state.value.visit!!.journey.entry)
        ui.runOnIdle { model.testStopped(true) }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == true }

        ui.onNodeWithTag("indoor-guide").assertDoesNotExist()
        ui.onNodeWithText("입차 테스트 · 실제 감지 아님").performScrollTo().performClick()
        ui.waitUntil(10_000) { model.state.value.showIndoorGuide && repo.state.value.visit?.phase == VisitPhase.PARKING_GUIDANCE }
        ui.onNodeWithTag("indoor-guide").assertIsDisplayed()
        ui.onNodeWithText("닫기").performClick()
        ui.waitUntil(10_000) { repo.state.value.visit?.visibility == GuideVisibility.HIDDEN && !repo.state.value.saving }
        ui.runOnIdle { model.confirmVisitEntry(true, "repeated-test-entry") }
        ui.waitForIdle(); ui.onNodeWithTag("indoor-guide").assertDoesNotExist()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("indoor-guide").assertDoesNotExist()
        // Stage 5 extends this same explicit-entry visit, avoiding order-dependent new visits.
        lateinit var currentModel: AppModel
        ui.runOnIdle { currentModel = ViewModelProvider(ui.activity)[AppModel::class.java] }
        fun startTest() {
            val previous = repo.state.value.visit!!.observation.latest?.id
            ui.runOnIdle { currentModel.startVisitTestObservation() }
            ui.waitUntil(10_000) { repo.state.value.visit?.observation?.latest?.let { it.id != previous && it.source == VisitFrameSource.TEST } == true && currentModel.state.value.visitObservationMessage.contains("관측 중") }
        }
        fun feed(case: String) {
            val before = repo.state.value.visit!!.observation.latest!!.samples.size
            ui.runOnIdle { currentModel.feedVisitTestObservation(case) }
            ui.waitUntil(10_000) { (repo.state.value.visit?.observation?.latest?.samples?.size ?: 0) >= before + 2 && !repo.state.value.saving }
        }
        fun summary() = VisitObservationRules.summarize(repo.state.value.visit!!.observation, System.currentTimeMillis())
        startTest(); feed("B3")
        assertEquals(SignCandidate("B3", "C12"), summary().candidate)
        feed("B4"); assertTrue(summary().status.contains("상충")); assertNull(summary().candidate)
        startTest(); feed("LOW"); assertTrue(summary().status.contains("낮은"))
        startTest(); feed("STALE"); assertTrue(summary().status.contains("오래됨"))
        startTest(); feed("NONE"); assertTrue(summary().status.contains("표기 없음"))
        ui.runOnIdle { currentModel.startVisitVideo(asset = "generated-blocked.mp4") }
        ui.waitUntil(90_000) { !currentModel.state.value.observingVisit && repo.state.value.visit?.observation?.latest?.source == VisitFrameSource.GENERATED_VIDEO && repo.state.value.visit?.observation?.latest?.stopReason != null && !repo.state.value.saving }
        val video = repo.state.value.visit!!.observation.latest!!
        assertTrue("Actual Android OCR must return C12", video.samples.any { it.rawText.contains("C12") })
        assertTrue(video.samples.all { it.textConfidence == null })
        assertNull(repo.state.value.visit!!.parking)
        startTest(); feed("B3")
        ui.runOnIdle { currentModel.openIndoorGuide() }
        ui.waitUntil(10_000) { currentModel.state.value.showIndoorGuide }
        ui.onNodeWithText("닫기").performClick()
        ui.waitUntil(10_000) { !currentModel.state.value.observingVisit && repo.state.value.visit?.observation?.latest?.stopReason != null && !repo.state.value.saving }
        val count = repo.state.value.visit!!.observation.latest!!.samples.size
        ui.runOnIdle { currentModel.feedVisitTestObservation("B4") }
        ui.waitForIdle(); assertEquals(count, repo.state.value.visit!!.observation.latest!!.samples.size)
        startTest(); ui.runOnIdle { currentModel.testStopped(false) }
        ui.waitUntil(10_000) { !currentModel.state.value.observingVisit && repo.state.value.visit?.observation?.latest?.stopReason != null }
        ui.runOnIdle { currentModel.startVisitTestObservation() }; assertFalse(currentModel.state.value.observingVisit)
        ui.runOnIdle { currentModel.testStopped(true) }
        ui.activityRule.scenario.recreate()
        ui.runOnIdle { currentModel = ViewModelProvider(ui.activity)[AppModel::class.java]; assertFalse(currentModel.state.value.observingVisit) }
        val saved = SqliteGuidanceStorage(ui.activity).use { it.currentVisit()!! }
        assertEquals(original.id, saved.id); assertEquals(original.catalogVersion, saved.catalogVersion)
        assertEquals(GuidanceInputSource.TEST, saved.journey.entry!!.source); assertNull(saved.parking)
        assertEquals(profile, services.profiles.state.value.profile); assertEquals(notification, services.outbox.state.value.record)
    }
}
