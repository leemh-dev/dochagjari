package dev.leemh.dochagjari

import org.json.JSONArray
import org.json.JSONObject

data class ParkingDetails(val floor: String? = null, val zone: String? = null, val pillar: String? = null, val elevator: String? = null, val memo: String = "") {
    fun json() = JSONObject().put("floor", floor ?: JSONObject.NULL).put("zone", zone ?: JSONObject.NULL)
        .put("pillar", pillar ?: JSONObject.NULL).put("elevator", elevator ?: JSONObject.NULL).put("memo", memo)
    companion object {
        fun parse(j: JSONObject) = ParkingDetails(if (j.isNull("floor")) null else j.getString("floor"),
            if (j.isNull("zone")) null else j.getString("zone"), if (j.isNull("pillar")) null else j.getString("pillar"), if (j.isNull("elevator")) null else j.getString("elevator"), j.getString("memo"))
    }
}
data class ParkingRevision(val id: String, val source: GuidanceInputSource, val confirmedAt: Long, val details: ParkingDetails)
data class ParkingResume(val at: Long, val vehicleSource: InputSource, val explicit: Boolean)
data class ParkedVisit(val id: String, val completedAt: Long, val source: GuidanceInputSource, val vehicleSource: InputSource,
    val revisions: List<ParkingRevision> = emptyList(), val resumed: ParkingResume? = null) {
    val notes get() = revisions.lastOrNull()
}
data class VisitParkingJournal(val records: List<ParkedVisit> = emptyList()) {
    val latest get() = records.lastOrNull()
    val active get() = latest?.takeIf { it.resumed == null }
    fun json() = JSONObject().put("records", JSONArray().apply { records.forEach { r -> put(JSONObject().apply {
        put("id", r.id); put("completedAt", r.completedAt); put("source", r.source.name); put("vehicleSource", r.vehicleSource.name)
        put("revisions", JSONArray().apply { r.revisions.forEach { n -> put(JSONObject().put("id", n.id).put("source", n.source.name).put("confirmedAt", n.confirmedAt).put("details", n.details.json())) } })
        r.resumed?.let { put("resumed", JSONObject().put("at", it.at).put("vehicleSource", it.vehicleSource.name).put("explicit", it.explicit)) }
    }) } })
    companion object {
        fun parse(j: JSONObject?): VisitParkingJournal {
            if (j == null) return VisitParkingJournal()
            val a = j.getJSONArray("records")
            return VisitParkingJournal((0 until a.length()).map { i -> val r = a.getJSONObject(i); val ns = r.getJSONArray("revisions"); val resumed = r.optJSONObject("resumed")
                ParkedVisit(r.getString("id"), r.getLong("completedAt"), GuidanceInputSource.valueOf(r.getString("source")), InputSource.valueOf(r.getString("vehicleSource")),
                    (0 until ns.length()).map { k -> val n = ns.getJSONObject(k); ParkingRevision(n.getString("id"), GuidanceInputSource.valueOf(n.getString("source")), n.getLong("confirmedAt"), ParkingDetails.parse(n.getJSONObject("details"))) },
                    resumed?.let { ParkingResume(it.getLong("at"), InputSource.valueOf(it.getString("vehicleSource")), it.getBoolean("explicit")) })
            })
        }
    }
}
data class ParkingCommit(val visit: VisitRecord, val created: Boolean)
object VisitParkingRules {
    fun complete(v: VisitRecord, id: String, source: GuidanceInputSource, vehicleSource: InputSource, at: Long): ParkingCommit {
        require(id.isNotBlank() && at > 0 && source in setOf(GuidanceInputSource.USER, GuidanceInputSource.TEST))
        require(vehicleSource in setOf(InputSource.TEST, InputSource.VEHICLE))
        if (v.parked.records.any { it.id == id } || v.parked.active != null) return ParkingCommit(v, false)
        require(v.journey.entry != null && v.phase == VisitPhase.PARKING_GUIDANCE)
        require(v.parked.records.size < 16) { "Parking history full; retain previous records" }
        val stoppedRuns = v.observation.runs.map { if (it.stopReason == null) it.copy(stopReason = "주차 완료 · 관측 중지") else it }
        return ParkingCommit(v.copy(phase = VisitPhase.PARKED, visibility = GuideVisibility.HIDDEN,
            observation = VisitObservationJournal(stoppedRuns), parked = VisitParkingJournal(v.parked.records + ParkedVisit(id, at, source, vehicleSource))), true)
    }
    fun amend(v: VisitRecord, parkingId: String, revision: ParkingRevision): VisitRecord {
        val r = requireNotNull(v.parked.active); require(r.id == parkingId && v.phase == VisitPhase.PARKED)
        require(revision.source in setOf(GuidanceInputSource.USER, GuidanceInputSource.TEST) && revision.id.isNotBlank() && revision.confirmedAt > 0)
        val d = revision.details
        require(listOf(d.floor, d.zone, d.pillar, d.elevator).all { it == null || it.isNotBlank() && it.length <= 64 })
        require(d.memo.length <= 280)
        r.revisions.find { it.id == revision.id }?.let { require(it == revision) { "Conflicting revision ID" }; return v }
        require(r.revisions.size < 32) { "Notes history full; retain earlier confirmations" }
        return replace(v, r.copy(revisions = r.revisions + revision))
    }
    fun resume(v: VisitRecord, parkingId: String, source: InputSource, explicit: Boolean, at: Long): VisitRecord {
        val r = v.parked.active ?: return v
        if (r.id != parkingId) return v
        require(source in setOf(InputSource.TEST, InputSource.VEHICLE) && at > 0)
        return replace(v, r.copy(resumed = ParkingResume(at, source, explicit))).copy(phase = VisitPhase.PARKING_GUIDANCE, visibility = GuideVisibility.HIDDEN)
    }
    private fun replace(v: VisitRecord, r: ParkedVisit) = v.copy(parked = VisitParkingJournal(v.parked.records.dropLast(1) + r))
}
