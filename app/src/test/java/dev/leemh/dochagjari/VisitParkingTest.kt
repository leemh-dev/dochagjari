package dev.leemh.dochagjari

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VisitParkingTest {
    private val app get() = RuntimeEnvironment.getApplication() as android.app.Application
    private fun seed() = app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() }
    private fun base() = VisitRecord("parking-visit", "pangyo-ettoi-20261113-v1", "hyundai-pangyo-ettoi", createdAt = 1,
        phase = VisitPhase.PARKING_GUIDANCE, journey = VisitJourney(entry = EntryEvent("test-entry", GuidanceInputSource.TEST, 1)))
    private fun complete(v: VisitRecord = base(), id: String = "parking-one") = VisitParkingRules.complete(v, id, GuidanceInputSource.TEST, InputSource.TEST, 100).visit
    private fun note(id: String = "note-one", text: String = "테스트 메모") = ParkingRevision(id, GuidanceInputSource.TEST, 120, ParkingDetails("B3", pillar = "C12", memo = text))
    @Test fun oldJsonAndLegacyParkingSurviveWithoutNewCompletion() {
        val v = base().copy(parking = ParkingClue(pillar = "legacy", source = GuidanceInputSource.RECORDED_VIDEO, observedAt = 1))
        val j = JSONObject(VisitGuidanceCodec.encode(v)); j.remove("parked")
        assertEquals(v, VisitGuidanceCodec.decode(j.toString())); assertNull(v.parked.active); assertEquals(v.parking, complete(v).parking)
    }
    @Test fun completeStopsJournalButNeverPromotesGeneratedText() {
        val observed = base().copy(observation = VisitObservationJournal(listOf(VisitScan("generated", VisitFrameSource.GENERATED_VIDEO, 1,
            listOf(VisitSample("frame", "FLOOR B3 C12", null, 99, 0))))))
        val parked = complete(observed)
        assertEquals(VisitPhase.PARKED, parked.phase); assertEquals(GuideVisibility.HIDDEN, parked.visibility)
        assertNull(parked.parking); assertNull(parked.parked.active!!.notes)
        assertNotNull(parked.observation.latest!!.stopReason); assertEquals(observed.journey, parked.journey)
        assertEquals(observed.id, parked.id); assertEquals(observed.catalogVersion, parked.catalogVersion)
    }
    @Test fun entryAndPhaseAndExplicitSourceAreRequired() {
        assertThrows(IllegalArgumentException::class.java) { complete(base().copy(journey = VisitJourney())) }
        assertThrows(IllegalArgumentException::class.java) { complete(base().copy(phase = VisitPhase.ROAD_GUIDANCE)) }
        assertThrows(IllegalArgumentException::class.java) { VisitParkingRules.complete(base(), "id", GuidanceInputSource.RECORDED_VIDEO, InputSource.TEST, 100) }
    }
    @Test fun repeatedCompletionWithSameOrNewIdsDoesNotAddOrReopen() {
        val p = complete(); assertFalse(VisitParkingRules.complete(p, "another", GuidanceInputSource.USER, InputSource.TEST, 200).created)
        assertEquals(p, complete(p)); assertEquals(p, complete(p, "another")); assertEquals(1, p.parked.records.size)
    }
    @Test fun oldCompletionCannotReplayAfterMovementButNewExplicitCompletionCan() {
        val moving = VisitParkingRules.resume(complete(), "parking-one", InputSource.TEST, false, 200)
        assertEquals(moving, complete(moving)); assertNull(moving.parked.active)
        val again = complete(moving, "parking-two"); assertEquals(2, again.parked.records.size); assertNull(again.parked.active!!.notes)
    }
    @Test fun userConfirmationSourceTimeAndUnknownFieldsRoundTrip() {
        val p = complete(); val revised = VisitParkingRules.amend(p, p.parked.active!!.id, ParkingRevision("user", GuidanceInputSource.USER, 200, ParkingDetails(memo = "사용자 확인 메모")))
        val restored = VisitGuidanceCodec.decode(VisitGuidanceCodec.encode(revised)); assertEquals(revised, restored)
        assertNull(restored.parked.active!!.notes!!.details.floor); assertEquals(200L, restored.parked.active!!.notes!!.confirmedAt)
        assertEquals(GuidanceInputSource.USER, restored.parked.active!!.notes!!.source); assertNull(restored.parking)
    }
    @Test fun explicitAndSignalResumeRetainNotesAndHideGuideWithoutSourceRestart() {
        val p = VisitParkingRules.amend(complete(), "parking-one", note())
        for (explicit in listOf(true, false)) {
            val resumed = VisitParkingRules.resume(p, "parking-one", InputSource.TEST, explicit, 200)
            assertEquals(VisitPhase.PARKING_GUIDANCE, resumed.phase); assertEquals(GuideVisibility.HIDDEN, resumed.visibility)
            assertNull(resumed.parked.active); assertEquals(note(), resumed.parked.latest!!.notes)
            assertEquals(explicit, resumed.parked.latest!!.resumed!!.explicit)
            assertEquals(resumed, VisitParkingRules.resume(resumed, "parking-one", InputSource.TEST, explicit, 300))
        }
    }
    @Test fun lateFramesAndOldResumeCannotChangeNewCycle() {
        val scanning = VisitObservationRules.begin(base(), VisitFrameSource.TEST); val p = complete(scanning)
        assertEquals(p, VisitObservationRules.append(p, scanning.observation.latest!!.id, VisitSample("late", "C12", .9, 200, 0)))
        val next = complete(VisitParkingRules.resume(p, "parking-one", InputSource.TEST, true, 200), "parking-two")
        assertEquals(next, VisitParkingRules.resume(next, "parking-one", InputSource.TEST, false, 300))
    }
    @Test fun revisionsAreIdempotentAndConflictingIdsOrOversizedFieldsAreRejected() {
        val p = VisitParkingRules.amend(complete(), "parking-one", note())
        assertEquals(p, VisitParkingRules.amend(p, "parking-one", note()))
        assertThrows(IllegalArgumentException::class.java) { VisitParkingRules.amend(p, "parking-one", note(text = "changed")) }
        assertThrows(IllegalArgumentException::class.java) { VisitParkingRules.amend(p, "parking-one", note("too-long").copy(details = ParkingDetails(memo = "x".repeat(281)))) }
    }
    @Test fun sqliteReopenRetainsVisitSourceAndPreviousRecords() {
        val name = "parking-${UUID.randomUUID()}.db"
        val saved = SqliteGuidanceStorage(app, name).use { db ->
            val catalog = db.installCatalog(seed()); val visit = db.beginOrResume(catalog.version, base().destinationId)
            val v = VisitParkingRules.amend(complete(visit.copy(phase = VisitPhase.PARKING_GUIDANCE, journey = base().journey)), "parking-one", note())
            db.saveVisit(v); v
        }
        SqliteGuidanceStorage(app, name).use { db -> assertEquals(saved, db.currentVisit()); assertEquals(saved, db.beginOrResume(saved.catalogVersion, saved.destinationId)) }
    }
    @Test fun concurrentCompletionsAndQueuedNoteChangesPreserveOneCycleAndLastRevision() = runTest {
        SqliteGuidanceStorage(app, "parking-${UUID.randomUUID()}.db").use { db ->
            val repo = VisitGuidanceRepository(db, backgroundScope, ::seed); runCurrent(); repo.prepare(base().destinationId); runCurrent()
            val original = repo.state.value.visit!!; db.saveVisit(original.copy(phase = VisitPhase.PARKING_GUIDANCE, journey = base().journey))
            val commits = (1..8).map { i -> async { repo.completeParking(original.id, "complete-$i", GuidanceInputSource.TEST, InputSource.TEST, 100) } }.awaitAll()
            assertEquals(1, commits.count { it.created }); val id = db.currentVisit()!!.parked.active!!.id
            val changes = (1..8).map { i -> async { repo.amendParking(original.id, id, note("revision-$i", "$i")) } }; changes.awaitAll()
            assertEquals(8, db.currentVisit()!!.parked.active!!.revisions.size); assertEquals("8", db.currentVisit()!!.parked.active!!.notes!!.details.memo)
            assertEquals(original.id, db.currentVisit()!!.id)
        }
    }
    @Test fun writeFailureAndReadBackFailureAreNotAcknowledgedAsSaved() = runTest {
        SqliteGuidanceStorage(app, "parking-${UUID.randomUUID()}.db").use { db ->
            var failWrite = false; var staleRead = false; var old: VisitRecord? = null
            val storage = object : GuidanceStorage by db {
                override fun currentVisit() = if (staleRead) old else db.currentVisit()
                override fun saveVisit(record: VisitRecord) { if (failWrite) throw IOException("injected failure"); db.saveVisit(record) }
            }
            val repo = VisitGuidanceRepository(storage, backgroundScope, ::seed); runCurrent(); repo.prepare(base().destinationId); runCurrent()
            val v = repo.state.value.visit!!.copy(phase = VisitPhase.PARKING_GUIDANCE, journey = base().journey); db.saveVisit(v); old = v
            failWrite = true
            try { repo.completeParking(v.id, "frozen-id", GuidanceInputSource.TEST, InputSource.TEST, 100); fail("must fail") } catch (_: IOException) {}
            assertNull(db.currentVisit()!!.parked.active)
            failWrite = false; staleRead = true
            try { repo.completeParking(v.id, "frozen-id", GuidanceInputSource.TEST, InputSource.TEST, 100); fail("readback must fail") } catch (_: IllegalStateException) {}
            assertNull(repo.state.value.visit!!.parked.active); assertNotNull(db.currentVisit()!!.parked.active)
            staleRead = false; val recovered = repo.completeParking(v.id, "frozen-id", GuidanceInputSource.TEST, InputSource.TEST, 100)
            assertFalse(recovered.created); assertEquals(1, recovered.visit.parked.records.size)
        }
    }
}
