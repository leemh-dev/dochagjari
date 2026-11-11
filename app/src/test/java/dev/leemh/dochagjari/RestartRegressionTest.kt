package dev.leemh.dochagjari

import android.app.Application
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Reproduces the old journal and cancelled ViewModel write using the actual AppModel. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RestartRegressionTest {
    @Test fun confirmedArrivalSurvivesTerminationBeforeWorkerStarts() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val app: Application = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("session-journal", 0).edit().clear().commit()
        val firstStore = ViewModelStore()
        val secondStore = ViewModelStore()
        try {
            val first = AppModel(app); firstStore.put("model", first)
            runCurrent()
            first.fillDemoPosition(); first.configureNotify(consent = true); first.confirmLocation()
            val original = first.state.value.session
            assertNotNull(original.position)
            firstStore.clear() // queued sender has not run
            val second = AppModel(app); secondStore.put("model", second)
            assertEquals(original.id, second.state.value.session.id)
            assertEquals("confirmed coordinates must survive restart", original.position, second.state.value.session.position)
            assertEquals(original.clue, second.state.value.session.clue)
        } finally {
            firstStore.clear(); secondStore.clear(); Dispatchers.resetMain()
        }
    }

    @Test fun profileWriteDoesNotDisappearWhenViewModelIsCleared() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val app: Application = RuntimeEnvironment.getApplication()
        val store = ViewModelStore()
        try {
            val model = AppModel(app); store.put("model", model)
            runCurrent()
            model.profile(Profile(leftDoor = true, stroller = true, heightM = 2.2))
            assertTrue(model.state.value.profile.stroller)
            store.clear() // cancel before the old viewModelScope edit starts
            runCurrent()
            val file = java.io.File(app.filesDir, "datastore/profile.preferences_pb")
            assertTrue("a completed save must reach durable storage despite ViewModel disposal", file.exists())
        } finally { store.clear(); Dispatchers.resetMain() }
    }
}
