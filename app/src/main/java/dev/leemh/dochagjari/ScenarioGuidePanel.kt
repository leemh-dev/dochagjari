package dev.leemh.dochagjari

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Fixture recommendations are explicitly separated from the stored store visit/outbox. */
@Composable fun ScenarioGuidePanel(scene: ArrivalScene, state: AppState, model: AppModel, close: () -> Unit) {
    val facility = state.facilities.firstOrNull { it.id == if (scene == ArrivalScene.HOSPITAL) "hospital" else "academy" }
        ?.takeIf { scene == ArrivalScene.HOSPITAL || scene == ArrivalScene.ACADEMY }
    var mode by remember(scene) { mutableStateOf(if (scene == ArrivalScene.HOSPITAL) VisitMode.DROPOFF else VisitMode.PICKUP) }
    var blocked by remember(scene) { mutableStateOf(emptySet<String>()) }
    val active = state.occupants.active
    val profile = active?.needs?.applyTo(state.profile)?.copy(seats = active.seats) ?: state.profile
    val recommendation = facility?.let { Recommender().recommend(it, mode, profile, blocked, emptyMap()) }
    val density = LocalDensity.current.density
    Dialog(close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("scenario-guide"), color = CabinInk) {
            Column(Modifier.safeDrawingPadding().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(scene.label, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(if (facility != null) "가상 시설 · TEST 추천 / 실제 동선 미확인" else "개념 안내 · 실제 차량 승차점 미확인", color = CabinMuted, fontSize = 14.sp) }
                    OutlinedButton(close) { Text("닫기") }
                }
                SampleCameraPreview(null, "실제 도로·카메라 입력 미연결", Modifier.fillMaxWidth().weight(.24f))
                ArrivalMapCanvas(scene, Modifier.fillMaxWidth().weight(.46f).clipToBounds().graphicsLayer {
                    rotationX = 18f; rotationZ = -6f; scaleX = .94f; scaleY = .94f; cameraDistance = 2000 * density
                })
                Text(if (active != null) "이번 구성 · ${active.name} / 역할은 사용자 확인값" else "탑승 구성 미선택 · 기존 접근성 설정 사용", color = CabinMuted, fontSize = 14.sp)
                if (recommendation != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(mode == VisitMode.PICKUP || mode == VisitMode.DROPOFF,
                            { mode = if (scene == ArrivalScene.HOSPITAL) VisitMode.DROPOFF else VisitMode.PICKUP }, { Text("승하차") })
                        FilterChip(mode == VisitMode.TOGETHER, { mode = VisitMode.TOGETHER }, { Text("함께 주차") })
                    }
                    Text(recommendation.point?.name ?: "조건에 맞는 후보가 없습니다", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (recommendation.point != null) Text(recommendation.reason, color = CabinMuted, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ recommendation.point?.let { blocked = blocked + it.id } }, enabled = recommendation.point != null) { Text("이용 불가 · TEST") }
                        OutlinedButton({ blocked = emptySet() }) { Text("후보 다시 보기") }
                        Button({ recommendation.point?.let(model::previewMockRoute) }, enabled = state.testNavi && recommendation.point != null) { Text("모의 안내") }
                    }
                    Text(if (state.testNavi) state.naviStatus else "가상 시설은 모의 안내에서만 요청할 수 있어요", color = CabinMuted, fontSize = 13.sp)
                } else {
                    Text(if (scene == ArrivalScene.STATION) "사람 출구와 차량 승차점을 구분해요" else "터미널·탑승편을 확인한 뒤 이동해요", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(if (scene == ArrivalScene.STATION) "출구 번호·합법적인 정차 장소·현장 보행 연결을 확인해야 안내를 확정할 수 있어요."
                        else "장기 대기는 주차장 이용을 확인하세요. 터미널·출입구·주차장 연결 자료를 확인하기 전에는 위치 안내를 만들지 않아요.", color = CabinMuted, fontSize = 16.sp)
                }
                Text("지도는 배치 예시 · 현재 위치·정차 허용 구역 아님", color = CabinMuted, fontSize = 13.sp)
            }
        }
    }
}
