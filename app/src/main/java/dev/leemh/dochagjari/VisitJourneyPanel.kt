package dev.leemh.dochagjari

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun VisitJourneyPanel(state: AppState, model: AppModel) {
    val visit = state.guidance.visit ?: return
    val road = visit.journey.latest
    val target = road?.target ?: state.roadTarget ?: return
    val stopped = state.vehicle.stopped == true
    val available = stopped && !state.guidance.saving && !state.roadStarting
    Text("시설 목적지 · ${target.name}")
    Text(target.address, fontSize = 15.sp)
    Text("공식 시설 대표 좌표 ${target.latitude}, ${target.longitude} · 차량 입구 좌표 아님", fontSize = 14.sp)
    Text("자료 ${target.checkedAt} · ${target.sourceUrl}", fontSize = 12.sp)
    Text("차량 진입 게이트·주차층/기둥·승강기 연결은 미확인입니다.", fontSize = 14.sp)
    val canRequest = visit.journey.entry == null && visit.phase in setOf(VisitPhase.PLANNED, VisitPhase.ROAD_GUIDANCE) &&
        (road == null || road.status in setOf(RoadStatus.PREPARED, RoadStatus.FAILED, RoadStatus.INTERRUPTED, RoadStatus.CANCELLED))
    Button(onClick = model::requestVisitRoad, enabled = available && canRequest) { Text("시설 목적지로 내비 요청") }
    Text("다음 요청 방식 · ${if (state.testNavi) "모의 요청" else "Pleos SDK"}. 요청 결과와 실제 목적지 핀·입차는 별도 확인합니다.", fontSize = 14.sp)
    road?.let { a ->
        Text("내비 요청 ID · ${a.id}", fontSize = 12.sp)
        Text("저장된 요청 방식 · ${if (a.channel == RoadChannel.MOCK) "모의 내비" else "Pleos SDK"}", fontSize = 13.sp)
        Text(when (a.status) {
            RoadStatus.PREPARED -> "내비 요청 준비 저장됨"
            RoadStatus.DISPATCHED -> "내비 전달 구간 기록 · 종료되었다면 외부 실행 여부 미확인"
            RoadStatus.MOCK_COMPLETED -> "모의 내비 계산 완료 · 실제 SDK/핀 미검증"
            RoadStatus.SDK_CALL_RETURNED -> "SDK 요청 함수 반환 · 콜백/실제 핀 미검증"
            RoadStatus.SDK_EVENT_RECEIVED -> "SDK 이벤트 수신 · 요청 일치/실제 핀 미검증"
            RoadStatus.FAILED -> "SDK 함수 예외 · 전달 결과/입차 미확인"
            RoadStatus.INTERRUPTED -> "앱 내비 연결 중단 · 순정 내비 상태 미확인"
            RoadStatus.CANCELLED -> "앱 요청 취소됨 · 순정 내비 중지 아님"
        })
        a.events.takeLast(3).forEach { Text(it.detail, fontSize = 13.sp) }
        Text("앱 재시작만으로 내비를 자동 재요청하지 않습니다.", fontSize = 13.sp)
        if (visit.phase == VisitPhase.ROAD_GUIDANCE) OutlinedButton(model::cancelVisitRoad, enabled = available) { Text("앱 내비 연결 취소") }
    }
    if (visit.journey.entry == null) {
        Text("주차장에 실제로 들어간 뒤 직접 확인하거나, 개발 중에는 입차 테스트를 사용하세요.", fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ model.confirmVisitEntry(false) }, enabled = available && visit.phase == VisitPhase.ROAD_GUIDANCE) { Text("직접 입차 확인 · 센서 감지 아님") }
            OutlinedButton({ model.confirmVisitEntry(true) }, enabled = available && state.testVehicle && visit.phase == VisitPhase.ROAD_GUIDANCE) { Text("입차 테스트 · 실제 감지 아님") }
        }
    } else {
        Text("입차 기록 · ${visit.journey.entry.source} · 같은 방문 유지")
        Text("입차 기록은 주차 완료·카메라 측위가 아닙니다.", fontSize = 14.sp)
        if (!stopped) Text("주행 중 또는 상태 미확인 · 상세 지도를 숨깁니다.")
    }
    if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
}
