package dev.leemh.dochagjari

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable fun ArrivalGuidePanel(state: AppState, model: AppModel) {
    val d = state.guidance.visitDestination ?: return
    val visit = state.guidance.visit ?: return
    val entered = visit.journey.entry != null
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
                Column(Modifier.safeDrawingPadding().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text(if (tab == ArrivalGuideTab.PARKING) "주차장 안내" else "5F  에뜨와", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("현대백화점 판교점 · ${state.vehicle.source.label}", color = CabinMuted, fontSize = 13.sp) }
                        OutlinedButton(model::closeIndoorGuide) { Text("닫기") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(tab == ArrivalGuideTab.PARKING, { tab = ArrivalGuideTab.PARKING }, { Text("주차 지도") })
                        FilterChip(tab == ArrivalGuideTab.FLOOR, { tab = ArrivalGuideTab.FLOOR }, { Text("5층 동선") })
                        Text("현재 위치 미확인", color = CabinMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                    }
                    val candidate = summary.candidate?.let { listOfNotNull(it.floor, "${it.pillar} 후보").joinToString(" · ") }
                    SampleCameraPreview(if (preview && entered) asset else null,
                        if (candidate != null) "${visit.observation.latest?.source?.label} · $candidate" else "층·기둥 관측 확인 전",
                        Modifier.fillMaxWidth().weight(.28f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (entered) {
                            OutlinedButton({ preview = !preview; if (!preview) model.stopVisitObservation("미리보기 중지") }) { Text(if (preview) "영상 중지" else "생성 영상 보기") }
                            OutlinedButton({ preview = true; model.startVisitVideo(asset = asset) }, enabled = !state.observingVisit) { Text("영상 분석") }
                            TextButton({ model.stopVisitObservation("입력 변경"); asset = if (asset == "generated-blocked.mp4") "generated-empty.mp4" else "generated-blocked.mp4"; preview = false }) { Text(if (asset == "generated-blocked.mp4") "빈 영상 선택" else "C12 영상 선택") }
                        } else Text("입차를 확인하면 사전 영상 관측을 시작할 수 있어요.", color = CabinMuted, fontSize = 14.sp)
                    }
                    Box(Modifier.fillMaxWidth().weight(.52f)) {
                        if (tab == ArrivalGuideTab.FLOOR && supported) {
                            FloorCanvas(d, progress, Modifier.fillMaxSize().graphicsLayer {
                                rotationX = 26f; rotationZ = -8f; scaleX = .88f; scaleY = .88f; cameraDistance = 12 * density
                            }.testTag("product-map"))
                        } else ArrivalMapCanvas(ArrivalScene.PARKING, Modifier.fillMaxSize().graphicsLayer {
                            rotationX = 28f; rotationZ = -8f; scaleX = .9f; scaleY = .9f; cameraDistance = 12 * density
                        })
                    }
                    if (tab == ArrivalGuideTab.FLOOR) {
                        Text("엘리베이터에서\n에뜨와까지", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("공식 동선 기반 개략도 · 파란 선은 경로 재생, 측위 아님", color = CabinMuted, fontSize = 13.sp)
                        if (!reduced && supported) OutlinedButton({ if (progress >= 1f) progress = 0f; playing = !playing }) { Text(if (playing) "일시정지" else "동선 재생") }
                        if (reduced) Text("모션 줄이기 · 전체 경로 표시", color = CabinMuted, fontSize = 13.sp)
                    } else {
                        Text("지도는 배치 예시 · 승강 연결 미확인", color = CabinMuted, fontSize = 14.sp)
                        if (entered) Text(summary.status + " · " + summary.recommendation, color = CabinMuted, fontSize = 13.sp)
                    }
                    if (entered) Button({ model.confirmParking(state.testVehicle) }, enabled = !state.parkingSaving && !state.guidance.saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (state.testVehicle) "주차 완료 · TEST" else "주차 완료 확인") }
                }
            }
        }
    }
}
