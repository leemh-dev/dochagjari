package dev.leemh.dochagjari

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class ArrivalGuideTab { PARKING, FLOOR }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ArrivalGuidePanel(state: AppState, model: AppModel) {
    val d = state.guidance.visitDestination ?: return
    val visit = state.guidance.visit ?: return
    val entered = visit.journey.entry != null
    val observeAllowed = entered && visit.phase == VisitPhase.PARKING_GUIDANCE
    val density = LocalDensity.current.density
    val context = LocalContext.current
    val reduced = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    var tab by remember { mutableStateOf(state.guideTab) }
    var progress by remember { mutableFloatStateOf(if (reduced) 1f else 0f) }
    var playing by remember { mutableStateOf(!reduced) }
    val supported = EttoiFloorPlan.supports(visit.catalogVersion, d)
    var preview by remember { mutableStateOf(false) }
    var asset by remember { mutableStateOf("generated-blocked.mp4") }
    val summary = VisitObservationRules.summarize(visit.observation, System.currentTimeMillis())
    LaunchedEffect(playing, supported, tab) {
        if (playing && supported && tab == ArrivalGuideTab.FLOOR) {
            var previous = withFrameNanos { it }
            while (progress < 1f) {
                val now = withFrameNanos { it }
                progress = (progress + (now - previous).coerceAtMost(200_000_000L) / 6_000_000_000f).coerceAtMost(1f)
                previous = now
            }
            playing = false
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = CabinBlue, background = CabinInk, surface = CabinPanel, onSurface = Color.White)) {
        Dialog(model::closeIndoorGuide, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize().testTag("product-floor"), color = CabinInk) {
                Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text(if (tab == ArrivalGuideTab.PARKING) "주차장 안내" else "5F  에뜨와", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("현대백화점 판교점 · ${state.vehicle.source.label}", color = CabinMuted, fontSize = 13.sp) }
                        OutlinedButton(model::closeIndoorGuide) { Text("닫기") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(tab == ArrivalGuideTab.PARKING, { tab = ArrivalGuideTab.PARKING }, { Text("주차 지도") })
                        FilterChip(tab == ArrivalGuideTab.FLOOR, { tab = ArrivalGuideTab.FLOOR }, { Text("5층 동선") })
                        Text("현재 위치 미확인", color = CabinMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                    }
                    val candidate = summary.candidate?.let { listOfNotNull(it.floor, "${it.pillar} 후보").joinToString(" · ") }
                    SampleCameraPreview(if (preview && observeAllowed) asset else null,
                        if (candidate != null) "저장 관측 · ${visit.observation.latest?.source?.label} · $candidate" else "층·기둥 관측 확인 전",
                        Modifier.fillMaxWidth().weight(1f))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (observeAllowed) {
                            OutlinedButton({ preview = !preview; if (!preview) model.stopVisitObservation("미리보기 중지") }) { Text(if (preview) "영상 중지" else "생성 영상 보기") }
                            OutlinedButton({ preview = true; model.startVisitVideo(asset = asset) }, enabled = !state.observingVisit) { Text("영상 분석") }
                            TextButton({ model.stopVisitObservation("입력 변경"); asset = if (asset == "generated-blocked.mp4") "generated-empty.mp4" else "generated-blocked.mp4"; preview = false }) { Text(if (asset == "generated-blocked.mp4") "빈 영상 선택" else "C12 영상 선택") }
                        } else Text(if (entered) "저장한 주차 기록 안내 · 관측은 중지돼 있어요." else "입차를 확인하면 사전 영상 관측을 시작할 수 있어요.", color = CabinMuted, fontSize = 14.sp)
                    }
                    if (entered) Text(when {
                        state.observingVisit -> "생성 영상을 분석하고 있어요…"
                        state.visitObservationMessage.contains("완료") -> "영상 분석 완료 · 현재 위치 확정 아님"
                        state.visitObservationMessage.contains("실패") || state.visitObservationMessage.contains("권한") -> "영상을 확인할 수 없어요. 다시 선택해 주세요."
                        else -> "분석은 직접 시작하며, 화면을 닫으면 멈춥니다."
                    }, color = CabinMuted, fontSize = 13.sp)
                    Box(Modifier.fillMaxWidth().weight(3.2f).clipToBounds(), contentAlignment = Alignment.Center) {
                        if (tab == ArrivalGuideTab.FLOOR && supported) {
                            FloorCanvas(d, progress, Modifier.fillMaxHeight().aspectRatio(1.14f, matchHeightConstraintsFirst = true).graphicsLayer {
                                rotationX = 18f; rotationZ = -6f; scaleX = .92f; scaleY = .92f; cameraDistance = 2000 * density
                            }.testTag("product-map"))
                        } else ArrivalMapCanvas(ArrivalScene.PARKING, Modifier.fillMaxSize().graphicsLayer {
                            rotationX = 18f; rotationZ = -6f; scaleX = .94f; scaleY = .94f; cameraDistance = 2000 * density
                        })
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (tab == ArrivalGuideTab.FLOOR) {
                            Text("엘리베이터에서\n에뜨와까지", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 160.dp))
                            if (!reduced && supported) OutlinedButton({ if (progress >= 1f) progress = 0f; playing = !playing }) { Text(if (playing) "일시정지" else "동선 재생") }
                            if (reduced) Text("모션 줄이기 · 전체 경로 표시", color = CabinMuted, fontSize = 13.sp)
                        } else Text(summary.status, color = CabinMuted, fontSize = 14.sp)
                        if (observeAllowed) Button({ model.confirmParking(state.testVehicle) }, enabled = !state.parkingSaving && !state.guidance.saving,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text(if (state.testVehicle) "주차 완료 · TEST" else "주차 완료 확인") }
                    }
                    Text(if (tab == ArrivalGuideTab.FLOOR) "공식 동선 기반 개략도 · 파란 선은 경로 재생, 측위 아님" else "주차 지도는 배치 예시 · 층·현재 위치·승강 연결 미확인", color = CabinMuted, fontSize = 13.sp)

                }
            }
        }
    }
}
