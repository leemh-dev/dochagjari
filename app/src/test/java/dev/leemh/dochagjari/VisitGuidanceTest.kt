package dev.leemh.dochagjari

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VisitGuidanceTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private fun seed() = app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() }
    private fun database() = "guidance-${UUID.randomUUID()}.db"
    private val version = "pangyo-ettoi-20261113-v1"
    private val id = "hyundai-pangyo-ettoi"

    @Test fun offlineAliasLookupKeepsUnknownParkingAndDrawingUnitsSeparate() {
        SqliteGuidanceStorage(app, database()).use { db ->
            db.installCatalog(seed())
            listOf("에뜨와", " 에뜨와 판교 현백 ", "판교·현대백화점 에뜨와").forEach { query ->
                val found = db.search(version, query).single()
                assertEquals(id, found.id); assertEquals("5F", found.floor); assertEquals(14, found.route.size)
                assertTrue(found.unverifiedFields.any { it.contains("기둥") })
            }
            assertTrue(db.search(version, "아가방").isEmpty())
            assertTrue(db.search(version, "더현대 서울 에뜨와").isEmpty())
            assertTrue(db.search(version, "' OR 1=1 --").isEmpty())
        }
    }
    @Test fun ambiguousAliasReturnsAllMatchesRatherThanSilentlyChoosingOne() {
        val json = JSONObject(seed()); val destinations = json.getJSONArray("destinations")
        val other = JSONObject(destinations.getJSONObject(0).toString()).put("id", "test-other-branch")
        destinations.put(other)
        SqliteGuidanceStorage(app, database()).use { db ->
            db.installCatalog(json.toString())
            assertEquals(2, db.search(version, "에뜨와").size)
        }
    }
    @Test fun closedVisitAndParkingEvidenceSurviveDatabaseReopenWithSameIdentity() {
        val name = database()
        val expected = SqliteGuidanceStorage(app, name).use { db ->
            db.installCatalog(seed())
            db.beginOrResume(version, id).copy(phase = VisitPhase.CLOSED, visibility = GuideVisibility.HIDDEN,
                parking = ParkingClue(floor = "TEST_B3", zone = "TEST_ZONE", confirmed = false,
                    source = GuidanceInputSource.TEST, observedAt = 1000)).also(db::saveVisit)
        }
        SqliteGuidanceStorage(app, name).use { db ->
            assertEquals(expected, db.currentVisit())
            assertEquals(expected, db.beginOrResume(version, id))
            assertNull(db.currentVisit()!!.parking!!.pillar)
        }
    }
    @Test fun newCatalogImportRetainsOriginalVisitAndOldSnapshot() {
        SqliteGuidanceStorage(app, database()).use { db ->
            db.installCatalog(seed()); val original = db.beginOrResume(version, id)
            val changed = JSONObject(seed()).put("version", "test-v2")
            changed.getJSONArray("destinations").getJSONObject(0).put("floor", "TEST_NEW_FLOOR")
            db.installCatalog(changed.toString())
            assertEquals("TEST_NEW_FLOOR", db.destination("test-v2", id)!!.floor)
            assertEquals("5F", db.destination(version, id)!!.floor)
            assertEquals(original, db.beginOrResume("test-v2", id))
            assertEquals(original, db.currentVisit())
        }
    }
    @Test fun alteredSameVersionIsRejectedWithoutDestroyingVisitOrCatalog() {
        SqliteGuidanceStorage(app, database()).use { db ->
            db.installCatalog(seed()); val original = db.beginOrResume(version, id)
            val changed = JSONObject(seed()); changed.getJSONArray("destinations").getJSONObject(0).put("floor", "TEST_BAD_FLOOR")
            assertThrows(IllegalArgumentException::class.java) { db.installCatalog(changed.toString()) }
            assertEquals("5F", db.destination(version, id)!!.floor)
            assertEquals(original, db.currentVisit())
            assertThrows(IllegalArgumentException::class.java) { db.saveVisit(original.copy(destinationId = "different")) }
            assertEquals(original, db.currentVisit())
        }
    }
    @Test fun concurrentRepeatedPrepareCreatesOnlyOneVisitAcrossConnections() {
        val name = database(); val first = SqliteGuidanceStorage(app, name); val second = SqliteGuidanceStorage(app, name)
        val pool = Executors.newFixedThreadPool(2)
        try {
            first.installCatalog(seed()); second.installCatalog(seed())
            val tasks = (0 until 10).map { i -> pool.submit<String> { (if (i % 2 == 0) first else second).beginOrResume(version, id).id } }
            assertEquals(1, tasks.map { it.get(10, TimeUnit.SECONDS) }.toSet().size)
            assertEquals(first.currentVisit(), second.currentVisit())
        } finally { pool.shutdownNow(); first.close(); second.close() }
    }
    @Test fun unknownDestinationCannotCreateAnOrphanVisit() {
        SqliteGuidanceStorage(app, database()).use { db ->
            db.installCatalog(seed())
            assertThrows(IllegalArgumentException::class.java) { db.beginOrResume(version, "missing") }
            assertNull(db.currentVisit())
        }
    }
    @Test fun repositoryOnlyPublishesSavedVisitAfterSuccessfulWriteAndCanRetry() = runTest {
        SqliteGuidanceStorage(app, database()).use { db ->
            var fail = true
            val storage = object : GuidanceStorage by db {
                override fun beginOrResume(version: String, destinationId: String): VisitRecord {
                    if (fail) throw IOException("injected write failure")
                    return db.beginOrResume(version, destinationId)
                }
            }
            val repo = VisitGuidanceRepository(storage, backgroundScope, ::seed)
            runCurrent(); repo.search("에뜨와 판교 현백"); runCurrent()
            assertEquals(id, repo.state.value.matches.single().id)
            repo.prepare(id); runCurrent()
            assertNull(repo.state.value.visit); assertNull(db.currentVisit())
            assertTrue(repo.state.value.error.contains("저장 실패"))
            fail = false; repo.prepare(id); runCurrent()
            assertEquals(db.currentVisit(), repo.state.value.visit)
            assertEquals("", repo.state.value.error)
            val reopenedRepo = VisitGuidanceRepository(db, backgroundScope, ::seed)
            runCurrent(); assertEquals(repo.state.value.visit, reopenedRepo.state.value.visit)
        }
    }
    @Test fun previewVisibilityPersistsWithoutReplacingVisitOrParkingEvidence() = runTest {
        SqliteGuidanceStorage(app, database()).use { db ->
            val repo = VisitGuidanceRepository(db, backgroundScope, ::seed)
            runCurrent(); repo.prepare(id); runCurrent()
            val before = repo.state.value.visit!!
            repo.setVisibility(GuideVisibility.HIDDEN); runCurrent()
            assertEquals(before.copy(visibility = GuideVisibility.HIDDEN), db.currentVisit())
            val restored = VisitGuidanceRepository(db, backgroundScope, ::seed)
            runCurrent(); assertEquals(GuideVisibility.HIDDEN, restored.state.value.visit!!.visibility)
            restored.setVisibility(GuideVisibility.VISIBLE); runCurrent()
            assertEquals(before, db.currentVisit())
        }
    }

}
