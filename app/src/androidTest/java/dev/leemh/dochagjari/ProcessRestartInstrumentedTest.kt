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
    private fun report() = InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply { putInt("arrival_process_pid", Process.myPid()) })
    private val expected = Profile(leftDoor = true, rightDoor = true, stroller = true, wheelchair = true, companion = true, heightM = 2.2)
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
        report()
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
        assertEquals(Position(37.48544722, 127.03636666), frozen.position)
        assertEquals(frozen.position, s.session.position)
        assertEquals("demo", frozen.recipientId)
        assertEquals(DeliveryChannel.SERVER_MOCK, frozen.channel)
        assertEquals(DeliveryStatus.QUEUED, s.session.notificationStatus)
        assertTrue(s.consent)
        assertEquals("", s.token)
        assertFalse(s.delivery.record.serverObserved)
        report()
        instrumentation.runOnMainSync { assertFalse(model.newSession()); store.clear() }
    }
}
