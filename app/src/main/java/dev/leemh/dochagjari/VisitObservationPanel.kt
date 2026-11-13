package dev.leemh.dochagjari

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun VisitObservationPanel(state: AppState, model: AppModel, chooseVideo: () -> Unit) {
    val visit = state.guidance.visit ?: return
    if (visit.journey.entry == null) return
    if (state.vehicle.stopped != true) {
        Text("주행 중/상태 미확인 · 방문 관측 중지 · 영상/단서 상세 숨김")
        return
    }
    val run = visit.observation.latest
    val summary = VisitObservationRules.summarize(visit.observation, System.currentTimeMillis())
    val enabled = state.vehicle.stopped == true && !state.analyzing && !state.guidance.saving
    Text("방문 관측 · 출처와 단서", fontSize = 22.sp)
    Text("실제 외부 카메라 미연동 · 선택/생성/공개 영상과 테스트 입력", fontSize = 14.sp)
    Text(state.visitObservationMessage)
    Text("저장 관측 출처: ${run?.source?.label ?: "없음"} · 저장 프레임 ${run?.samples?.size ?: 0}")
    Text("관측 판단: ${summary.status}", color = MaterialTheme.colorScheme.primary)
    Text(summary.recommendation, fontSize = 15.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(model::startVisitTestObservation, enabled = enabled && state.testVehicle) { Text("테스트 관측 시작") }
        OutlinedButton({ model.startVisitVideo(asset = "generated-blocked.mp4") }, enabled = enabled) { Text("생성 C12 방문 관측") }
        OutlinedButton(chooseVideo, enabled = enabled) { Text("방문 영상 선택") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ model.startVisitVideo(asset = "generated-empty.mp4") }, enabled = enabled) { Text("생성 빈 방문 관측") }
        OutlinedButton({ model.startVisitVideo(asset = "public-obstacles.mp4") }, enabled = enabled) { Text("공개 영상 방문 관측") }
        OutlinedButton({ model.stopVisitObservation() }, enabled = state.observingVisit) { Text("방문 관측 중지") }
    }
    val test = enabled && state.observingVisit && run?.source == VisitFrameSource.TEST && run.stopReason == null
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ model.feedVisitTestObservation("B3") }, enabled = test) { Text("테스트 B3/C12 반복") }
        OutlinedButton({ model.feedVisitTestObservation("B4") }, enabled = test) { Text("테스트 B4/C12 반복") }
        OutlinedButton({ model.feedVisitTestObservation("NONE") }, enabled = test) { Text("테스트 표기 없음") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ model.feedVisitTestObservation("LOW") }, enabled = test) { Text("테스트 낮은 신뢰도") }
        OutlinedButton({ model.feedVisitTestObservation("STALE") }, enabled = test) { Text("테스트 오래됨") }
        OutlinedButton({ model.stopVisitObservation("입력 권한 철회 테스트") }, enabled = state.observingVisit) { Text("입력 권한 철회 테스트") }
    }
    run?.let {
        Text("관측 ID · ${it.id} · ${it.stopReason ?: "시작 기록 · 앱 재시작 후 자동 재개 없음"}", fontSize = 12.sp)
        it.samples.takeLast(3).forEach { sample ->
            Text("원문: ${sample.rawText.ifBlank { "표기 없음" }}${if (sample.textTruncated) " (일부 보관)" else ""}", fontSize = 14.sp)
            Text("분석시각 ${sample.analyzedAt} · 영상 ${sample.frameTimeMs}ms · 문자점수 ${sample.textConfidence?.toString() ?: "미제공"} · 점유 ${sample.occupancy}", fontSize = 12.sp)
        }
    }
    Text("분석 시각은 영상 촬영 시각/현재 차량 위치가 아닙니다. 반복 후보도 실제 기둥·층·승강 연결을 확정하지 않습니다.", fontSize = 13.sp)
}
