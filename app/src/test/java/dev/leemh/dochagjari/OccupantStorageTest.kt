package dev.leemh.dochagjari

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class OccupantStorageTest {
    @get:Rule val folder = TemporaryFolder()
    private fun store(file: File, scope: CoroutineScope) = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    @Test fun legacyFileMigrationAndUpdatesPreserveAllExistingAndUnrelatedKeys() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val ds = store(File(folder.root, "legacy.preferences_pb"), scope)
            val profiles = DataStoreProfileStorage(ds)
            val old = Profile(true, true, true, true, true, 2.4, SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.INFANT, true)))
            profiles.write(old); ds.edit { it[stringPreferencesKey("unrelated")] = "keep" }
            val storage = DataStoreOccupantStorage(ds); val first = storage.read()
            assertEquals(old.seats, first.presets.first().seats); assertNull(first.active)
            storage.write(first.select(first.presets[1], OccupantInput.USER, 100))
            assertEquals(old, profiles.read()); assertEquals("keep", ds.data.first()[stringPreferencesKey("unrelated")])
            profiles.write(old.copy(heightM = 2.5))
            assertEquals("solo", storage.read().active!!.presetId)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun immediateShutdownReopensActualFileButDoesNotClaimFreshConfirmation() = runBlocking {
        val file = File(folder.root, "restart.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        lateinit var expected: OccupantBook
        try {
            val storage = DataStoreOccupantStorage(store(file, scope)); val b = storage.read()
            expected = b.select(b.presets[1], OccupantInput.PRESET, 100); storage.write(expected)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storage = DataStoreOccupantStorage(store(file, scope)); val repo = OccupantRepository(storage, scope)
            withTimeout(10_000) { repo.state.first { it.saved } }
            assertEquals(expected, repo.readStored()); assertEquals(expected.active, repo.state.value.book.active)
            assertNull(repo.state.value.active); assertFalse(repo.state.value.confirmedThisRun)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun futureSchemaIsNotOverwrittenOnRead() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val ds = store(File(folder.root, "future.preferences_pb"), scope)
            val raw = "{\"schema\":99,\"future\":true}"
            ds.edit { it[stringPreferencesKey("occupant_book_v1")] = raw }
            assertTrue(runCatching { DataStoreOccupantStorage(ds).read() }.isFailure)
            assertEquals(raw, ds.data.first()[stringPreferencesKey("occupant_book_v1")])
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun failedActivationKeepsOldDiskAndRetryConfirmsOnlyAfterReadback() = runTest {
        var disk = OccupantBook.migrate(Profile()); var fail = true
        val storage = object : OccupantStorage {
            override suspend fun read() = disk
            override suspend fun write(book: OccupantBook) { if (fail) throw IOException("injected"); disk = book }
        }
        val repo = OccupantRepository(storage, backgroundScope); runCurrent()
        val before = disk
        repo.activate(disk.presets[1], now = 100); runCurrent()
        assertEquals(ProfileSaveStatus.ERROR, repo.state.value.status); assertEquals(before, disk)
        assertEquals(before, repo.state.value.persisted); assertNull(repo.state.value.active)
        fail = false; repo.retry(); runCurrent()
        assertTrue(repo.state.value.saved); assertEquals(disk.active, repo.state.value.active)
    }
    @Test fun queuedChangesAcknowledgeLatestRevisionAndKeepPresetSnapshotSeparate() = runTest {
        val gate = CompletableDeferred<Unit>(); var disk = OccupantBook.migrate(Profile())
        val written = mutableListOf<OccupantBook>()
        val repo = OccupantRepository(object : OccupantStorage {
            override suspend fun read() = disk
            override suspend fun write(book: OccupantBook) { gate.await(); disk = book; written += book }
        }, backgroundScope); runCurrent()
        repo.activate(disk.presets[1], now = 100); runCurrent()
        repo.activate(disk.presets[2], now = 101); runCurrent()
        assertEquals(ProfileSaveStatus.SAVING, repo.state.value.status); assertNull(repo.state.value.active)
        gate.complete(Unit); runCurrent()
        assertEquals(2, written.size); assertTrue(repo.state.value.saved)
        assertEquals("family", repo.state.value.active!!.presetId)
        assertEquals(written[0].active!!.id, written[1].active!!.id)
        assertEquals(disk, repo.readStored())
    }
    @Test fun skipAndEndDoNotDeletePresetsAndEachNewTripGetsAnIndependentId() = runTest {
        var disk = OccupantBook.migrate(Profile())
        val repo = OccupantRepository(object : OccupantStorage {
            override suspend fun read() = disk
            override suspend fun write(book: OccupantBook) { disk = book }
        }, backgroundScope); runCurrent()
        repo.activate(disk.presets[1], now = 100); runCurrent(); val first = disk.active!!.id
        repo.skip(); runCurrent(); assertTrue(disk.skipped); assertEquals(4, disk.presets.size)
        repo.activate(disk.presets[0], now = 101); runCurrent()
        assertNotEquals(first, disk.active!!.id); assertFalse(disk.skipped)
        repo.endTrip(); runCurrent(); assertNull(disk.active); assertEquals(4, disk.presets.size)
    }
}
