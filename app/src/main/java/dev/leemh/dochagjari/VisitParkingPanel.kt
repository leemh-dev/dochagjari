package dev.leemh.dochagjari

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun atLabel(at: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(Date(at)) + " · 기기 시각"
@Composable private fun SavedParking(record: ParkedVisit) {
    Text(if (record.resumed == null) "명시적으로 저장한 주차 기록" else "이동 재개 후 이전 기록 · 현재 주차 위치 아님", fontSize = 22.sp)
    Text("주차 확인: ${record.source} · 차량 상태 출처: ${record.vehicleSource.label}")
    Text("확인 시각 · ${atLabel(record.completedAt)}", fontSize = 13.sp)
    record.notes?.let { n ->
        Text(if (n.source == GuidanceInputSource.TEST) "저장 단서 출처 · TEST · 실제 시설 확인 아님" else "저장 단서 출처 · USER · 사용자가 입력 확인 · 현장/지도 연결 미검증")
        Text("단서 확인 시각 · ${atLabel(n.confirmedAt)}", fontSize = 13.sp)
        Text("저장한 층 · ${n.details.floor ?: "미확인"} / 구역 · ${n.details.zone ?: "미확인"}")
        Text("저장한 기둥 · ${n.details.pillar ?: "미확인"} / 승강기 · ${n.details.elevator ?: "미확인"}")
        Text("메모 · ${n.details.memo.ifBlank { "없음" }}")
    } ?: Text("주차층·구역·기둥·승강기 미확인 · 관측 문자를 자동 확정하지 않았습니다.")
    Text("현재 위치·승강 연결·복귀 경로 미확인", color = MaterialTheme.colorScheme.primary)
    Text("저장 단서는 측위/실제 판교점 검증이 아닙니다. 실제 위치와 현장 안내를 확인하세요.", fontSize = 14.sp)
}
@Composable fun VisitParkingPanel(state: AppState, model: AppModel) {
    val v = state.guidance.visit ?: return
    if (v.journey.entry == null) return
    Text("주차 완료 · 저장 · 귀환", fontSize = 22.sp)
    if (state.vehicle.stopped != true) { Text("주행 중/상태 미확인 · 주차 상세 조작과 단서 숨김"); return }
    Text(state.parkingSaveMessage)
    if (state.parkingError.isNotBlank()) {
        Text(state.parkingError, color = MaterialTheme.colorScheme.error)
        OutlinedButton(model::retryParkingSave, enabled = !state.parkingSaving) { Text("실패한 요청 다시 저장") }
    }
    val enabled = !state.parkingSaving && !state.guidance.saving
    if (v.phase == VisitPhase.PARKING_GUIDANCE && v.parked.active == null) {
        Text("주차 완료는 직접 확인해야 합니다. 정차/입차/문자 인식만으로 완료하지 않습니다.", fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ model.confirmParking(false) }, enabled = enabled) { Text("직접 주차 완료 · 센서 감지 아님") }
            OutlinedButton({ model.confirmParking(true) }, enabled = enabled && state.testVehicle) { Text("주차 완료 테스트") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ model.setParkingSummarySeconds(8) }) { Text("요약 자동 닫기 8초") }
            OutlinedButton({ model.setParkingSummarySeconds(0) }) { Text("요약 자동 닫기 끄기") }
        }
        Text("현재 실행의 요약 설정 · ${if (state.parkingSummarySeconds == 0) "직접 닫기" else "${state.parkingSummarySeconds}초"} · 재시작 후 요약 자동 열림 없음", fontSize = 13.sp)
    }
    v.parked.latest?.let { r ->
        SavedParking(r)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(model::openParkingReturn, enabled = enabled) { Text("차로 돌아가기 · 저장 단서") }
            if (r.resumed == null) OutlinedButton(model::resumeParkingSearch, enabled = enabled) { Text("주차 위치 변경 · 다시 찾기") }
        }
        if (r.resumed == null) {
            Text("직접 확인한 단서 입력 · 빈 값은 미확인으로 저장")
            Text("입력 출처 · ${state.parkingDraftSource} · ${if (state.parkingDraftDirty) "입력 변경 · 미저장" else "새 입력 없음"}")
            val d = state.parkingDraft
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(d.floor.orEmpty(), { model.editParkingDetails(d.copy(floor = it.take(64))) }, label = { Text("확인한 주차층") }, modifier = Modifier.weight(1f).testTag("parking-floor"))
                OutlinedTextField(d.zone.orEmpty(), { model.editParkingDetails(d.copy(zone = it.take(64))) }, label = { Text("확인한 구역") }, modifier = Modifier.weight(1f).testTag("parking-zone"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(d.pillar.orEmpty(), { model.editParkingDetails(d.copy(pillar = it.take(64))) }, label = { Text("확인한 기둥") }, modifier = Modifier.weight(1f).testTag("parking-pillar"))
                OutlinedTextField(d.elevator.orEmpty(), { model.editParkingDetails(d.copy(elevator = it.take(64))) }, label = { Text("확인한 승강기") }, modifier = Modifier.weight(1f).testTag("parking-elevator"))
            }
            OutlinedTextField(d.memo, { model.editParkingDetails(d.copy(memo = it.take(280))) }, label = { Text("직접 확인한 메모") }, modifier = Modifier.fillMaxWidth().testTag("parking-memo"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(model::saveParkingDetails, enabled = state.parkingDraftDirty) { Text("입력한 단서 확인 · 저장") }
                OutlinedButton(model::clearParkingDraft) { Text("비우고 직접 입력") }
                OutlinedButton(model::fillParkingTestDraft, enabled = state.testVehicle) { Text("테스트 주차 단서 채우기") }
            }
            Text("저장 중 바꾼 입력은 저장 완료와 구분합니다. TEST는 비우고 직접 입력하기 전까지 TEST 출처를 유지합니다.", fontSize = 13.sp)
        }
    }
    if (v.parked.latest == null && v.parking != null) Text("이전 형식 단서는 보존되어 있습니다. 새 주차 완료나 현재 위치로 취급하지 않습니다.")
}
@Composable fun ParkingSummaryScreen(state: AppState, model: AppModel, view: ParkingView) {
    val visit = state.guidance.visit ?: return; val r = visit.parked.latest ?: return
    Dialog(onDismissRequest = model::dismissParkingView, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("parking-sheet"), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (view == ParkingView.SUMMARY) "주차 저장 완료 · 요약" else "차로 돌아가기 · 저장 기록", fontSize = 28.sp)
                    OutlinedButton(model::dismissParkingView) { Text(if (view == ParkingView.SUMMARY) "요약 닫기" else "귀환 화면 닫기") }
                }
                Text("${state.guidance.visitDestination?.facilityName.orEmpty()} · ${state.guidance.visitDestination?.name.orEmpty()}")
                SavedParking(r)
                Text("저장 완료는 SQLite 재조회 후 표시합니다. 관측/지도/내비를 자동 재개하지 않습니다.")
                if (view == ParkingView.SUMMARY) Text("요약 자동 닫기 · ${if (state.parkingSummarySeconds == 0) "꺼짐" else "${state.parkingSummarySeconds}초"} · 기록은 유지")
                Text("확인된 자료는 5층 엘리베이터→에뜨와의 매장행 동선입니다. 차로 돌아가는 선이나 주차장 승강 연결을 추정하지 않습니다.")
                Button({ model.dismissParkingView(); model.openIndoorGuide() }, enabled = !state.parkingSaving) { Text("저장된 5층 동선 보기 · 매장행") }
                Text("방문 ID · ${visit.id}\n주차 기록 ID · ${r.id}", fontSize = 12.sp)
            }
        }
    }
}
