package dev.leemh.dochagjari

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Catalog coordinates describe the floor drawing, never GPS or measured meters. */
data class FloorPoint(val x: Double, val y: Double)
data class IndoorDestination(
    val id: String, val facilityId: String, val facilityName: String, val name: String,
    val floor: String, val aliases: List<String>, val sourceUrl: String, val checkedAt: String,
    val routeStartId: String, val routeStartDescription: String, val routeEndId: String,
    val route: List<FloorPoint>, val unverifiedFields: List<String>
)
data class FacilityCatalog(val version: String, val destinations: List<IndoorDestination>)
enum class VisitPhase { PLANNED, ROAD_GUIDANCE, PARKING_GUIDANCE, PARKING_CANDIDATE, PARKED, SUMMARY, CLOSED }
enum class GuideVisibility { VISIBLE, HIDDEN }
enum class GuidanceInputSource { USER, TEST, RECORDED_VIDEO, VEHICLE }
data class ParkingClue(
    val floor: String? = null, val zone: String? = null, val pillar: String? = null,
    val elevatorId: String? = null, val confirmed: Boolean = false,
    val source: GuidanceInputSource, val observedAt: Long
)
data class VisitRecord(
    val id: String, val catalogVersion: String, val destinationId: String,
    val originLabel: String = "현대자동차 양재 사옥", val createdAt: Long,
    val phase: VisitPhase = VisitPhase.PLANNED, val visibility: GuideVisibility = GuideVisibility.VISIBLE,
    val source: GuidanceInputSource = GuidanceInputSource.USER, val parking: ParkingClue? = null, val journey: VisitJourney = VisitJourney(), val observation: VisitObservationJournal = VisitObservationJournal(), val parked: VisitParkingJournal = VisitParkingJournal()
)

object VisitGuidanceCodec {
    fun normalize(value: String): String = value.lowercase(Locale.ROOT).replace(Regex("[\\s·.,()_\\-]"), "")
    private fun strings(array: JSONArray): List<String> = (0 until array.length()).map(array::getString)
    fun catalog(json: String): FacilityCatalog {
        val root = JSONObject(json)
        val items = root.getJSONArray("destinations")
        val destinations = (0 until items.length()).map { destination(items.getJSONObject(it)) }
        require(root.getString("version").isNotBlank() && destinations.isNotEmpty())
        require(destinations.map { it.id }.distinct().size == destinations.size)
        return FacilityCatalog(root.getString("version"), destinations)
    }
    fun destination(json: JSONObject): IndoorDestination {
        val route = json.getJSONObject("route")
        require(route.getString("coordinateUnit") == "DRAWING_UNITS_UNVERIFIED")
        val points = route.getJSONArray("points")
        return IndoorDestination(
            json.getString("id"), json.getString("facilityId"), json.getString("facilityName"),
            json.getString("name"), json.getString("floor"), strings(json.getJSONArray("aliases")),
            json.getString("sourceUrl"), json.getString("checkedAt"), route.getString("startId"),
            route.getString("startDescription"), route.getString("endId"),
            (0 until points.length()).map { FloorPoint(points.getJSONObject(it).getDouble("x"), points.getJSONObject(it).getDouble("y")) },
            strings(json.getJSONArray("unverifiedFields"))
        ).also { d ->
            require(d.id.isNotBlank() && d.floor.isNotBlank() && d.aliases.isNotEmpty())
            require(d.route.size >= 2 && d.route.all { it.x.isFinite() && it.y.isFinite() })
            require(d.aliases.all { normalize(it).isNotEmpty() })
        }
    }
    fun encode(record: VisitRecord): String = JSONObject().apply {
        put("id", record.id); put("catalogVersion", record.catalogVersion); put("destinationId", record.destinationId)
        put("originLabel", record.originLabel); put("createdAt", record.createdAt)
        put("phase", record.phase.name); put("visibility", record.visibility.name); put("source", record.source.name)
        put("parked", record.parked.json()); put("journey", record.journey.json()); put("observation", record.observation.json())
        record.parking?.let { p -> put("parking", JSONObject().apply {
            put("floor", p.floor); put("zone", p.zone); put("pillar", p.pillar); put("elevatorId", p.elevatorId)
            put("confirmed", p.confirmed); put("source", p.source.name); put("observedAt", p.observedAt)
        }) }
    }.toString()
    fun decode(value: String): VisitRecord {
        val j = JSONObject(value)
        val p = j.optJSONObject("parking")
        fun optional(field: String): String? = if (p == null || p.isNull(field)) null else p.getString(field)
        return VisitRecord(j.getString("id"), j.getString("catalogVersion"), j.getString("destinationId"),
            j.getString("originLabel"), j.getLong("createdAt"), VisitPhase.valueOf(j.getString("phase")),
            GuideVisibility.valueOf(j.getString("visibility")), GuidanceInputSource.valueOf(j.getString("source")),
            p?.let { ParkingClue(optional("floor"), optional("zone"), optional("pillar"), optional("elevatorId"),
                it.getBoolean("confirmed"), GuidanceInputSource.valueOf(it.getString("source")), it.getLong("observedAt")) }, VisitJourney.parse(j.optJSONObject("journey")), VisitObservationJournal.parse(j.optJSONObject("observation")), VisitParkingJournal.parse(j.optJSONObject("parked")))
    }
}
