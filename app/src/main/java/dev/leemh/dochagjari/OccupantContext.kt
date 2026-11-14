package dev.leemh.dochagjari

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class OccupantMode(val label: String) { SOLO("혼자"), CHILD("아이 동반"), FAMILY("가족"), SUPPORT("동행"), CUSTOM("직접 설정") }
enum class OccupantInput(val label: String) { PRESET("모드 선택"), TEXT("텍스트 입력"), SPEECH("Android 음성 입력"), USER("직접 설정") }

data class MobilityNeeds(val leftDoor: Boolean = false, val rightDoor: Boolean = false,
    val stroller: Boolean = false, val wheelchair: Boolean = false, val companion: Boolean = false) {
    fun applyTo(base: Profile) = base.copy(leftDoor = leftDoor, rightDoor = rightDoor, stroller = stroller,
        wheelchair = wheelchair, companion = companion)
    companion object { fun from(p: Profile) = MobilityNeeds(p.leftDoor, p.rightDoor, p.stroller, p.wheelchair, p.companion) }
}
data class OccupantPreset(val id: String, val name: String, val mode: OccupantMode,
    val seats: SeatPreferences = SeatPreferences(SeatSetup.CONFIGURED), val needs: MobilityNeeds = MobilityNeeds())
data class ActiveOccupants(val id: String, val presetId: String?, val name: String, val mode: OccupantMode,
    val seats: SeatPreferences, val needs: MobilityNeeds, val source: OccupantInput, val confirmedAt: Long)
data class OccupantBook(val presets: List<OccupantPreset>, val active: ActiveOccupants? = null, val skipped: Boolean = false) {
    fun validate(): OccupantBook {
        require(presets.size in 1..20 && presets.map { it.id }.distinct().size == presets.size)
        presets.forEach { require(it.id.isNotBlank() && it.id.length <= 80 && it.name.isNotBlank() && it.name.length <= 40) }
        active?.let { require(it.id.isNotBlank() && it.name.isNotBlank() && it.name.length <= 40 && it.confirmedAt > 0) }
        return this
    }
    fun select(p: OccupantPreset, source: OccupantInput, now: Long) = copy(skipped = false,
        active = ActiveOccupants(active?.id ?: UUID.randomUUID().toString(), p.id, p.name, p.mode, p.seats, p.needs, source, now))
    companion object {
        fun migrate(old: Profile): OccupantBook {
            val defaults = listOf(
                OccupantPreset("solo", "혼자", OccupantMode.SOLO),
                OccupantPreset("child", "아이 등하원", OccupantMode.CHILD,
                    SeatPreferences(SeatSetup.CONFIGURED, right = SeatAssignment(SeatRole.CHILD, true))),
                OccupantPreset("family", "가족 나들이", OccupantMode.FAMILY,
                    SeatPreferences(SeatSetup.CONFIGURED, SeatAssignment(SeatRole.ADULT), SeatAssignment(SeatRole.CHILD, true))),
                OccupantPreset("support", "부모님 동행", OccupantMode.SUPPORT,
                    SeatPreferences(SeatSetup.CONFIGURED, right = SeatAssignment(SeatRole.ADULT)), MobilityNeeds(companion = true)))
            val hasOld = old.seats.setup != SeatSetup.NOT_SET || MobilityNeeds.from(old) != MobilityNeeds()
            return OccupantBook(if (hasOld) listOf(OccupantPreset("legacy", "이전 저장 구성", OccupantMode.CUSTOM,
                old.seats, MobilityNeeds.from(old))) + defaults else defaults, skipped = old.seats.setup == SeatSetup.SKIPPED)
        }
    }
}

/** Only point observations, never identity/age inference or an automatic preset switch. */
object OccupantSuggestions {
    fun describe(left: Occupancy, right: Occupancy): String = when {
        left == Occupancy.UNKNOWN || right == Occupancy.UNKNOWN -> "좌석 신호 미확인 · 이번 구성을 직접 알려주세요"
        left == Occupancy.EMPTY && right == Occupancy.EMPTY -> "뒷좌석은 비어 있어요 · 앞좌석과 탑승 역할은 직접 확인해요"
        else -> "좌석 점유가 있어요 · 저장한 구성 중 이번 탑승을 선택해 주세요"
    }
}

