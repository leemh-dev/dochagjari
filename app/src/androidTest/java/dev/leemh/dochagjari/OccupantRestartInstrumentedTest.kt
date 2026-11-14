package dev.leemh.dochagjari

import android.app.Application
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Host invokes these individually around force-stop on its own new test AVD. */
class OccupantRestartInstrumentedTest {
    @Test fun prepare() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("occupantPhase") == "prepare")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val services = AppServices.get(app); val repo = requireNotNull(services.occupants)
        withTimeout(15_000) { repo.state.first { it.saved } }
        repo.activate(repo.state.value.book.presets.first { it.mode == OccupantMode.CHILD })
        withTimeout(15_000) { repo.state.first { it.saved && it.active != null } }
        val profile = services.profiles.readStoredProfile().toString()
        val journal = FileArrivalJournal(File(app.filesDir, "arrival/outbox.json")).load()
        val visitId = SqliteGuidanceStorage(app).use { it.currentVisit()?.id }
        val proof = JSONObject().put("pid", Process.myPid()).put("book", OccupantCodec.encode(repo.readStored()))
            .put("profile", profile).put("visitId", visitId ?: JSONObject.NULL)
            .put("outbox", journal?.request?.fingerprint() ?: JSONObject.NULL)
        File(app.filesDir, "occupant-restart-proof.json").writeText(proof.toString())
        println("OCCUPANT_PREPARED_PID=${Process.myPid()}")
    }
    @Test fun verify() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("occupantPhase") == "verify")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val proof = JSONObject(File(app.filesDir, "occupant-restart-proof.json").readText())
        val services = AppServices.get(app); val repo = requireNotNull(services.occupants)
        withTimeout(15_000) { repo.state.first { it.saved } }
        assertNotEquals(proof.getInt("pid"), Process.myPid())
        assertEquals(OccupantCodec.decode(proof.getString("book")), repo.readStored())
        assertNull(repo.state.value.active); assertFalse(repo.state.value.confirmedThisRun)
        assertEquals(proof.getString("profile"), services.profiles.readStoredProfile().toString())
        assertEquals(proof.optString("visitId", "null"), SqliteGuidanceStorage(app).use { it.currentVisit()?.id } ?: "null")
        assertEquals(proof.optString("outbox", "null"), FileArrivalJournal(File(app.filesDir, "arrival/outbox.json")).load()?.request?.fingerprint() ?: "null")
        println("OCCUPANT_VERIFIED_PID=${Process.myPid()}")
    }
}
