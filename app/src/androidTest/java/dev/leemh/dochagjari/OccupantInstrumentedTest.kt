package dev.leemh.dochagjari

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test

class OccupantInstrumentedTest {
    val ui = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(ui).around(FailureScreenshot())
    @Test fun independentPresetsOnceEditCommandAndRecreationUseActualSavedData() {
        val services = AppServices.get(ui.activity.application)
        val repo = requireNotNull(services.occupants)
        lateinit var model: AppModel
        ui.runOnIdle { model = ViewModelProvider(ui.activity)[AppModel::class.java] }
        ui.waitUntil(10_000) { repo.state.value.saved && model.state.value.vehicle.stopped == true }
        val before = runBlocking { repo.readStored() }.presets
        val visit = services.guidance?.state?.value?.visit
        val outbox = services.outbox.state.value.record
        ui.onNodeWithText("탑승 설정").performClick()
        ui.onNodeWithTag("occupant-settings").assertIsDisplayed()
        ui.onNodeWithText("아이 등하원 적용").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.saved && repo.state.value.active?.presetId == "child" }
        val child = runBlocking { repo.readStored() }.active!!
        ui.onNodeWithText("이번만 변경").performScrollTo().performClick()
        ui.onNodeWithTag("occupant-preset-name").performTextReplacement("이번만 검증")
        ui.onNodeWithText("왼쪽 성인").performScrollTo().performClick()
        ui.onNodeWithText("이번 탑승만 적용").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.saved && repo.state.value.active?.name == "이번만 검증" }
        val once = runBlocking { repo.readStored() }
        assertEquals(child.id, once.active!!.id); assertEquals(before, once.presets)
        assertEquals(SeatRole.ADULT, once.active!!.seats.left.role)
        ui.onNodeWithTag("occupant-command").performScrollTo().performTextInput("오늘 혼자야")
        ui.onNodeWithText("구성 확인").performScrollTo().performClick()
        ui.onNodeWithText("혼자 확인 적용").performScrollTo().performClick()
        ui.waitUntil(10_000) { repo.state.value.saved && repo.state.value.active?.mode == OccupantMode.SOLO }
        assertEquals(OccupantInput.TEXT, runBlocking { repo.readStored() }.active!!.source)
        ui.onNodeWithText("닫기").performScrollTo().performClick()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("product-home").assertIsDisplayed()
        assertEquals(visit, services.guidance?.state?.value?.visit)
        assertEquals(outbox, services.outbox.state.value.record)
        ui.runOnIdle { model.testStopped(false) }
        ui.onNodeWithText("탑승 설정").assertIsNotEnabled()
        ui.runOnIdle { model.testStopped(true); model.endOccupantTrip() }
        ui.waitUntil(10_000) { repo.state.value.saved && repo.state.value.book.active == null }
    }
}
