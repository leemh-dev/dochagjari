package dev.leemh.dochagjari

import org.junit.Assert.*
import org.junit.Test

class SeatPreferencesTest {
    @Test fun occupancyOnlySuggestsExplicitlyConfiguredChoices() {
        val seats = SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.INFANT, true), SeatAssignment(SeatRole.ADULT))
        assertTrue(seats.suggestion(true, Occupancy.OCCUPIED)!!.contains("영유아"))
        assertTrue(seats.suggestion(false, Occupancy.OCCUPIED)!!.contains("성인"))
        assertNull(seats.suggestion(true, Occupancy.EMPTY)); assertNull(seats.suggestion(true, Occupancy.UNKNOWN))
        assertNull(seats.copy(setup = SeatSetup.SKIPPED).suggestion(true, Occupancy.OCCUPIED))
        assertEquals(SeatRole.INFANT, seats.left.role)
    }
    @Test fun unconfiguredOrUnspecifiedRolesAreNotInferredFromOccupancy() {
        assertNull(SeatPreferences().suggestion(true, Occupancy.OCCUPIED))
        assertNull(SeatPreferences(setup = SeatSetup.CONFIGURED).suggestion(true, Occupancy.OCCUPIED))
    }
}
