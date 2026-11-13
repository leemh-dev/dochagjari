package dev.leemh.dochagjari

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileDataStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private fun store(file: File, scope: CoroutineScope) = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    @Test fun oldPreferencesRemainIntactAndRolesDefaultToNotSet() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = store(File(folder.root, "legacy.preferences_pb"), scope)
            data.edit { p ->
                listOf("left", "right", "stroller", "wheelchair", "companion").forEach { p[booleanPreferencesKey(it)] = true }
                p[doublePreferencesKey("height")] = 2.3
                p[stringPreferencesKey("unrelated")] = "preserve"
            }
            val storage = DataStoreProfileStorage(data)
            val old = Profile(true, true, true, true, true, 2.3)
            assertEquals(old, storage.read())
            val edited = old.copy(seats = SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.INFANT, true)))
            storage.write(edited)
            assertEquals(edited, storage.read())
            assertEquals("preserve", data.data.first()[stringPreferencesKey("unrelated")])
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun acknowledgedWriteSurvivesImmediateStoreShutdownAndReopen() = runBlocking {
        val file = File(folder.root, "restart.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val expected = Profile(stroller = true, heightM = 2.2, seats = SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.INFANT, true), SeatAssignment(SeatRole.ADULT)))
        try { DataStoreProfileStorage(store(file, scope)).write(expected) }
        finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storage = DataStoreProfileStorage(store(file, scope)); assertEquals(expected, storage.read())
            val skipped = expected.copy(seats = expected.seats.copy(setup = SeatSetup.SKIPPED))
            storage.write(skipped); assertEquals(skipped, storage.read())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun unknownRoleNamesDoNotInventAPassenger() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = store(File(folder.root, "unknown.preferences_pb"), scope)
            data.edit { it[stringPreferencesKey("seat_setup")] = "FUTURE"; it[stringPreferencesKey("seat_left_role")] = "FUTURE" }
            assertEquals(SeatPreferences(), DataStoreProfileStorage(data).read().seats)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun failedWriteLeavesActualStoredRolesUntouchedUntilRetry() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val disk = DataStoreProfileStorage(store(File(folder.root, "failure.preferences_pb"), scope))
            val old = Profile(stroller = true, seats = SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.ADULT)))
            disk.write(old)
            var fail = true
            val repository = ProfileRepository(object : ProfileStorage {
                override suspend fun read() = disk.read()
                override suspend fun write(profile: Profile) { if (fail) throw java.io.IOException("simulated write failure"); disk.write(profile) }
            }, scope)
            withTimeout(10_000) { repository.state.first { it.status == ProfileSaveStatus.SAVED } }
            val edited = old.copy(seats = old.seats.copy(left = SeatAssignment(SeatRole.INFANT, true)))
            repository.update(edited)
            withTimeout(10_000) { repository.state.first { it.status == ProfileSaveStatus.ERROR } }
            assertEquals(old, disk.read()); assertEquals(old, repository.state.value.persisted)
            assertEquals(edited, repository.state.value.profile)
            fail = false; repository.retry()
            withTimeout(10_000) { repository.state.first { it.status == ProfileSaveStatus.SAVED && it.persisted == edited } }
            assertEquals(edited, disk.read())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

}
