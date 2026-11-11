package dev.leemh.dochagjari

import org.json.JSONArray
import java.time.Clock
import java.time.LocalTime
import java.util.UUID

enum class VisitMode(val label: String) { PICKUP("바로 태우기"), PARK("주차하고 데리러 가기"), DROPOFF("하차 후 주차"), TOGETHER("함께 주차") }
enum class Occupancy { EMPTY, OCCUPIED, UNKNOWN }
enum class Evidence { DEMO, VERIFIED, UNKNOWN }
enum class InputSource(val label: String) { VEHICLE("차량 API"), TEST("테스트 입력"), VIDEO("선택 영상 분석"), PUBLIC_VIDEO("공개 샘플 분석"), GENERATED("생성 영상 분석") }
data class Position(val latitude: Double, val longitude: Double)
data class AccessPoint(
    val id: String, val name: String, val kind: String, val position: Position,
    val stepFree: Evidence, val doorSide: String, val heightM: Double,
    val open: String, val close: String, val walking: String, val permitted: Boolean = true
)
data class Facility(val id: String, val name: String, val entrance: String, val fictional: Boolean, val points: List<AccessPoint>)
data class Profile(val leftDoor: Boolean = false, val rightDoor: Boolean = false, val stroller: Boolean = false, val wheelchair: Boolean = false, val companion: Boolean = false, val heightM: Double = 1.8)
data class Observation(val occupancy: Occupancy, val confidence: Float, val observedAt: Long, val source: InputSource, val detail: String = "") {
    fun effective(now: Long): Occupancy = if (confidence >= .65f && now - observedAt in 0..30_000) occupancy else Occupancy.UNKNOWN
}
data class Recommendation(val point: AccessPoint?, val reason: String, val excluded: List<String>)

class Recommender(private val clock: Clock = Clock.systemDefaultZone()) {
    fun recommend(facility: Facility, mode: VisitMode, profile: Profile, blocked: Set<String>, observations: Map<String, Observation>): Recommendation {
        val rejected = mutableListOf<String>()
        val now = clock.millis()
        val time = LocalTime.now(clock)
        val parking = mode != VisitMode.PICKUP && mode != VisitMode.DROPOFF || profile.companion
        val candidates = facility.points.filter { p ->
            val from = LocalTime.parse(p.open); val until = LocalTime.parse(p.close)
            val isOpen = if (from == until) true else if (from < until) time >= from && time < until else time >= from || time < until
            val accessible = p.stepFree == Evidence.VERIFIED || facility.fictional && p.stepFree == Evidence.DEMO
            val reason = when {
                !p.permitted -> "이용 허용 미확인"
                p.id in blocked -> "이번 방문 이용 불가"
                observations[p.id]?.effective(now) == Occupancy.OCCUPIED -> "영상에서 점유 관측"
                !isOpen -> "운영시간 밖"
                profile.heightM > p.heightM -> "차량 높이 제한"
                (profile.stroller || profile.wheelchair) && !accessible -> "무단차 미확인"
                parking && p.kind != "parking" -> "주차·동행 필요"
                else -> null
            }
            if (reason != null) rejected += "${p.name}: $reason"
            reason == null
        }.sortedWith(compareBy<AccessPoint> {
            if (parking) 0 else if (it.kind == "dropoff") 0 else 1
        }.thenBy {
            if (profile.leftDoor && it.doorSide !in listOf("left", "both") || profile.rightDoor && it.doorSide !in listOf("right", "both")) 1 else 0
        })
        val chosen = candidates.firstOrNull()
        return Recommendation(chosen, when {
            chosen == null -> "조건에 맞는 후보가 없습니다"
            chosen.kind == "parking" && !parking -> "승하차점 대신 이용 가능한 주차장으로 우회"
            profile.companion -> "보호자와 함께 주차한 뒤 출입구로 이동"
            chosen.kind == "parking" -> "주차 후 ${facility.entrance}까지 이동"
            mode == VisitMode.DROPOFF -> "동승자 하차 후 주차장으로 이동"
            else -> "같은 출입구에 연결된 승하차점"
        }, rejected)
    }
}

object FacilityParser {
    fun parse(json: String): List<Facility> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val f = array.getJSONObject(i); val points = f.getJSONArray("points")
            Facility(f.getString("id"), f.getString("name"), f.getString("entrance"), f.getBoolean("fictional"), (0 until points.length()).map { j ->
                val p = points.getJSONObject(j)
                AccessPoint(p.getString("id"), p.getString("name"), p.getString("kind"), Position(p.getDouble("latitude"), p.getDouble("longitude")), Evidence.valueOf(p.getString("stepFree")), p.getString("doorSide"), p.getDouble("heightM"), p.getString("open"), p.getString("close"), p.getString("walking"))
            })
        }
    }
}

data class PickupSession(
    val id: String = UUID.randomUUID().toString(), val stopped: Boolean = false,
    val confirmed: Boolean = false, val position: Position? = null, val clue: String = "",
    val notification: String = "대기", val source: InputSource = InputSource.TEST
) {
    val eligible: Boolean get() = stopped && confirmed && position != null && clue.isNotBlank() && notification == "대기"
    fun shareText(): String = "$clue\nhttps://maps.google.com/?q=${position?.latitude},${position?.longitude}"
}
