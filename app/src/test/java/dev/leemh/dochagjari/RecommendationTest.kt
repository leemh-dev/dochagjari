package dev.leemh.dochagjari

import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class RecommendationTest {
    private val now = Instant.parse("2026-11-11T03:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Asia/Seoul"))
    private val engine = Recommender(clock)
    private val stop = AccessPoint("stop", "승하차", "dropoff", Position(37.48,127.03), Evidence.DEMO, "right", 4.0, "00:00", "00:00", "보행 출입구")
    private val parking = stop.copy(id = "parking", name = "주차장", kind = "parking", position = Position(37.49,127.04), doorSide = "both", heightM = 2.3)
    private val facility = Facility("academy", "가상학원", "서쪽문", true, listOf(stop, parking))
    private fun choose(mode: VisitMode = VisitMode.PICKUP, profile: Profile = Profile(), blocked: Set<String> = emptySet(), observations: Map<String, Observation> = emptyMap(), f: Facility = facility) = engine.recommend(f,mode,profile,blocked,observations)

    @Test fun modesChangeVehiclePointKeepPedestrianEntrance() {
        assertEquals("stop", choose().point?.id)
        assertEquals("parking", choose(VisitMode.PARK).point?.id)
        assertEquals("서쪽문", facility.entrance)
        assertNotEquals(stop.position, parking.position)
    }
    @Test fun blockedAndOccupiedDivertToParking() {
        assertEquals("parking", choose(blocked = setOf("stop")).point?.id)
        val occupied = Observation(Occupancy.OCCUPIED,.9f,clock.millis(),InputSource.VIDEO)
        assertEquals("parking", choose(observations = mapOf("stop" to occupied)).point?.id)
        assertNull(choose(blocked = setOf("stop","parking")).point)
    }
    @Test fun expiredLowConfidenceAndFutureAreUnknown() {
        listOf(Observation(Occupancy.OCCUPIED,.9f,clock.millis()-31_000,InputSource.VIDEO),
            Observation(Occupancy.OCCUPIED,.3f,clock.millis(),InputSource.VIDEO),
            Observation(Occupancy.OCCUPIED,.9f,clock.millis()+1,InputSource.VIDEO)).forEach {
            assertEquals(Occupancy.UNKNOWN, it.effective(clock.millis()))
            assertEquals("stop", choose(observations = mapOf("stop" to it)).point?.id)
        }
    }
    @Test fun companionNeverLeavesPassengerAtDropoff() {
        assertEquals("parking", choose(VisitMode.DROPOFF, Profile(companion=true)).point?.id)
    }
    @Test fun realUnknownAccessibilityNeverPassesMandatoryConstraint() {
        val real = facility.copy(fictional=false, points=listOf(stop.copy(stepFree=Evidence.UNKNOWN), parking.copy(stepFree=Evidence.VERIFIED)))
        assertEquals("parking", choose(profile=Profile(wheelchair=true), f=real).point?.id)
        assertNull(choose(profile=Profile(stroller=true), f=real.copy(points=listOf(stop))).point)
    }
    @Test fun fictionalAccessibilityIsExplicitlyDemo() {
        assertEquals(Evidence.DEMO, choose(profile=Profile(stroller=true)).point?.stepFree)
    }
    @Test fun openingHoursHeightAndPermissionFilter() {
        assertNull(choose(VisitMode.PARK,Profile(heightM=2.5)).point)
        assertEquals("parking",choose(f=facility.copy(points=listOf(stop.copy(open="13:00",close="16:00"),parking))).point?.id)
        assertEquals("parking",choose(f=facility.copy(points=listOf(stop.copy(permitted=false),parking))).point?.id)
        assertNull(choose(f=facility.copy(points=listOf(stop.copy(open="22:00",close="06:00")))).point)
    }
    @Test fun notificationNeedsStopLocationClueAndUnsentSession() {
        val s = PickupSession(stopped=true,confirmed=true,position=stop.position,clue="B1 C12")
        assertTrue(s.eligible)
        assertFalse(s.copy(stopped=false).eligible)
        assertFalse(s.copy(confirmed=false).eligible)
        assertFalse(s.copy(position=null).eligible)
        assertFalse(s.copy(clue="").eligible)
        assertFalse(s.copy(notificationStatus=DeliveryStatus.MOCK).eligible)
        assertTrue(s.shareText().contains("37.48,127.03"))
        assertFalse(s.shareText().contains(s.id))
    }
    @Test fun bundledJsonLoadsBothFacilities() {
        val f = FacilityParser.parse(java.io.File("src/main/assets/facilities.json").readText())
        assertEquals(2, f.size)
        assertTrue(f.all { it.fictional })
        assertEquals(2, f[0].points.size)
        assertEquals(3, f[1].points.size)
    }
}
