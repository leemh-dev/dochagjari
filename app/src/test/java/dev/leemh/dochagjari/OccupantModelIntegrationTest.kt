package dev.leemh.dochagjari

import android.app.Application
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OccupantModelIntegrationTest {
    @Test fun confirmedContextChangesOnlyRecommendationAndPreservesLegacyProfileVisitAndOutbox() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val app: Application = RuntimeEnvironment.getApplication()
        val views = ViewModelStore()
        val db = SqliteGuidanceStorage(app, "occupant-${UUID.randomUUID()}.db")
        try {
            var profile = Profile(heightM = 2.2)
            val profiles = ProfileRepository(object : ProfileStorage {
                override suspend fun read() = profile
                override suspend fun write(profileValue: Profile) { profile = profileValue }
            }, backgroundScope)
            var book = OccupantBook.migrate(profile)
            val occupants = OccupantRepository(object : OccupantStorage {
                override suspend fun read() = book
                override suspend fun write(value: OccupantBook) { book = value }
            }, backgroundScope)
            val id = UUID.randomUUID().toString()
            val request = ConfirmedArrival(id, Position(37.4, 127.0), "test-only", "demo", DeliveryChannel.LOCAL_MOCK, "", InputSource.TEST, 100, 90)
            val record = ArrivalRecord(id, request, DeliveryStatus.UNKNOWN, true, true)
            val journal = FileArrivalJournal(File(app.filesDir, "occupant-outbox-${UUID.randomUUID()}.json")); journal.save(record)
            val outbox = ArrivalOutbox(journal, object : ArrivalTransport {
                override fun lookup(request: ConfirmedArrival, token: String): LookupResult = error("Unexpected network")
                override fun submit(request: ConfirmedArrival, token: String): DeliveryReceipt = error("Unexpected network")
            }, StandardTestDispatcher(testScheduler))
            val visits = VisitGuidanceRepository(db, backgroundScope) { app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() } }
            runCurrent(); visits.prepare("hyundai-pangyo-ettoi"); runCurrent()
            val before = visits.state.value.visit!!
            val model = AppModel(app, AppServices(backgroundScope, outbox, profiles, visits, occupants)); views.put("model", model); runCurrent()
            assertTrue(model.state.value.vehicle.stopped == true)
            model.applyOccupants(book.presets.single { it.mode == OccupantMode.SUPPORT }); runCurrent()
            assertEquals("parking", model.state.value.recommendation.point!!.kind)
            assertEquals(before, visits.state.value.visit)
            assertEquals(record, outbox.state.value.record); assertEquals(Profile(heightM = 2.2), profile)
            assertEquals(record.request!!.fingerprint(), outbox.state.value.record.request!!.fingerprint())
            val applied = book
            model.testStopped(false); runCurrent()
            model.applyOccupants(book.presets.first()); model.saveOccupantPreset(book.presets.first().copy(name = "blocked")); model.skipOccupants(); runCurrent()
            assertEquals(applied, book)
            model.testStopped(null); runCurrent(); model.endOccupantTrip(); runCurrent(); assertEquals(applied, book)
        } finally { views.clear(); db.close(); Dispatchers.resetMain() }
    }
}