data class OccupantCommand(val candidates: List<OccupantPreset>, val message: String)
object OccupantCommands {
    fun parse(text: String, book: OccupantBook): OccupantCommand {
        val t = text.trim().replace(" ", "")
        if (t.isEmpty() || t.length > 160) return OccupantCommand(emptyList(), "탑승 구성을 짧게 입력해 주세요")
        if (("혼자" in t && ("아이" in t || "가족" in t || "부모" in t)) ||
            (listOf("안탔", "안타", "없어", "아니", "취소").any { it in t } && "혼자" !in t))
            return OccupantCommand(emptyList(), "이번 탑승 모드를 직접 선택하거나 더 구체적으로 알려주세요")
        val exact = book.presets.filter { t.contains(it.name.replace(" ", "")) }
        if (exact.isNotEmpty()) return OccupantCommand(exact, "이번 탑승에 적용할 구성을 확인해 주세요")
        val mode = when {
            "혼자" in t || "나홀로" in t -> OccupantMode.SOLO
            "아이" in t || "아기" in t || "등하원" in t -> OccupantMode.CHILD
            "가족" in t -> OccupantMode.FAMILY
            "부모" in t || "동행" in t -> OccupantMode.SUPPORT
            else -> null
        }
        val matched = book.presets.filter { it.mode == mode }
        if (mode == OccupantMode.CHILD && ("왼쪽" in t || "오른쪽" in t)) {
            val left = "왼쪽" in t
            if (left && "오른쪽" in t) return OccupantCommand(emptyList(), "양쪽 좌석은 직접 구성에서 각각 확인해 주세요")
            val previous = book.active?.seats ?: SeatPreferences(SeatSetup.CONFIGURED)
            val seats = if (left) previous.copy(setup = SeatSetup.CONFIGURED, left = SeatAssignment(SeatRole.CHILD))
                else previous.copy(setup = SeatSetup.CONFIGURED, right = SeatAssignment(SeatRole.CHILD))
            return OccupantCommand(listOf(OccupantPreset("command-once", "${if (left) "왼쪽" else "오른쪽"} 뒤 아이 동반",
                OccupantMode.CHILD, seats, book.active?.needs ?: MobilityNeeds())), "명시한 좌석만 바꿔 이번 탑승에 적용해요")
        }
        return OccupantCommand(matched, if (matched.isEmpty()) "저장한 모드나 좌석을 직접 선택해 주세요" else "이번 탑승에 적용할 구성을 확인해 주세요")
    }
}

object OccupantCodec {
    private fun seat(a: SeatAssignment) = JSONObject().put("role", a.role.name).put("childSeat", a.childSeat)
    private fun seats(s: SeatPreferences) = JSONObject().put("setup", s.setup.name).put("left", seat(s.left)).put("right", seat(s.right))
    private fun needs(n: MobilityNeeds) = JSONObject().put("leftDoor", n.leftDoor).put("rightDoor", n.rightDoor)
        .put("stroller", n.stroller).put("wheelchair", n.wheelchair).put("companion", n.companion)
    private fun preset(p: OccupantPreset) = JSONObject().put("id", p.id).put("name", p.name).put("mode", p.mode.name)
        .put("seats", seats(p.seats)).put("needs", needs(p.needs))
    fun encode(book: OccupantBook): String {
        book.validate()
        val o = JSONObject().put("schema", 1).put("presets", JSONArray(book.presets.map(::preset))).put("skipped", book.skipped)
        book.active?.let { a -> o.put("active", preset(OccupantPreset(a.id, a.name, a.mode, a.seats, a.needs))
            .put("presetId", a.presetId ?: JSONObject.NULL).put("source", a.source.name).put("confirmedAt", a.confirmedAt)) }
        return o.toString()
    }
    private inline fun <reified T : Enum<T>> enum(raw: String): T = enumValues<T>().firstOrNull { it.name == raw }
        ?: throw IllegalArgumentException("Unsupported occupant value")
    private fun readSeats(o: JSONObject): SeatPreferences {
        fun read(s: JSONObject) = SeatAssignment(enum<SeatRole>(s.getString("role")), s.getBoolean("childSeat"))
        return SeatPreferences(enum(o.getString("setup")), read(o.getJSONObject("left")), read(o.getJSONObject("right")))
    }
    private fun readNeeds(o: JSONObject) = MobilityNeeds(o.getBoolean("leftDoor"), o.getBoolean("rightDoor"),
        o.getBoolean("stroller"), o.getBoolean("wheelchair"), o.getBoolean("companion"))
    private fun readPreset(o: JSONObject) = OccupantPreset(o.getString("id"), o.getString("name"),
        enum(o.getString("mode")), readSeats(o.getJSONObject("seats")), readNeeds(o.getJSONObject("needs")))
    fun decode(raw: String): OccupantBook {
        val o = JSONObject(raw); require(o.getInt("schema") == 1) { "Unsupported occupant schema" }
        val presets = o.getJSONArray("presets")
        val a = o.optJSONObject("active")?.let { v -> val p = readPreset(v)
            ActiveOccupants(p.id, if (v.isNull("presetId")) null else v.getString("presetId"), p.name, p.mode, p.seats, p.needs,
                enum(v.getString("source")), v.getLong("confirmedAt")) }
        return OccupantBook((0 until presets.length()).map { readPreset(presets.getJSONObject(it)) }, a, o.getBoolean("skipped")).validate()
    }
}
