package dev.leemh.dochagjari

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class VisitFrameSource(val label: String) { TEST("테스트 관측"), GENERATED_VIDEO("생성 영상 분석"), SELECTED_VIDEO("선택 영상 분석"), PUBLIC_VIDEO("공개 영상 분석") }
data class VisitSample(val id: String, val rawText: String, val textConfidence: Double?, val analyzedAt: Long,
    val frameTimeMs: Long, val occupancy: Occupancy = Occupancy.UNKNOWN, val occupancyConfidence: Double = 0.0, val textTruncated: Boolean = false)
data class VisitScan(val id: String, val source: VisitFrameSource, val startedAt: Long, val samples: List<VisitSample> = emptyList(), val stopReason: String? = null)
data class VisitObservationJournal(val runs: List<VisitScan> = emptyList()) {
    val latest get() = runs.lastOrNull()
    fun json() = JSONObject().put("runs", JSONArray().apply { runs.forEach { r -> put(JSONObject().apply {
        put("id", r.id); put("source", r.source.name); put("startedAt", r.startedAt); put("stopReason", r.stopReason ?: JSONObject.NULL)
        put("samples", JSONArray().apply { r.samples.forEach { s -> put(JSONObject().apply {
            put("id", s.id); put("rawText", s.rawText); put("textConfidence", s.textConfidence ?: JSONObject.NULL)
            put("analyzedAt", s.analyzedAt); put("frameTimeMs", s.frameTimeMs); put("occupancy", s.occupancy.name)
            put("occupancyConfidence", s.occupancyConfidence); put("textTruncated", s.textTruncated)
        }) } })
    }) } })
    companion object {
        fun parse(j: JSONObject?): VisitObservationJournal {
            if (j == null) return VisitObservationJournal()
            val a = j.getJSONArray("runs")
            return VisitObservationJournal((0 until a.length()).map { i -> val r = a.getJSONObject(i); val samples = r.getJSONArray("samples")
                VisitScan(r.getString("id"), VisitFrameSource.valueOf(r.getString("source")), r.getLong("startedAt"),
                    (0 until samples.length()).map { k -> val s = samples.getJSONObject(k)
                        VisitSample(s.getString("id"), s.getString("rawText"), if (s.isNull("textConfidence")) null else s.getDouble("textConfidence"),
                            s.getLong("analyzedAt"), s.getLong("frameTimeMs"), Occupancy.valueOf(s.getString("occupancy")), s.getDouble("occupancyConfidence"), s.optBoolean("textTruncated"))
                    }, if (r.isNull("stopReason")) null else r.getString("stopReason"))
            })
        }
    }
}
data class SignCandidate(val floor: String?, val pillar: String)
data class VisitObservationSummary(val status: String, val candidate: SignCandidate?, val recommendation: String)

