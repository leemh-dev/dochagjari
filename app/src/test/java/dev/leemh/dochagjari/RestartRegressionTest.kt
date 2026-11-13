package dev.leemh.dochagjari

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RestartRegressionTest {
    private class NoNetwork : ArrivalTransport {
        override fun lookup(request: ConfirmedArrival, token: String): LookupResult = error("Unexpected network request")
        override fun submit(request: ConfirmedArrival, token: String): DeliveryReceipt = error("Unexpected network request")
    }
    private class MemoryProfile : ProfileStorage {
        var saved = Profile()
        override suspend fun read() = saved
        override suspend fun write(profile: Profile) { saved = profile }
    }
    private fun root(): File = File(RuntimeEnvironment.getApplication().filesDir, UUID.randomUUID().toString()).also { it.mkdirs() }

    @Test fun confirmedArrivalSurvivesTerminationBeforeWorkerStarts() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val app: Application = RuntimeEnvironment.getApplication()
        val directory = root(); val file = File(directory, "outbox.json")
        val dispatcher = StandardTestDispatcher(testScheduler)
        val firstScope = CoroutineScope(SupervisorJob() + dispatcher)
        val secondScope = CoroutineScope(SupervisorJob() + dispatcher)
        val firstStore = ViewModelStore(); val secondStore = ViewModelStore()
        try {
            val firstOutbox = ArrivalOutbox(FileArrivalJournal(file), NoNetwork(), dispatcher)
            val first = AppModel(app, AppServices(firstScope, firstOutbox, ProfileRepository(MemoryProfile(), firstScope)))
            firstStore.put("model", first)
            first.fillDemoPosition(); first.configureNotify(consent = true); first.confirmLocation()
            val original = first.state.value.session
            assertNotNull(original.position)
            firstStore.clear(); firstScope.cancel() // Simulated process termination before worker runs.
            val restoredOutbox = ArrivalOutbox(FileArrivalJournal(file), NoNetwork(), dispatcher)
            val second = AppModel(app, AppServices(secondScope, restoredOutbox, ProfileRepository(MemoryProfile(), secondScope)))
            secondStore.put("model", second)
            assertEquals(original.id, second.state.value.session.id)
            assertEquals(original.position, second.state.value.session.position)
            assertEquals(original.clue, second.state.value.session.clue)
            assertEquals(DeliveryStatus.QUEUED, second.state.value.session.notificationStatus)
            restoredOutbox.recover("")
            assertEquals(DeliveryStatus.MOCK, restoredOutbox.state.value.record.status)
        } finally {
            firstStore.clear(); secondStore.clear(); firstScope.cancel(); secondScope.cancel(); runCurrent(); Dispatchers.resetMain()
        }
    }

    @Test fun profileWriteDoesNotDisappearWhenViewModelIsCleared() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app: Application = RuntimeEnvironment.getApplication()
        val directory = root(); val file = File(directory, "profile.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var secondScope: CoroutineScope? = null
        val firstStore = ViewModelStore(); val secondStore = ViewModelStore()
        try {
            val disk = DataStoreProfileStorage(PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file }))
            val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
            val gated = object : ProfileStorage {
                override suspend fun read() = disk.read()
                override suspend fun write(profile: Profile) { started.complete(Unit); release.await(); disk.write(profile) }
            }
            val profiles = ProfileRepository(gated, firstScope)
            val services = AppServices(firstScope, ArrivalOutbox(FileArrivalJournal(File(directory, "outbox.json")), NoNetwork()), profiles)
            val first = AppModel(app, services); firstStore.put("model", first)
            withTimeout(10_000) { profiles.state.first { it.status == ProfileSaveStatus.SAVED } }
            val expected = Profile(leftDoor = true, rightDoor = true, stroller = true, wheelchair = true, companion = true, heightM = 2.2)
            first.profile(expected)
            withTimeout(10_000) { started.await() }
            assertEquals(ProfileSaveStatus.SAVING, profiles.state.value.status)
            assertNotEquals(expected, profiles.state.value.persisted)
            firstStore.clear(); release.complete(Unit)
            withTimeout(10_000) { profiles.state.first { it.status == ProfileSaveStatus.SAVED && it.persisted == expected } }
            assertTrue(file.exists())
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val reopened = DataStoreProfileStorage(PreferenceDataStoreFactory.create(scope = secondScope!!, produceFile = { file }))
            val nextProfiles = ProfileRepository(reopened, secondScope!!)
            val second = AppModel(app, AppServices(secondScope!!, ArrivalOutbox(FileArrivalJournal(File(directory, "outbox.json")), NoNetwork()), nextProfiles))
            secondStore.put("model", second)
            val saved = withTimeout(10_000) { second.state.first { it.profileSave.status == ProfileSaveStatus.SAVED } }
            assertEquals(expected, saved.profile)
            assertEquals(expected, reopened.read())
        } finally {
            firstStore.clear(); secondStore.clear(); firstScope.cancel(); secondScope?.cancel(); Dispatchers.resetMain()
        }
    }

    @Test fun frozenDestinationAndConsentCannotBeReboundByConfigurationChanges() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val app: Application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val journal = FileArrivalJournal(File(root(), "outbox.json"))
        val outbox = ArrivalOutbox(journal, NoNetwork(), StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val model = AppModel(app, AppServices(scope, outbox, ProfileRepository(MemoryProfile(), scope))); store.put("model", model)
            model.configureNotify(useServer = true, url = "https://example.invalid", recipient = "original", sensServer = true)
            model.configureNotify(consent = true); model.fillDemoPosition(); model.confirmLocation()
            val frozen = outbox.state.value.record.request!!
            model.configureNotify(useServer = false, recipient = "other", url = "https://changed.invalid", token = "not-stored")
            model.editLocation(clue = "changed", latitude = "1", longitude = "2")
            assertEquals(frozen, outbox.state.value.record.request)
            assertEquals(frozen.position, model.state.value.session.position)
            assertEquals("original", model.state.value.recipientId)
            assertTrue(model.state.value.useServer)
            assertFalse(model.newSession())
            model.chooseFacility(1); assertEquals(0, model.state.value.facilityIndex)
            model.configureNotify(consent = false)
            assertFalse(journal.load()!!.consentActive)
            assertFalse(ArrivalRecordCodec.encode(journal.load()!!).contains("not-stored"))
        } finally { store.clear(); scope.cancel(); runCurrent(); Dispatchers.resetMain() }
    }
}
