package dev.leemh.dochagjari

import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID

enum class RoadTargetKind { FACILITY_POI }
data class RoadTarget(val version: String, val facilityId: String, val name: String, val address: String,
    val latitude: Double, val longitude: Double, val kind: RoadTargetKind, val sourceUrl: String, val checkedAt: String) {
    fun json() = JSONObject().apply {
        put("version", version); put("facilityId", facilityId); put("name", name); put("address", address)
        put("latitude", latitude); put("longitude", longitude); put("kind", kind.name); put("sourceUrl", sourceUrl); put("checkedAt", checkedAt)
    }
    companion object {
        fun parse(j: JSONObject) = RoadTarget(j.getString("version"), j.getString("facilityId"), j.getString("name"), j.getString("address"),
            j.getDouble("latitude"), j.getDouble("longitude"), RoadTargetKind.valueOf(j.getString("kind")), j.getString("sourceUrl"), j.getString("checkedAt")).also {
            require(it.version.isNotBlank() && it.facilityId.isNotBlank() && it.name.isNotBlank())
            require(it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0)
            require(it.sourceUrl.startsWith("https://www.ehyundai.com/"))
        }
    }
}
enum class RoadChannel { MOCK, PLEOS }
enum class RoadStatus { PREPARED, DISPATCHED, MOCK_COMPLETED, SDK_CALL_RETURNED, SDK_EVENT_RECEIVED, FAILED, INTERRUPTED, CANCELLED }
data class RoadEvent(val kind: String, val detail: String, val at: Long)
data class RoadAttempt(val id: String, val target: RoadTarget, val channel: RoadChannel, val status: RoadStatus = RoadStatus.PREPARED,
    val events: List<RoadEvent> = emptyList())
data class EntryEvent(val id: String, val source: GuidanceInputSource, val at: Long)
data class VisitJourney(val attempts: List<RoadAttempt> = emptyList(), val entry: EntryEvent? = null) {
    val latest: RoadAttempt? get() = attempts.lastOrNull()
    fun json() = JSONObject().apply {
        put("attempts", JSONArray().apply { attempts.forEach { a -> put(JSONObject().apply {
            put("id", a.id); put("target", a.target.json()); put("channel", a.channel.name); put("status", a.status.name)
            put("events", JSONArray().apply { a.events.forEach { e -> put(JSONObject().put("kind", e.kind).put("detail", e.detail).put("at", e.at)) } })
        }) } })
        entry?.let { put("entry", JSONObject().put("id", it.id).put("source", it.source.name).put("at", it.at)) }
    }
    companion object {
        fun parse(j: JSONObject?): VisitJourney {
            if (j == null) return VisitJourney()
            val a = j.getJSONArray("attempts"); val entry = j.optJSONObject("entry")
            return VisitJourney((0 until a.length()).map { i -> val v = a.getJSONObject(i); val events = v.getJSONArray("events")
                RoadAttempt(v.getString("id"), RoadTarget.parse(v.getJSONObject("target")), RoadChannel.valueOf(v.getString("channel")), RoadStatus.valueOf(v.getString("status")),
                    (0 until events.length()).map { k -> events.getJSONObject(k).let { RoadEvent(it.getString("kind"), it.getString("detail"), it.getLong("at")) } }) },
                entry?.let { EntryEvent(it.getString("id"), GuidanceInputSource.valueOf(it.getString("source")), it.getLong("at")) })
        }
    }
}

/** Pure transitions: a route callback is evidence only; entry requires a separate explicit event. */
object VisitJourneyRules {
    private val terminal = setOf(RoadStatus.FAILED, RoadStatus.INTERRUPTED, RoadStatus.CANCELLED)
    fun prepare(v: VisitRecord, target: RoadTarget, channel: RoadChannel): VisitRecord {
        require(v.phase in setOf(VisitPhase.PLANNED, VisitPhase.ROAD_GUIDANCE) && v.journey.entry == null)
        if (v.journey.latest?.status?.let { it !in terminal } == true) return v
        require(v.journey.attempts.size < 16) { "Attempt history limit; do not erase prior requests" }
        return v.copy(phase = VisitPhase.ROAD_GUIDANCE, journey = v.journey.copy(attempts = v.journey.attempts + RoadAttempt(UUID.randomUUID().toString(), v.journey.latest?.target ?: target, channel)))
    }
    fun claim(v: VisitRecord, id: String): VisitRecord? {
        val a = v.journey.latest ?: return null
        if (v.phase != VisitPhase.ROAD_GUIDANCE || a.id != id || a.status != RoadStatus.PREPARED) return null
        return replace(v, a.copy(status = RoadStatus.DISPATCHED))
    }
    fun event(v: VisitRecord, id: String, status: RoadStatus, kind: String, detail: String): VisitRecord {
        val a = v.journey.latest ?: return v
        if (a.id != id || a.status in terminal || v.journey.entry != null || v.phase != VisitPhase.ROAD_GUIDANCE) return v
        require(status in setOf(RoadStatus.MOCK_COMPLETED, RoadStatus.SDK_CALL_RETURNED, RoadStatus.SDK_EVENT_RECEIVED, RoadStatus.FAILED, RoadStatus.INTERRUPTED))
        if (a.status == RoadStatus.PREPARED && status != RoadStatus.INTERRUPTED) return v
        if (status == RoadStatus.MOCK_COMPLETED && a.channel != RoadChannel.MOCK) return v
        if (status in setOf(RoadStatus.SDK_CALL_RETURNED, RoadStatus.SDK_EVENT_RECEIVED) && a.channel != RoadChannel.PLEOS) return v
        return replace(v, a.copy(status = if (a.status == RoadStatus.SDK_EVENT_RECEIVED && status == RoadStatus.SDK_CALL_RETURNED) a.status else status, events = (a.events + RoadEvent(kind, detail, System.currentTimeMillis())).takeLast(16)))
    }
    fun cancel(v: VisitRecord): VisitRecord {
        val a = v.journey.latest ?: return v
        if (v.journey.entry != null || v.phase != VisitPhase.ROAD_GUIDANCE) return v
        return replace(v.copy(phase = VisitPhase.PLANNED), a.copy(status = RoadStatus.CANCELLED))
    }
    fun enter(v: VisitRecord, id: String, source: GuidanceInputSource): Pair<VisitRecord, Boolean> {
        require(id.isNotBlank() && source in setOf(GuidanceInputSource.USER, GuidanceInputSource.TEST))
        if (v.journey.entry != null) return v to false
        require(v.phase == VisitPhase.ROAD_GUIDANCE && v.journey.latest != null)
        return v.copy(phase = VisitPhase.PARKING_GUIDANCE, visibility = GuideVisibility.VISIBLE,
            journey = v.journey.copy(entry = EntryEvent(id, source, System.currentTimeMillis()))) to true
    }
    private fun replace(v: VisitRecord, a: RoadAttempt) = v.copy(journey = v.journey.copy(attempts = v.journey.attempts.dropLast(1) + a))
}