object VisitObservationRules {
    fun begin(v: VisitRecord, source: VisitFrameSource): VisitRecord {
        require(v.phase == VisitPhase.PARKING_GUIDANCE && v.journey.entry != null)
        val runs = v.observation.runs.map { if (it.stopReason == null) it.copy(stopReason = "입력 소스 변경/명시적 새 관측") else it }
        return v.copy(observation = VisitObservationJournal((runs + VisitScan(UUID.randomUUID().toString(), source, System.currentTimeMillis())).takeLast(8)))
    }
    fun append(v: VisitRecord, runId: String, s: VisitSample): VisitRecord {
        val r = v.observation.latest ?: return v
        if (r.id != runId || r.stopReason != null || v.phase != VisitPhase.PARKING_GUIDANCE) return v
        require(s.id.isNotBlank() && s.rawText.length <= 1000 && s.frameTimeMs >= 0)
        require(s.textConfidence == null || s.textConfidence.isFinite() && s.textConfidence in 0.0..1.0)
        require(s.occupancyConfidence.isFinite() && s.occupancyConfidence in 0.0..1.0)
        if (r.samples.any { it.id == s.id }) return v
        return replace(v, r.copy(samples = (r.samples + s).takeLast(24)))
    }
    fun stop(v: VisitRecord, runId: String, reason: String): VisitRecord {
        val r = v.observation.latest ?: return v
        return if (r.id == runId && r.stopReason == null) replace(v, r.copy(stopReason = reason)) else v
    }
    fun parseSign(raw: String): SignCandidate? {
        val text = raw.uppercase()
        val floor = Regex("\\bFLOOR\\s+(B[0-9]{1,2})\\b").find(text)?.groupValues?.get(1)
            ?: Regex("\\b(B[0-9]{1,2})F\\b").find(text)?.groupValues?.get(1)
            ?: Regex("지하\\s*([0-9]{1,2})\\s*층").find(text)?.groupValues?.get(1)?.let { "B$it" }
        val pillars = Regex("\\b[A-Z][ -]?[0-9]{1,3}\\b").findAll(text).map { it.value.replace(" ", "").replace("-", "") }.filter { it != floor }.distinct().toList()
        return if (pillars.size == 1) SignCandidate(floor, pillars.single()) else null
    }
    fun summarize(journal: VisitObservationJournal, now: Long): VisitObservationSummary {
        val r = journal.latest ?: return VisitObservationSummary("관측 기록 없음", null, "주차층·위치·승강기 연결 미확인 · 기존 5층 동선 유지")
        val fresh = r.samples.filter { now - it.analyzedAt in 0..30_000 }
        val valid = fresh.filter { it.textConfidence == null || it.textConfidence >= .65 }
        val parsed = valid.mapNotNull { s -> parseSign(s.rawText)?.let { s to it } }
        val conflict = parsed.groupBy { it.second.pillar }.values.any { group -> group.mapNotNull { it.second.floor }.distinct().size > 1 }
        if (conflict) return VisitObservationSummary("층별 같은 번호/상충 관측 · 후보 보류", null, "서로 다른 층 단서가 겹칩니다. 현재 위치·엘리베이터를 확정하지 않습니다.")
        val lastTwo = r.samples.takeLast(2)
        val signs = lastTwo.map { parseSign(it.rawText) }
        val stable = if (lastTwo.size == 2 && lastTwo.all { now - it.analyzedAt in 0..30_000 && (it.textConfidence == null || it.textConfidence >= .65) } && signs[0] != null && signs[0] == signs[1]) signs[0] else null
        val status = when {
            r.samples.isEmpty() -> "프레임 대기"
            now - r.samples.last().analyzedAt !in 0..30_000 -> "오래됨/시각 미확인 · 후보 보류"
            fresh.isEmpty() -> "오래됨/시각 미확인 · 후보 보류"
            r.samples.last().textConfidence?.let { it < .65 } == true -> "낮은 신뢰도 · 후보 보류"
            valid.isEmpty() -> "낮은 신뢰도 · 후보 보류"
            stable != null -> "반복 표지 후보 · 위치 확정 아님"
            parseSign(valid.last().rawText) == null -> "표기 없음/여러 표기 · 후보 보류"
            else -> "단일/변경 관측 · 반복 확인 필요"
        }
        val clue = stable?.let { "${it.floor ?: "층 미확인"} / ${it.pillar} 문자 후보 · " }.orEmpty()
        val obstacle = if (fresh.takeLast(2).count { it.occupancy == Occupancy.OCCUPIED && it.occupancyConfidence >= .65 } >= 2) "영상 장애물 반복 관측 · 통행/정차 허용은 별도 확인. " else ""
        return VisitObservationSummary(status, stable, obstacle + clue + "실제 판교점 기둥/현재 위치·승강기 연결 미확인 · 기존 5층 동선 유지")
    }
    private fun replace(v: VisitRecord, r: VisitScan) = v.copy(observation = VisitObservationJournal(v.observation.runs.dropLast(1) + r))
}
