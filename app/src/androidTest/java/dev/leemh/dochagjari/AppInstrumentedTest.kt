package dev.leemh.dochagjari

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun academyModeAndBlockedCandidateChangeDestination() {
        ui.onNodeWithText("서쪽 승하차점 (시연 좌표)").assertIsDisplayed()
        ui.onNodeWithText("주차하고 데리러 가기").performClick()
        ui.onNodeWithText("뒤편 주차장 입구 (시연 좌표)").assertIsDisplayed()
        ui.onNodeWithText("바로 태우기").performClick()
        ui.onNodeWithText("이 지점 이용 불가 · 우회").performClick()
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
        ui.waitUntil(5000) { model.state.value.profile.stroller }
        ui.runOnIdle { model.fillDemoPosition(); model.configureNotify(consent = true); model.confirmLocation() }
        ui.waitUntil(5000) { model.state.value.session.notification.contains("모의 발송 완료") }
        val id = model.state.value.session.id
        ui.runOnIdle { store.clear(); store = ViewModelStore(); model = AppModel(app); store.put("model", model) }
        ui.waitUntil(5000) { model.state.value.profile.stroller && model.state.value.profile.leftDoor }
        assertEquals(id, model.state.value.session.id)
        assertFalse(model.state.value.session.eligible)
        assertTrue(model.state.value.session.notification.contains("모의 발송 완료"))
        ui.runOnIdle { model.profile(Profile()); model.newSession(); store.clear() }
    }
}
