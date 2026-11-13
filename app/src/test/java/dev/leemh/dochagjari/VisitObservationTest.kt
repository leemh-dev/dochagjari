package dev.leemh.dochagjari

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VisitObservationTest {
    private val now = 1_000_000L
    private fun base() = VisitRecord("visit-observation", "pangyo-ettoi-20261113-v1", "hyundai-pangyo-ettoi", createdAt = 1,
        phase = VisitPhase.PARKING_GUIDANCE, journey = VisitJourney(entry = EntryEvent("entry-test", GuidanceInputSource.TEST, 1)))
    private fun sample(id: String, text: String = "C12", confidence: Double? = .9, at: Long = now, occupancy: Occupancy = Occupancy.UNKNOWN) =
        VisitSample(id, text, confidence, at, 0, occupancy, if (occupancy == Occupancy.OCCUPIED) .9 else .0)
    private fun run(vararg samples: VisitSample) = VisitObservationJournal(listOf(VisitScan("run-test", VisitFrameSource.TEST, 1, samples.toList())))
    @Test fun oldVisitJsonDefaultsToNoObservationAndKeepsJourney() {
        val j = JSONObject(VisitGuidanceCodec.encode(base())); j.remove("observation")
        assertEquals(base(), VisitGuidanceCodec.decode(j.toString()))
    }
    @Test fun journalPreservesRawSourceNullTextScoreAndVideoFrameTime() {
        val r = VisitScan("generated", VisitFrameSource.GENERATED_VIDEO, 1, listOf(sample("frame", confidence = null).copy(frameTimeMs = 2000)))
        val v = base().copy(observation = VisitObservationJournal(listOf(r)))
        assertEquals(v, VisitGuidanceCodec.decode(VisitGuidanceCodec.encode(v)))
        assertNull(v.parking)
    }
    @Test fun cannotStartBeforeExplicitEntryAndSourceSwitchStopsPreviousRun() {
        assertThrows(IllegalArgumentException::class.java) { VisitObservationRules.begin(base().copy(journey = VisitJourney()), VisitFrameSource.TEST) }
        val first = VisitObservationRules.begin(base(), VisitFrameSource.TEST)
        val second = VisitObservationRules.begin(first, VisitFrameSource.GENERATED_VIDEO)
        assertNotNull(second.observation.runs.first().stopReason)
        assertNotEquals(first.observation.latest!!.id, second.observation.latest!!.id)
        assertEquals(second, VisitObservationRules.append(second, first.observation.latest!!.id, sample("late")))
    }
    @Test fun duplicatedFrameCannotCreateRepeatAndStoppedRunRejectsLateFrame() {
        val v = VisitObservationRules.begin(base(), VisitFrameSource.TEST); val id = v.observation.latest!!.id
        val one = VisitObservationRules.append(v, id, sample("frame-one"))
        val duplicate = VisitObservationRules.append(one, id, sample("frame-one"))
        assertEquals(1, duplicate.observation.latest!!.samples.size)
        assertNull(VisitObservationRules.summarize(duplicate.observation, now).candidate)
        val stopped = VisitObservationRules.stop(duplicate, id, "closed")
        assertEquals(stopped, VisitObservationRules.append(stopped, id, sample("frame-two")))
    }
    @Test fun pillarWithoutFloorNeverBecomesCurrentFloorOrElevator() {
        val result = VisitObservationRules.summarize(run(sample("one", confidence = null), sample("two", confidence = null)), now)
        assertEquals("C12", result.candidate!!.pillar); assertNull(result.candidate!!.floor)
        assertTrue(result.recommendation.contains("연결 미확인")); assertTrue(result.recommendation.contains("기존 5층 동선 유지"))
    }
    @Test fun sameNumberOnDifferentExplicitFloorsIsAConflict() {
        val r = run(sample("one", "FLOOR B3 C12"), sample("two", "FLOOR B3 C12"), sample("three", "FLOOR B4 C12"), sample("four", "FLOOR B4 C12"))
        val result = VisitObservationRules.summarize(r, now)
        assertNull(result.candidate); assertTrue(result.status.contains("상충"))
    }
    @Test fun lowStaleFutureAndMissingSignsDoNotReuseOldStableCandidate() {
        val stable = listOf(sample("a"), sample("b"))
        for (bad in listOf(sample("c", confidence = .2), sample("d", at = now - 60_000), sample("e", at = now + 1000), sample("f", text = ""))) {
            assertNull(VisitObservationRules.summarize(run(*(stable + bad).toTypedArray()), now).candidate)
        }
    }
    @Test fun bareBasementLikeTextAndMultipleNumbersRemainAmbiguous() {
        assertNull(VisitObservationRules.parseSign("B3 C12"))
        assertNull(VisitObservationRules.parseSign("C12 C13"))
        assertEquals(SignCandidate("B3", "C12"), VisitObservationRules.parseSign("FLOOR B3 C12"))
        assertEquals(SignCandidate("B4", "C12"), VisitObservationRules.parseSign("지하4층 C12"))
    }
    @Test fun repeatedObstacleUpdatesAdviceWithoutInventingARouteOrParking() {
        val result = VisitObservationRules.summarize(run(sample("a", occupancy = Occupancy.OCCUPIED), sample("b", occupancy = Occupancy.OCCUPIED)), now)
        assertTrue(result.recommendation.contains("장애물")); assertTrue(result.recommendation.contains("기존 5층 동선 유지"))
        assertNull(base().parking)
    }
    @Test fun boundedHistoryRetainsSourceAndRejectsInvalidConfidence() {
        var v = base()
        repeat(10) { v = VisitObservationRules.begin(v, VisitFrameSource.TEST) }
        assertEquals(8, v.observation.runs.size)
        val id = v.observation.latest!!.id
        repeat(30) { v = VisitObservationRules.append(v, id, sample("frame-$it")) }
        assertEquals(24, v.observation.latest!!.samples.size)
        assertThrows(IllegalArgumentException::class.java) { VisitObservationRules.append(v, id, sample("invalid", confidence = Double.NaN)) }
    }
    @Test fun sqliteReopenKeepsEnteredVisitSamplesAndHiddenState() {
        val app = RuntimeEnvironment.getApplication()
        val name = "observation-${UUID.randomUUID()}.db"
        val expected = SqliteGuidanceStorage(app, name).use { db ->
            db.installCatalog(app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() })
            val v = db.beginOrResume(base().catalogVersion, base().destinationId).copy(phase = VisitPhase.PARKING_GUIDANCE, journey = base().journey)
            var scan = VisitObservationRules.begin(v, VisitFrameSource.GENERATED_VIDEO)
            scan = VisitObservationRules.append(scan, scan.observation.latest!!.id, sample("raw-generated", confidence = null))
            scan.copy(visibility = GuideVisibility.HIDDEN).also(db::saveVisit)
        }
        SqliteGuidanceStorage(app, name).use { assertEquals(expected, it.currentVisit()); assertNull(it.currentVisit()!!.parking) }
    }
    @Test fun orderedRepositoryStopPreventsQueuedLateObservationWrites() = runTest {
        val app = RuntimeEnvironment.getApplication()
        SqliteGuidanceStorage(app, "scan-order-${UUID.randomUUID()}.db").use { db ->
            val repo = VisitGuidanceRepository(db, backgroundScope) { app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() } }; runCurrent()
            repo.prepare(base().destinationId); runCurrent(); val id = repo.state.value.visit!!.id
            val target = RoadTarget.parse(JSONObject(app.assets.open("road-target.json").bufferedReader().use { it.readText() }))
            repo.prepareRoad(id, target, RoadChannel.MOCK); repo.enter(id, "entry", GuidanceInputSource.TEST)
            val r = repo.beginObservation(id, VisitFrameSource.TEST)
            repo.appendObservation(id, r.id, sample("before")); repo.stopObservation(id, r.id, "close")
            repo.appendObservation(id, r.id, sample("after")); runCurrent()
            assertEquals(listOf("before"), db.currentVisit()!!.observation.latest!!.samples.map { it.id })
        }
    }
}
