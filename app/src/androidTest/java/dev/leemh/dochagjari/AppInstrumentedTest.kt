package dev.leemh.dochagjari

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class AppInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Before fun openDeveloperTools() {
        ui.waitUntil(10_000) { androidx.lifecycle.ViewModelProvider(ui.activity)[AppModel::class.java].state.value.vehicle.stopped == true }
        ui.onNodeWithText("개발자 도구").performClick()
    }
    @Test fun academyModeAndBlockedCandidateChangeDestination() {
        ui.onNodeWithText("서쪽 승하차점 (시연 좌표)").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("주차하고 데리러 가기").performClick()
        ui.onNodeWithText("뒤편 주차장 입구 (시연 좌표)").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("바로 태우기").performClick()
        ui.onNodeWithText("이 지점 이용 불가 · 우회").performScrollTo().performClick()
        ui.onNodeWithText("뒤편 주차장 입구 (시연 좌표)").assertIsDisplayed()
    }
    @Test fun realFrameAnalysisChangesWithVideoAndReadsPillar() {
        val analyzer = VideoSceneAnalyzer(InstrumentationRegistry.getInstrumentation().targetContext)
        val empty = analyzer.analyze(null, "generated-empty.mp4", InputSource.GENERATED)
        val blocked = analyzer.analyze(null, "generated-blocked.mp4", InputSource.GENERATED)
        val publicVideo = analyzer.analyze(null, "public-obstacles.mp4", InputSource.PUBLIC_VIDEO)
        assertEquals(Occupancy.EMPTY, empty.observation.occupancy)
        assertEquals(Occupancy.OCCUPIED, blocked.observation.occupancy)
        assertTrue(blocked.clue.replace(" ", "").contains("C12"))
        assertEquals(Occupancy.OCCUPIED, publicVideo.observation.occupancy)
    }
    @Test fun savedOptionsAndNotificationJournalSurviveModelRestart() {
        val app = ui.activity.application as Application
        var store = ViewModelStore()
        lateinit var model: AppModel
        ui.runOnIdle { model = AppModel(app); store.put("model", model); model.newSession() }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == true }
        ui.runOnIdle { model.profile(Profile(leftDoor = true, stroller = true)) }
        ui.waitUntil(10_000) { model.state.value.profileSave.status == ProfileSaveStatus.SAVED && model.state.value.profileSave.persisted?.stroller == true }
        ui.runOnIdle { model.fillDemoPosition(); model.configureNotify(consent = true); model.confirmLocation() }
        ui.waitUntil(5000) { model.state.value.session.notification.contains("모의 발송 완료") }
        val id = model.state.value.session.id
        ui.runOnIdle { store.clear(); store = ViewModelStore(); model = AppModel(app); store.put("model", model) }
        ui.waitUntil(5000) { model.state.value.profile.stroller && model.state.value.profile.leftDoor }
        assertEquals(id, model.state.value.session.id)
        assertFalse(model.state.value.session.eligible)
        assertTrue(model.state.value.session.notification.contains("모의 발송 완료"))
        ui.runOnIdle { model.profile(Profile()); model.newSession() }
        ui.waitUntil(10_000) { model.state.value.profileSave.status == ProfileSaveStatus.SAVED && model.state.value.profileSave.persisted == Profile() }
        ui.runOnIdle { store.clear() }
    }

    @Test fun hospitalCompanionAndMissingStepFreeRouteChangeCandidates() {
        val app = ui.activity.application as Application
        val store = ViewModelStore(); lateinit var model: AppModel
        ui.runOnIdle { model = AppModel(app); store.put("model", model) }
        ui.waitUntil(10_000) { model.state.value.vehicle.stopped == true && model.state.value.profileSave.status == ProfileSaveStatus.SAVED }
        ui.runOnIdle { model.newSession(); model.chooseFacility(1); model.profile(Profile(wheelchair = true)) }
        ui.waitUntil(5000) { model.state.value.recommendation.point?.id == "hospital-stop" }
        ui.runOnIdle { model.profile(Profile(wheelchair = true, companion = true)) }
        ui.waitUntil(5000) { model.state.value.recommendation.point?.id == "hospital-parking" }
        ui.runOnIdle { model.blocked() }
        assertNull(model.state.value.recommendation.point) // unknown step-free annex must not pass
        ui.runOnIdle { model.profile(Profile()); model.newSession() }
        ui.waitUntil(10_000) { model.state.value.profileSave.status == ProfileSaveStatus.SAVED }
        ui.runOnIdle { store.clear() }
    }

    @Test fun drivingAndUnknownStatesBlockLocationAndProfileChanges() {
        val store = ViewModelStore(); lateinit var model: AppModel
        ui.runOnIdle { model = AppModel(ui.activity.application); store.put("model", model) }
        ui.waitUntil(10_000) { model.state.value.vehicle.stopped == true && model.state.value.profileSave.status == ProfileSaveStatus.SAVED }
        ui.runOnIdle { model.newSession(); model.testStopped(false) }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == false }
        val original = model.state.value.profile
        ui.runOnIdle { model.profile(original.copy(stroller = !original.stroller)); model.fillDemoPosition(); model.confirmLocation(); model.analyze(null, "generated-empty.mp4", InputSource.GENERATED) }
        assertEquals(original, model.state.value.profile)
        assertEquals("", model.state.value.clue)
        assertFalse(model.state.value.session.confirmed)
        assertFalse(model.state.value.analyzing)
        ui.runOnIdle { model.testUnavailable("권한 없음") }
        ui.waitUntil(5000) { model.state.value.vehicle.stopped == null }
        assertEquals(Occupancy.UNKNOWN, model.state.value.vehicle.left)
        ui.runOnIdle { model.testStopped(true); store.clear() }
    }

    @Test fun videoInputChangesOccupancyAndDivertsRecommendationInApp() {
        val store = ViewModelStore(); lateinit var model: AppModel
        ui.runOnIdle { model = AppModel(ui.activity.application); store.put("model", model) }
        ui.waitUntil(10_000) { model.state.value.vehicle.stopped == true && model.state.value.profileSave.status == ProfileSaveStatus.SAVED }
        ui.runOnIdle { model.newSession(); model.profile(Profile()); model.mode(VisitMode.PICKUP); model.resetBlocked() }
        ui.waitUntil(5000) { model.state.value.recommendation.point?.id == "academy-stop" }
        ui.runOnIdle { model.analyze(null, "generated-empty.mp4", InputSource.GENERATED) }
        ui.waitUntil(60_000) { !model.state.value.analyzing }
        assertEquals(Occupancy.EMPTY, model.state.value.observations["academy-stop"]?.occupancy)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val selected = java.io.File(context.cacheDir, "selected-public.mp4")
        context.assets.open("public-obstacles.mp4").use { input -> selected.outputStream().use { input.copyTo(it) } }
        ui.runOnIdle { model.analyze(android.net.Uri.fromFile(selected)) }
        ui.waitUntil(60_000) { !model.state.value.analyzing }
        assertEquals(InputSource.VIDEO, model.state.value.observations["academy-stop"]?.source)
        assertEquals(Occupancy.OCCUPIED, model.state.value.observations["academy-stop"]?.occupancy)
        assertEquals("academy-parking", model.state.value.recommendation.point?.id)
        ui.runOnIdle { model.blocked() }
        assertNull(model.state.value.recommendation.point)
        selected.delete()
        ui.runOnIdle { store.clear() }
    }
}
