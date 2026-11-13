package dev.leemh.dochagjari

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class VisitObservationLifecycleTest {
    private class NoNetwork : ArrivalTransport {
        override fun lookup(request: ConfirmedArrival, token: String): LookupResult = error("Unexpected network")
        override fun submit(request: ConfirmedArrival, token: String): DeliveryReceipt = error("Unexpected network")
    }
    private class MemoryProfile : ProfileStorage { override suspend fun read() = Profile(); override suspend fun write(profile: Profile) {} }
    private suspend fun services(app: Application, scope: CoroutineScope): AppServices {
        val repo = VisitGuidanceRepository(SqliteGuidanceStorage(app, "scan-life-${UUID.randomUUID()}.db"), scope) { app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() } }
        withTimeout(10_000) { repo.state.first { !it.loading } }
        repo.prepare("hyundai-pangyo-ettoi")
        val v = withTimeout(10_000) { repo.state.first { it.visit != null && !it.saving } }.visit!!
        repo.prepareRoad(v.id, RoadTarget.parse(JSONObject(app.assets.open("road-target.json").bufferedReader().use { it.readText() })), RoadChannel.MOCK)
        repo.enter(v.id, "lifecycle-entry", GuidanceInputSource.TEST)
        return AppServices(scope, ArrivalOutbox(FileArrivalJournal(File(app.filesDir, "scan-${UUID.randomUUID()}.json")), NoNetwork()), ProfileRepository(MemoryProfile(), scope), repo)
    }
    @Test fun closeInvalidatesLateFrameAndReleasesRunningSource() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app: Application = RuntimeEnvironment.getApplication(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO); val store = ViewModelStore()
        val started = CompletableDeferred<Unit>(); val closed = CompletableDeferred<Unit>(); val release = CountDownLatch(1)
        try {
            val services = services(app, scope)
            val fake = object : VisitFrameAnalyzer {
                override fun analyzeVisit(uri: Uri?, asset: String?, source: InputSource, keepGoing: () -> Boolean, onFrame: (SceneFrame) -> Unit): SceneResult {
                    try {
                        started.complete(Unit); check(release.await(10, TimeUnit.SECONDS))
                        // Intentionally misbehaving source: emits even after cancellation.
                        onFrame(SceneFrame("C12", System.currentTimeMillis(), 0, Occupancy.UNKNOWN, .3f))
                        return SceneResult(Observation(Occupancy.UNKNOWN, .3f, 1, source), "C12", "test")
                    } finally { closed.complete(Unit) }
                }
            }
            val model = AppModel(app, services, fake); store.put("model", model)
            withTimeout(10_000) { model.state.first { it.guidance.visit?.journey?.entry != null && it.vehicle.stopped == true } }
            model.startVisitVideo(asset = "generated-blocked.mp4"); withTimeout(10_000) { started.await() }
            model.closeIndoorGuide(); assertFalse(model.state.value.observingVisit)
            release.countDown(); withTimeout(10_000) { closed.await() }
            val r = withTimeout(10_000) { services.guidance!!.state.first { it.visit?.observation?.latest?.stopReason != null && !it.saving } }.visit!!.observation.latest!!
            assertTrue(r.samples.isEmpty())
        } finally { release.countDown(); store.clear(); scope.coroutineContext[Job]!!.cancelAndJoin(); Dispatchers.resetMain() }
    }
    @Test fun permissionFailureStopsObservationAndKeepsRecordedEvidence() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app: Application = RuntimeEnvironment.getApplication(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO); val store = ViewModelStore()
        try {
            val services = services(app, scope)
            val fake = object : VisitFrameAnalyzer {
                override fun analyzeVisit(uri: Uri?, asset: String?, source: InputSource, keepGoing: () -> Boolean, onFrame: (SceneFrame) -> Unit): SceneResult {
                    onFrame(SceneFrame("C12", System.currentTimeMillis(), 0, Occupancy.UNKNOWN, .3f))
                    throw SecurityException("injected permission revocation")
                }
            }
            val model = AppModel(app, services, fake); store.put("model", model)
            withTimeout(10_000) { model.state.first { it.guidance.visit?.journey?.entry != null && it.vehicle.stopped == true } }
            model.startVisitVideo(asset = "generated-blocked.mp4")
            withTimeout(10_000) { model.state.first { !it.observingVisit && it.visitObservationMessage.contains("권한") } }
            val r = withTimeout(10_000) { services.guidance!!.state.first { it.visit?.observation?.latest?.stopReason != null && !it.saving } }.visit!!.observation.latest!!
            assertEquals(1, r.samples.size); assertNull(r.samples.single().textConfidence)
        } finally { store.clear(); scope.coroutineContext[Job]!!.cancelAndJoin(); Dispatchers.resetMain() }
    }
    @Test fun incompleteJournalRestoreNeverStartsAFrameSource() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app: Application = RuntimeEnvironment.getApplication(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO); val store = ViewModelStore(); val calls = AtomicInteger()
        try {
            val services = services(app, scope); val repo = requireNotNull(services.guidance); val v = repo.state.value.visit!!
            repo.beginObservation(v.id, VisitFrameSource.SELECTED_VIDEO)
            val fake = object : VisitFrameAnalyzer {
                override fun analyzeVisit(uri: Uri?, asset: String?, source: InputSource, keepGoing: () -> Boolean, onFrame: (SceneFrame) -> Unit): SceneResult { calls.incrementAndGet(); error("Must not auto resume") }
            }
            val model = AppModel(app, services, fake); store.put("model", model)
            withTimeout(10_000) { model.state.first { it.guidance.visit?.observation?.latest != null } }
            assertFalse(model.state.value.observingVisit); assertEquals(0, calls.get()); assertFalse(model.state.value.showIndoorGuide)
        } finally { store.clear(); scope.coroutineContext[Job]!!.cancelAndJoin(); Dispatchers.resetMain() }
    }
}
