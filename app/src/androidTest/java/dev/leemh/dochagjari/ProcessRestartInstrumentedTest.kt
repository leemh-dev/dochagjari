package dev.leemh.dochagjari

import android.app.Application
import android.os.Bundle
import android.os.Process
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Invoked as two separate instrumentation processes by scripts/process_restart_smoke.py. */
class ProcessRestartInstrumentedTest {
    private fun phase(expected: String) = assumeTrue(InstrumentationRegistry.getArguments().getString("restartPhase") == expected)
    private fun report(request: ConfirmedArrival) = InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
        putInt("arrival_process_pid", Process.myPid()); putString("arrival_request_hash", request.fingerprint())
        val visit = AppServices.get(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application).guidance!!.state.value.visit!!
        putString("visit_id", visit.id); putString("road_request_id", visit.journey.latest!!.id)
    })
    private val expected = Profile(leftDoor = true, rightDoor = true, stroller = true, wheelchair = true, companion = true, heightM = 2.2, seats = SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.INFANT, true), SeatAssignment(SeatRole.ADULT)))
    @Test fun prepareQueuedArrivalAndSavedProfile() = runBlocking {
        phase("prepare")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        val store = ViewModelStore(); lateinit var model: AppModel
        instrumentation.runOnMainSync { model = AppModel(app); store.put("model", model) }
        withTimeout(10_000) { model.state.first { it.vehicle.stopped == true && it.profileSave.status == ProfileSaveStatus.SAVED } }
        instrumentation.runOnMainSync {
            assertTrue(model.newSession())
            model.profile(expected)
            model.configureNotify(useServer = true, url = "http://10.0.2.2:8000", recipient = "demo")
            model.fillDemoPosition(); model.configureNotify(consent = true); model.confirmLocation()
        }
        withTimeout(10_000) { model.state.first { it.profileSave.status == ProfileSaveStatus.SAVED && it.profileSave.persisted == expected && it.delivery.issue == DeliveryIssue.TOKEN_REQUIRED } }
        assertEquals(DeliveryStatus.QUEUED, model.state.value.session.notificationStatus)
        assertNotNull(model.state.value.session.position)
        assertEquals(DeliveryChannel.SERVER_MOCK, model.state.value.delivery.record.request!!.channel)
        val guidance = requireNotNull(AppServices.get(app).guidance)
        withTimeout(10_000) { guidance.state.first { !it.loading } }
        guidance.prepare("hyundai-pangyo-ettoi")
        val visit = withTimeout(10_000) { guidance.state.first { it.visit != null && !it.saving } }.visit!!
        val request = guidance.prepareRoad(visit.id, requireNotNull(model.state.value.roadTarget), RoadChannel.MOCK)
        assertNotNull(guidance.claimRoad(visit.id, request.id)) // Simulate termination after claim, before external handoff.
        report(model.state.value.delivery.record.request!!)
        instrumentation.runOnMainSync { store.clear() }
    }
    @Test fun verifyRestoredArrivalAndProfile() = runBlocking {
        phase("verify")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        val store = ViewModelStore(); lateinit var model: AppModel
        instrumentation.runOnMainSync { model = AppModel(app); store.put("model", model) }
        withTimeout(10_000) { model.state.first { it.profileSave.status == ProfileSaveStatus.SAVED && it.delivery.issue == DeliveryIssue.TOKEN_REQUIRED } }
        val s = model.state.value
        assertEquals(expected, s.profile)
        val frozen = s.delivery.record.request!!
        val facility = FacilityParser.parse(app.assets.open("facilities.json").bufferedReader().use { it.readText() }).first()
        // The prepared profile requires companion parking, so the expected destination is the parking entrance.
        assertEquals(facility.points.single { it.id == "academy-parking" }.position, frozen.position)
        assertEquals(frozen.position, s.session.position)
        assertEquals("demo", frozen.recipientId)
        assertEquals(DeliveryChannel.SERVER_MOCK, frozen.channel)
        assertEquals(DeliveryStatus.QUEUED, s.session.notificationStatus)
        assertTrue(s.consent)
        assertEquals("", s.token)
        assertFalse(s.delivery.record.serverObserved)
        val guidance = requireNotNull(AppServices.get(app).guidance)
        val visit = withTimeout(10_000) { guidance.state.first { !it.loading && it.visit != null } }.visit!!
        assertEquals(VisitPhase.ROAD_GUIDANCE, visit.phase)
        assertEquals(RoadStatus.DISPATCHED, visit.journey.latest!!.status)
        assertTrue(visit.journey.latest!!.events.isEmpty())
        assertNull(visit.journey.entry); assertFalse(s.showIndoorGuide)
        report(frozen)
        instrumentation.runOnMainSync { assertFalse(model.newSession()); store.clear() }
    }
}
