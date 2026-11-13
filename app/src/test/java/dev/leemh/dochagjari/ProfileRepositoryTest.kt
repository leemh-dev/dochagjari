package dev.leemh.dochagjari

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {
    @Test fun queuedEditsAreWrittenInOrderAndOnlyLatestCompletionIsSaved() = runTest {
        val release = CompletableDeferred<Unit>()
        val writes = mutableListOf<Profile>()
        val storage = object : ProfileStorage {
            override suspend fun read() = Profile()
            override suspend fun write(profile: Profile) { release.await(); writes += profile }
        }
        val repository = ProfileRepository(storage, backgroundScope)
        runCurrent()
        val first = Profile(leftDoor = true, heightM = 2.1, seats = SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.INFANT, true)))
        val last = first.copy(stroller = true, companion = true, heightM = 2.3, seats = first.seats.copy(right = SeatAssignment(SeatRole.ADULT)))
        repository.update(first); repository.update(last); runCurrent()
        assertEquals(ProfileSaveStatus.SAVING, repository.state.value.status)
        assertEquals(last, repository.state.value.profile)
        assertEquals(Profile(), repository.state.value.persisted)
        release.complete(Unit); runCurrent()
        assertEquals(listOf(first, last), writes)
        assertEquals(last, repository.state.value.persisted)
        assertEquals(ProfileSaveStatus.SAVED, repository.state.value.status)
        assertEquals(repository.state.value.revision, repository.state.value.savedRevision)
    }
    @Test fun saveFailureIsVisibleAndRetryAcknowledgesOnlyDurableValue() = runTest {
        var persisted = Profile(); var fail = true
        val storage = object : ProfileStorage {
            override suspend fun read() = persisted
            override suspend fun write(profile: Profile) { if (fail) throw IOException("disk full"); persisted = profile }
        }
        val repository = ProfileRepository(storage, backgroundScope); runCurrent()
        val edited = Profile(rightDoor = true, wheelchair = true, heightM = 2.2, seats = SeatPreferences(SeatSetup.CONFIGURED, right = SeatAssignment(SeatRole.CHILD, true)))
        repository.update(edited); runCurrent()
        assertEquals(ProfileSaveStatus.ERROR, repository.state.value.status)
        assertEquals(edited, repository.state.value.profile)
        assertEquals(Profile(), repository.state.value.persisted)
        fail = false; repository.retry(); runCurrent()
        assertEquals(ProfileSaveStatus.SAVED, repository.state.value.status)
        assertEquals(edited, repository.state.value.persisted)
        assertEquals(edited, persisted)
    }
    @Test fun loadingOldValueDoesNotOverwriteAnAlreadyQueuedEdit() = runTest {
        val readGate = CompletableDeferred<Unit>()
        val storage = object : ProfileStorage {
            override suspend fun read(): Profile { readGate.await(); return Profile(leftDoor = true) }
            override suspend fun write(profile: Profile) {}
        }
        val repository = ProfileRepository(storage, backgroundScope); runCurrent()
        val latest = Profile(rightDoor = true, heightM = 2.4)
        repository.update(latest); readGate.complete(Unit); runCurrent()
        assertEquals(latest, repository.state.value.profile)
        assertEquals(latest, repository.state.value.persisted)
        assertEquals(ProfileSaveStatus.SAVED, repository.state.value.status)
    }
}
