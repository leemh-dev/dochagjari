package dev.leemh.dochagjari

import android.app.Application
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
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class VisitParkingLifecycleTest {
    private class NoNetwork : ArrivalTransport {
        override fun lookup(request: ConfirmedArrival, token: String): LookupResult = error("Unexpected network")
        override fun submit(request: ConfirmedArrival, token: String): DeliveryReceipt = error("Unexpected network")
    }
    private class MemoryProfile : ProfileStorage { override suspend fun read() = Profile(); override suspend fun write(profile: Profile) {} }
    private suspend fun services(app: Application, scope: CoroutineScope, db: SqliteGuidanceStorage, storage: GuidanceStorage): AppServices {
        val repo = VisitGuidanceRepository(storage, scope) { app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() } }
        withTimeout(10_000) { repo.state.first { !it.loading } }; repo.prepare("hyundai-pangyo-ettoi")
        val v = withTimeout(10_000) { repo.state.first { it.visit != null && !it.saving } }.visit!!
        repo.prepareRoad(v.id, RoadTarget.parse(JSONObject(app.assets.open("road-target.json").bufferedReader().use { it.readText() })), RoadChannel.MOCK)
        repo.enter(v.id, "parking-entry", GuidanceInputSource.TEST)
        return AppServices(scope, ArrivalOutbox(FileArrivalJournal(File(app.filesDir, "park-${UUID.randomUUID()}.json")), NoNetwork()), ProfileRepository(MemoryProfile(), scope), repo)
    }
    private suspend fun ready(model: AppModel) { withTimeout(10_000) { model.state.first { it.guidance.visit?.journey?.entry != null && it.vehicle.stopped == true } } }
    @Test fun failedCompletionStopsSourceAndOnlyRetryAcknowledgmentOpensSummary() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher()); val app: Application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO); val store = ViewModelStore(); val db = SqliteGuidanceStorage(app, "park-${UUID.randomUUID()}.db"); val fail = AtomicBoolean(true)
        try {
            val storage = object : GuidanceStorage by db { override fun saveVisit(record: VisitRecord) { if (record.phase == VisitPhase.PARKED && fail.get()) throw IOException("injected write failure"); db.saveVisit(record) } }
            val svc = services(app, scope, db, storage); val model = AppModel(app, svc); store.put("model", model); ready(model)
            model.startVisitTestObservation(); withTimeout(10_000) { model.state.first { it.visitObservationMessage.contains("관측 중") } }
            model.feedVisitTestObservation("B3"); withTimeout(10_000) { model.state.first { it.guidance.visit?.observation?.latest?.samples?.size == 2 } }
            val visit = db.currentVisit()!!; val notification = svc.outbox.state.value.record
            model.confirmParking(true, "fixed-completion")
            withTimeout(10_000) { model.state.first { !it.parkingSaving && it.parkingError.isNotBlank() } }
            assertNull(db.currentVisit()!!.parked.active); assertNull(model.state.value.parkingView); assertFalse(model.state.value.observingVisit)
            fail.set(false); model.retryParkingSave()
            withTimeout(10_000) { model.state.first { !it.parkingSaving && it.parkingView == ParkingView.SUMMARY } }
            val saved = db.currentVisit()!!
            assertEquals("fixed-completion", saved.parked.active!!.id); assertEquals(1, saved.parked.records.size)
            assertNull(saved.parked.active!!.notes); assertNull(saved.parking); assertEquals(visit.id, saved.id)
            assertEquals(notification, svc.outbox.state.value.record); assertNotNull(saved.observation.latest!!.stopReason)
        } finally { store.clear(); scope.coroutineContext[Job]!!.cancelAndJoin(); db.close(); Dispatchers.resetMain() }
    }
    @Test fun lostWriteAcknowledgmentRetriesSameCompletionAndRestoreDoesNotOpenAnyView() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher()); val app: Application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO); val store = ViewModelStore(); val db = SqliteGuidanceStorage(app, "park-${UUID.randomUUID()}.db"); val fail = AtomicBoolean(true)
        try {
            val storage = object : GuidanceStorage by db { override fun saveVisit(record: VisitRecord) { db.saveVisit(record); if (record.phase == VisitPhase.PARKED && fail.getAndSet(false)) throw IOException("lost acknowledgment") } }
            val svc = services(app, scope, db, storage); val model = AppModel(app, svc); store.put("model", model); ready(model)
            model.confirmParking(true, "same-id"); withTimeout(10_000) { model.state.first { !it.parkingSaving && it.parkingError.isNotBlank() } }
            assertEquals("same-id", db.currentVisit()!!.parked.active!!.id); assertNull(model.state.value.parkingView)
            model.retryParkingSave(); withTimeout(10_000) { model.state.first { !it.parkingSaving && it.parkingView == ParkingView.SUMMARY } }
            model.dismissParkingView(); model.confirmParking(true, "another-id")
            withTimeout(10_000) { model.state.first { !it.parkingSaving } }
            assertEquals(1, db.currentVisit()!!.parked.records.size); assertNull(model.state.value.parkingView)
            store.clear(); val restored = AppModel(app, svc); store.put("restored", restored); ready(restored)
            assertNull(restored.state.value.parkingView); assertFalse(restored.state.value.showIndoorGuide); assertFalse(restored.state.value.observingVisit)
        } finally { store.clear(); scope.coroutineContext[Job]!!.cancelAndJoin(); db.close(); Dispatchers.resetMain() }
    }
    @Test fun movementDuringCompletionWritePreventsSummaryAndInvalidatesOldParking() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher()); val app: Application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO); val store = ViewModelStore(); val db = SqliteGuidanceStorage(app, "park-${UUID.randomUUID()}.db")
        val started = CompletableDeferred<Unit>(); val release = CountDownLatch(1); val once = AtomicBoolean(true)
        try {
            val storage = object : GuidanceStorage by db { override fun saveVisit(record: VisitRecord) {
                if (record.phase == VisitPhase.PARKED && once.getAndSet(false)) { started.complete(Unit); check(release.await(10, TimeUnit.SECONDS)) }; db.saveVisit(record)
            } }
            val svc = services(app, scope, db, storage); val model = AppModel(app, svc); store.put("model", model); ready(model)
            model.confirmParking(true); withTimeout(10_000) { started.await() }; model.testStopped(false)
            withTimeout(10_000) { model.state.first { it.vehicle.stopped == false } }; release.countDown()
            withTimeout(10_000) { model.state.first { !it.parkingSaving && it.guidance.visit?.parked?.latest?.resumed != null } }
            assertEquals(VisitPhase.PARKING_GUIDANCE, db.currentVisit()!!.phase); assertNull(db.currentVisit()!!.parked.active)
            assertFalse(db.currentVisit()!!.parked.latest!!.resumed!!.explicit); assertNull(model.state.value.parkingView); assertFalse(model.state.value.observingVisit)
            model.testStopped(true); withTimeout(10_000) { model.state.first { it.vehicle.stopped == true } }
            assertFalse(model.state.value.observingVisit); assertFalse(model.state.value.showIndoorGuide)
        } finally { release.countDown(); store.clear(); scope.coroutineContext[Job]!!.cancelAndJoin(); db.close(); Dispatchers.resetMain() }
    }
    @Test fun lostAcknowledgmentThenMovementRetriesHistoricalIdWithoutCurrentParkingOrPopup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher()); val app: Application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO); val store = ViewModelStore(); val db = SqliteGuidanceStorage(app, "park-${UUID.randomUUID()}.db"); val fail = AtomicBoolean(true)
        try {
            val storage = object : GuidanceStorage by db { override fun saveVisit(record: VisitRecord) { db.saveVisit(record); if (record.phase == VisitPhase.PARKED && fail.getAndSet(false)) throw IOException("lost acknowledgment") } }
            val svc = services(app, scope, db, storage); val model = AppModel(app, svc); store.put("model", model); ready(model)
            model.confirmParking(true, "historical-id"); withTimeout(10_000) { model.state.first { !it.parkingSaving && it.parkingError.isNotBlank() } }
            model.testStopped(false); withTimeout(10_000) { model.state.first { it.vehicle.stopped == false } }
            model.testStopped(true); withTimeout(10_000) { model.state.first { it.vehicle.stopped == true } }
            model.retryParkingSave(); withTimeout(10_000) { model.state.first { !it.parkingSaving && it.guidance.visit?.parked?.latest?.resumed != null } }
            assertEquals("historical-id", db.currentVisit()!!.parked.latest!!.id); assertEquals(1, db.currentVisit()!!.parked.records.size)
            assertNull(db.currentVisit()!!.parked.active); assertNull(model.state.value.parkingView); assertFalse(model.state.value.observingVisit)
        } finally { store.clear(); scope.coroutineContext[Job]!!.cancelAndJoin(); db.close(); Dispatchers.resetMain() }
    }

    @Test fun editingWhileNotesSaveIsPendingKeepsNewDraftUnsavedUntilItsOwnAcknowledgment() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher()); val app: Application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO); val store = ViewModelStore(); val db = SqliteGuidanceStorage(app, "park-${UUID.randomUUID()}.db")
        val started = CompletableDeferred<Unit>(); val release = CountDownLatch(1); val once = AtomicBoolean(true)
        try {
            val storage = object : GuidanceStorage by db { override fun saveVisit(record: VisitRecord) {
                if (record.parked.active?.notes != null && once.getAndSet(false)) { started.complete(Unit); check(release.await(10, TimeUnit.SECONDS)) }; db.saveVisit(record)
            } }
            val svc = services(app, scope, db, storage); val model = AppModel(app, svc); store.put("model", model); ready(model)
            model.confirmParking(true); withTimeout(10_000) { model.state.first { !it.parkingSaving && it.parkingView != null } }; model.dismissParkingView()
            model.editParkingDetails(ParkingDetails(memo = "first synthetic note")); model.saveParkingDetails()
            withTimeout(10_000) { started.await() }; model.editParkingDetails(ParkingDetails(memo = "second synthetic note")); release.countDown()
            withTimeout(10_000) { model.state.first { !it.parkingSaving } }
            assertEquals("first synthetic note", db.currentVisit()!!.parked.active!!.notes!!.details.memo)
            assertTrue(model.state.value.parkingDraftDirty); assertEquals("second synthetic note", model.state.value.parkingDraft.memo)
            model.saveParkingDetails(); withTimeout(10_000) { model.state.first { !it.parkingSaving && !it.parkingDraftDirty } }
            assertEquals("second synthetic note", db.currentVisit()!!.parked.active!!.notes!!.details.memo); assertEquals(2, db.currentVisit()!!.parked.active!!.revisions.size)
        } finally { release.countDown(); store.clear(); scope.coroutineContext[Job]!!.cancelAndJoin(); db.close(); Dispatchers.resetMain() }
    }

}
