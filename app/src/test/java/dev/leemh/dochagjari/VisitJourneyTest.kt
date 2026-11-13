package dev.leemh.dochagjari

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.util.UUID
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VisitJourneyTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private fun seed() = app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() }
    private fun target() = RoadTarget.parse(JSONObject(app.assets.open("road-target.json").bufferedReader().use { it.readText() }))
    private fun planned() = VisitRecord("visit-test", "pangyo-ettoi-20261113-v1", "hyundai-pangyo-ettoi", createdAt = 100)
    @Test fun officialFacilityMarkerIsNotAVehicleEntrance() {
        val t = target(); assertEquals(RoadTargetKind.FACILITY_POI, t.kind)
        assertEquals(37.3927854, t.latitude, 0.0); assertEquals(127.1120699, t.longitude, 0.0)
        assertEquals(t, RoadTarget.parse(t.json()))
    }
    @Test fun oldVisitJournalDefaultsToNoNavigationOrEntry() {
        val legacy = JSONObject(VisitGuidanceCodec.encode(planned())).apply { remove("journey") }
        assertEquals(planned(), VisitGuidanceCodec.decode(legacy.toString()))
    }
    @Test fun mockCalculationCannotBecomeEntryOrParking() {
        val p = VisitJourneyRules.prepare(planned(), target(), RoadChannel.MOCK)
        val claimed = VisitJourneyRules.claim(p, p.journey.latest!!.id)!!
        val complete = VisitJourneyRules.event(claimed, claimed.journey.latest!!.id, RoadStatus.MOCK_COMPLETED, "MOCK", "mock")
        assertEquals(VisitPhase.ROAD_GUIDANCE, complete.phase); assertNull(complete.journey.entry); assertNull(complete.parking)
        assertEquals(complete, VisitGuidanceCodec.decode(VisitGuidanceCodec.encode(complete)))
    }
    @Test fun sdkBroadcastAndDestinationEventsDoNotVerifyEntryOrPin() {
        val p = VisitJourneyRules.prepare(planned(), target(), RoadChannel.PLEOS)
        val c = VisitJourneyRules.claim(p, p.journey.latest!!.id)!!
        val returned = VisitJourneyRules.event(c, c.journey.latest!!.id, RoadStatus.SDK_CALL_RETURNED, "SDK_CALL_RETURNED", "only a function return")
        assertNull(returned.journey.entry)
        val e = VisitJourneyRules.event(returned, c.journey.latest!!.id, RoadStatus.SDK_EVENT_RECEIVED, "onRoutingCompleted", "uncorrelated")
        assertEquals(VisitPhase.ROAD_GUIDANCE, e.phase); assertNull(e.journey.entry)
        assertEquals(e, VisitJourneyRules.event(e, "other-request", RoadStatus.SDK_EVENT_RECEIVED, "late", "ignored"))
        assertEquals(e, VisitJourneyRules.event(e, e.journey.latest!!.id, RoadStatus.MOCK_COMPLETED, "fake", "ignored"))
    }
    @Test fun repeatedEntryNeverReopensDismissedGuide() {
        val p = VisitJourneyRules.prepare(planned(), target(), RoadChannel.MOCK)
        val (entered, created) = VisitJourneyRules.enter(p, "entry-one", GuidanceInputSource.TEST)
        assertTrue(created); assertEquals(VisitPhase.PARKING_GUIDANCE, entered.phase)
        val hidden = entered.copy(visibility = GuideVisibility.HIDDEN)
        val (duplicate, again) = VisitJourneyRules.enter(hidden, "entry-two", GuidanceInputSource.TEST)
        assertFalse(again); assertEquals(hidden, duplicate); assertEquals(planned().id, duplicate.id)
    }
    @Test fun cancellationRejectsLateCallbacksAndPreservesHistoryOnExplicitRetry() {
        val p = VisitJourneyRules.prepare(planned(), target(), RoadChannel.PLEOS)
        val c = VisitJourneyRules.claim(p, p.journey.latest!!.id)!!
        val cancelled = VisitJourneyRules.cancel(c)
        assertEquals(VisitPhase.PLANNED, cancelled.phase)
        assertEquals(cancelled, VisitJourneyRules.event(cancelled, c.journey.latest!!.id, RoadStatus.SDK_EVENT_RECEIVED, "late", "ignored"))
        val retry = VisitJourneyRules.prepare(cancelled, target().copy(version = "new-bundle", latitude = 37.0), RoadChannel.MOCK)
        assertEquals(c.journey.latest!!.target, retry.journey.latest!!.target)
        assertEquals(c.id, retry.id); assertEquals(2, retry.journey.attempts.size)
        assertNotEquals(c.journey.latest!!.id, retry.journey.latest!!.id)
    }
    @Test fun closedVisitAndUnpreparedEntryAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { VisitJourneyRules.enter(planned(), "entry", GuidanceInputSource.USER) }
        assertThrows(IllegalArgumentException::class.java) { VisitJourneyRules.prepare(planned().copy(phase = VisitPhase.CLOSED), target(), RoadChannel.MOCK) }
    }
    @Test fun concurrentRequestsClaimOneHandoffAndOrderedDismissalWins() = runTest {
        SqliteGuidanceStorage(app, "journey-${UUID.randomUUID()}.db").use { db ->
            val repo = VisitGuidanceRepository(db, backgroundScope, ::seed); runCurrent()
            repo.prepare(planned().destinationId); runCurrent(); val visit = repo.state.value.visit!!
            val ids = (1..8).map { async { repo.prepareRoad(visit.id, target(), RoadChannel.MOCK).id } }.awaitAll()
            assertEquals(1, ids.distinct().size)
            val claims = (1..8).map { async { repo.claimRoad(visit.id, ids.first()) } }.awaitAll()
            assertEquals(1, claims.count { it != null })
            repo.setVisibility(GuideVisibility.VISIBLE); repo.setVisibility(GuideVisibility.HIDDEN); runCurrent()
            assertEquals(GuideVisibility.HIDDEN, db.currentVisit()!!.visibility)
        }
    }
    @Test fun crashAfterClaimDoesNotReplayAndKeepsTargetSnapshot() {
        val name = "journey-${UUID.randomUUID()}.db"
        val claimed = SqliteGuidanceStorage(app, name).use { db ->
            db.installCatalog(seed()); val p = VisitJourneyRules.prepare(db.beginOrResume(planned().catalogVersion, planned().destinationId), target(), RoadChannel.PLEOS)
            VisitJourneyRules.claim(p, p.journey.latest!!.id)!!.also(db::saveVisit)
        }
        SqliteGuidanceStorage(app, name).use { db ->
            val restored = db.currentVisit()!!
            assertEquals(claimed, restored)
            val unchanged = VisitJourneyRules.prepare(restored, target().copy(version = "new-data", latitude = 37.0), RoadChannel.PLEOS)
            assertEquals(restored, unchanged); assertNull(VisitJourneyRules.claim(unchanged, unchanged.journey.latest!!.id))
        }
    }
    @Test fun failedHandoffJournalWritePreventsExternalCall() = runTest {
        SqliteGuidanceStorage(app, "failure-${UUID.randomUUID()}.db").use { db ->
            val storage = object : GuidanceStorage by db {
                override fun saveVisit(record: VisitRecord) {
                    if (record.journey.latest?.status == RoadStatus.DISPATCHED) throw IOException("injected write failure")
                    db.saveVisit(record)
                }
            }
            val repo = VisitGuidanceRepository(storage, backgroundScope, ::seed); runCurrent()
            repo.prepare(planned().destinationId); runCurrent(); val v = repo.state.value.visit!!
            val a = repo.prepareRoad(v.id, target(), RoadChannel.MOCK)
            var externalCalls = 0
            try { if (repo.claimRoad(v.id, a.id) != null) externalCalls++ } catch (_: IOException) { }
            assertEquals(0, externalCalls); assertEquals(RoadStatus.PREPARED, db.currentVisit()!!.journey.latest!!.status)
        }
    }
}
