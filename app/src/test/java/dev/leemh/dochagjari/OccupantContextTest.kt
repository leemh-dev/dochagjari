package dev.leemh.dochagjari

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OccupantContextTest {
    @Test fun migrationPreservesSkippedRolesAndMobilityWithoutStartingTrip() {
        val p = Profile(stroller = true, companion = true, heightM = 2.3,
            seats = SeatPreferences(SeatSetup.SKIPPED, right = SeatAssignment(SeatRole.INFANT, true)))
        val b = OccupantBook.migrate(p)
        assertTrue(b.skipped); assertNull(b.active)
        val legacy = b.presets.single { it.id == "legacy" }
        assertEquals(p.seats, legacy.seats); assertEquals(MobilityNeeds.from(p), legacy.needs)
        assertEquals(2.3, legacy.needs.applyTo(p).heightM, 0.0)
    }
    @Test fun selectingAndEditingThisTripDoesNotMutateSavedPresetOrTripId() {
        val b = OccupantBook.migrate(Profile()); val selected = b.select(b.presets[1], OccupantInput.PRESET, 100)
        val custom = b.presets[1].copy(name = "이번만", needs = MobilityNeeds(stroller = true))
        val once = selected.select(custom, OccupantInput.TEXT, 101)
        assertEquals(selected.active!!.id, once.active!!.id)
        assertEquals(b.presets, once.presets); assertFalse(b.presets[1].needs.stroller)
        assertTrue(once.active!!.needs.stroller)
    }
    @Test fun serializationRetainsTripSnapshotEvenAfterItsPresetWasDeleted() {
        val b = OccupantBook.migrate(Profile()); val chosen = b.presets[1]
        val current = b.select(chosen, OccupantInput.TEXT, 100).copy(presets = b.presets.filterNot { it.id == chosen.id })
        assertEquals(current, OccupantCodec.decode(OccupantCodec.encode(current)))
    }
    @Test fun futureOrMalformedDataIsRejectedRatherThanReset() {
        for (raw in listOf("{}", "{\"schema\":2}", "not-json")) {
            assertTrue(runCatching { OccupantCodec.decode(raw) }.isFailure)
        }
        val b = OccupantBook.migrate(Profile())
        assertTrue(runCatching { OccupantCodec.decode(OccupantCodec.encode(b).replace("SOLO", "FUTURE")) }.isFailure)
    }
    @Test fun noSensorObservationClaimsAChildIdentityOrSoloTrip() {
        assertTrue(OccupantSuggestions.describe(Occupancy.EMPTY, Occupancy.EMPTY).contains("앞좌석"))
        assertFalse(OccupantSuggestions.describe(Occupancy.OCCUPIED, Occupancy.EMPTY).contains("아이"))
        assertTrue(OccupantSuggestions.describe(Occupancy.UNKNOWN, Occupancy.EMPTY).contains("미확인"))
    }
    @Test fun explicitChildSideChangesOnlyThatSeatAndNotThePreset() {
        val b = OccupantBook.migrate(Profile())
        val r = OccupantCommands.parse("오늘 아이 왼쪽 뒤에 탔어", b)
        assertEquals(SeatRole.CHILD, r.candidates.single().seats.left.role)
        assertEquals(SeatRole.UNSPECIFIED, r.candidates.single().seats.right.role)
        assertEquals(SeatRole.UNSPECIFIED, b.presets[1].seats.left.role)
    }
    @Test fun ambiguousNegativeAndUnrelatedCommandsDoNotChooseAMode() {
        val b = OccupantBook.migrate(Profile())
        for (t in listOf("아이 안 탔어", "혼자 아이랑", "왼쪽 오른쪽 아이", "병원으로 가자", "취소"))
            assertTrue("$t should require clarification", OccupantCommands.parse(t, b).candidates.isEmpty())
        assertEquals(OccupantMode.SOLO, OccupantCommands.parse("오늘 혼자야", b).candidates.single().mode)
    }
    @Test fun matchingMultipleChildPresetsRequiresUserChoice() {
        val b = OccupantBook.migrate(Profile())
        val two = b.copy(presets = b.presets + b.presets[1].copy(id = "child2", name = "아이 왼쪽"))
        assertEquals(2, OccupantCommands.parse("아이랑 탔어", two).candidates.size)
    }
}
