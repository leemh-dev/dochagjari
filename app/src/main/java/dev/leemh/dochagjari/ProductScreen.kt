package dev.leemh.dochagjari

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Paper = CabinInk
private val Blue = Color(0xFF246EFF)
private val ProductColors = darkColorScheme(primary = CabinBlue, onPrimary = Color(0xFF071426),
    background = Paper, surface = CabinPanel, secondary = Blue, onSurface = Color.White)

/** Product UI shares the existing model and persistence; developer tools remain a separate view. */
@Composable fun ProductApp(model: AppModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var developer by rememberSaveable { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var editingParking by remember { mutableStateOf(false) }
    var scenario by remember { mutableStateOf<ArrivalScene?>(null) }
    LaunchedEffect(state.vehicle.stopped) { if (state.vehicle.stopped != true) { editingParking = false; settings = false; scenario = null } }
    var initialTab by rememberSaveable { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { scenario = null; settings = false; model.closeIndoorGuide() } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); model.closeIndoorGuide() }
    }
    fun tools(tab: Int) { model.closeIndoorGuide(); initialTab = tab; developer = true }
    if (developer) {
        Column(Modifier.fillMaxSize().background(Paper).safeDrawingPadding()) {
            TextButton({ model.closeIndoorGuide(); developer = false }) { Text("안내 화면으로 돌아가기") }
            Box(Modifier.weight(1f)) { ArrivalApp(model, initialTab) }
        }
        return
    }
    MaterialTheme(colorScheme = ProductColors, typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 22.sp), bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 18.sp))) {
        val stopped = state.vehicle.stopped == true
        val v = state.guidance.visit
        Surface(Modifier.fillMaxSize().testTag("product-home"), color = Paper) {
            Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("도착자리", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("초행길의 마지막 안내", color = CabinMuted, fontSize = 15.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${if (stopped) "정차" else if (state.vehicle.stopped == false) "주행 중" else "상태 미확인"} · ${state.vehicle.source.label}", fontSize = 14.sp)
                        TextButton({ settings = true }, enabled = stopped) { Text("탑승 설정") }
                        TextButton({ tools(0) }, enabled = stopped) { Text("개발자 도구") }
                    }
                }
                if (!stopped) {
                    ProductCard(Modifier.fillMaxWidth().weight(1f)) {
                        Spacer(Modifier.weight(1f)); Text("안전하게 이동하세요", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Text("정차 후 매장 동선과 주차 기록을 확인할 수 있어요.")
                        Text("상세 안내는 자동으로 다시 열리지 않습니다.", fontSize = 16.sp); Spacer(Modifier.weight(1f))
                    }
                } else BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 760.dp
                    @Composable fun visual(modifier: Modifier) {
                        ProductCard(modifier) {
                            Text("HYUNDAI DEPARTMENT STORE", fontSize = 14.sp, color = CabinMuted)
                            Text(if (v?.parked?.latest != null) "주차 기록이 준비됐어요" else "현대백화점 판교점 · 에뜨와", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Box(Modifier.fillMaxWidth().weight(1f)) {
                                ArrivalMapCanvas(ArrivalScene.PARKING, Modifier.fillMaxSize().graphicsLayer { rotationX = 24f; rotationZ = -6f; scaleX = .9f; scaleY = .9f })
                            }
                            Text("주차 지도 → 5층 매장 동선", color = CabinBlue, fontSize = 20.sp)
                            Text("주차장 연결·현재 위치 미확인", fontSize = 15.sp, color = CabinMuted)
                        }
                    }
                    @Composable fun actions(modifier: Modifier) {
                        ProductCard(modifier.verticalScroll(rememberScrollState())) {
                            val entered = v?.journey?.entry != null
                            val parked = v?.parked?.active != null
                            Text(when { parked -> "주차 기록이 있어요"; entered -> "매장까지 이어서"; v?.phase == VisitPhase.ROAD_GUIDANCE -> "도로 안내는 순정 내비에서"; else -> "에뜨와로 갈까요?" }, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("출발 시나리오 · 현대자동차 양재 사옥", fontSize = 15.sp)
                            Text("다음 요청 · " + if (state.testNavi) "모의 내비" else "Pleos SDK", fontSize = 15.sp, color = Blue)
                            v?.journey?.latest?.let { a -> Text(if (a.channel == RoadChannel.MOCK) "저장 요청 · 모의 내비 / 실제 SDK·핀 미검증" else "저장 요청 · Pleos SDK / 실제 핀 별도 확인", fontSize = 15.sp) }
                            if (state.guidance.loading || state.guidance.saving) Text("방문 정보 저장·확인 중…")
                            if (state.guidance.error.isNotBlank()) Text(state.guidance.error, color = MaterialTheme.colorScheme.error)
                            if (v == null) {
                                Text("공식 안내를 바탕으로 5층 동선을 준비했어요.")
                                Button({ model.prepareVisit("hyundai-pangyo-ettoi") }, enabled = !state.guidance.loading && !state.guidance.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("방문 준비") }
                            } else {
                                val road = v.journey.latest
                                val canRequest = !entered && (road == null || road.status in setOf(RoadStatus.PREPARED, RoadStatus.FAILED, RoadStatus.INTERRUPTED, RoadStatus.CANCELLED))
                                if (canRequest) Button(model::requestVisitRoad, enabled = !state.guidance.saving && !state.roadStarting, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("내비 안내 시작") }
                                if (!entered && v.phase == VisitPhase.ROAD_GUIDANCE) {
                                    Text(if (road?.channel == RoadChannel.MOCK) "모의 요청 결과는 개발자 도구에서 확인할 수 있어요." else "SDK 결과와 실제 목적지 핀은 별도 확인이 필요해요.", fontSize = 16.sp)
                                    Text("주차장에 들어온 뒤 정차하고 확인하세요.")
                                    Button({ model.confirmVisitEntry(state.testVehicle) }, enabled = !state.guidance.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(if (state.testVehicle) "입차 확인 · TEST" else "입차 직접 확인") }
                                }
                                if (entered && !parked) OutlinedButton({ model.openArrivalGuide() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("주차 지도 보기") }
                                if (!parked) Button(model::openIndoorGuide, enabled = !state.guidance.saving && !state.parkingSaving, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("5층 동선 보기") }
                                if (entered && !parked) OutlinedButton({ model.confirmParking(state.testVehicle) }, enabled = !state.parkingSaving && !state.guidance.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(if (state.testVehicle) "주차 완료 · TEST" else "주차 완료 확인") }
                                if (v.parked.latest != null) Button(model::openParkingReturn, enabled = !state.parkingSaving, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("차로 돌아가기") }
                            }
                            if (state.parkingSaving) Text("주차 기록 저장·확인 중…")
                            if (state.parkingError.isNotBlank()) { Text(state.parkingError, color = MaterialTheme.colorScheme.error); OutlinedButton(model::retryParkingSave) { Text("저장 다시 시도") } }
                            if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
                            Text("차량 진입점·기둥·승강기 호기는 아직 확인되지 않았어요.", fontSize = 15.sp, color = CabinMuted)
                            if (entered) Text("입차 출처 · ${v!!.journey.entry!!.source}", fontSize = 14.sp)
                        }
                    }
                    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { visual(Modifier.weight(1.8f).fillMaxHeight()); actions(Modifier.weight(1f).fillMaxHeight()) }
                    else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { visual(Modifier.fillMaxWidth().weight(.8f)); actions(Modifier.fillMaxWidth().weight(1f)) }
                }
                if (stopped) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ArrivalScene.HOSPITAL, ArrivalScene.ACADEMY, ArrivalScene.STATION, ArrivalScene.AIRPORT).forEach { scene ->
                        OutlinedButton({ model.closeIndoorGuide(); scenario = scene }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), contentPadding = PaddingValues(4.dp)) { Text(scene.label, fontSize = 15.sp) }
                    }
                }
                Text("시설 대표 목적지로 안내 · 차량용 게이트 좌표 아님", fontSize = 14.sp, color = CabinMuted)
            }
        }
        if (stopped && state.showIndoorGuide) ArrivalGuidePanel(state, model)
        if (stopped && state.parkingView != null) ProductParking(state, model) { model.dismissParkingView(); model.clearParkingDraft(); editingParking = true }
        if (stopped) scenario?.let { ScenarioGuidePanel(it, state, model) { scenario = null } }
        if (stopped && settings) ProductSettings(state, model) { settings = false }
        if (stopped && editingParking) ProductParkingEditor(state, model) { editingParking = false }
    }
}

@Composable private fun ProductCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = CabinPanel) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
    }
}

@Composable private fun ProductParking(state: AppState, model: AppModel, edit: () -> Unit) {
    val record = state.guidance.visit?.parked?.latest ?: return
    val notes = record.notes
    Dialog(model::dismissParkingView, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("product-parking"), color = Paper) {
            Column(Modifier.safeDrawingPadding().padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (state.parkingView == ParkingView.SUMMARY) "주차 기록 저장 완료" else "차로 돌아가기", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    OutlinedButton(model::dismissParkingView, modifier = Modifier.heightIn(min = 64.dp)) { Text("닫기") }
                }
                Text("P", fontSize = 90.sp, color = Blue, fontWeight = FontWeight.Bold)
                if (record.resumed != null) Text("이동 재개 전 기록 · 현재 주차 위치 아님")
                ProductCard(Modifier.fillMaxWidth()) {
                    Text("${notes?.details?.floor ?: "주차층 미확인"}  ·  ${notes?.details?.pillar ?: "기둥 미확인"}", fontSize = 28.sp)
                    Text("승강기 · ${notes?.details?.elevator ?: "미확인"}")
                    if (notes?.details?.memo?.isNotBlank() == true) Text(notes.details.memo)
                    Text("출처 · ${notes?.source ?: record.source} · ${java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.KOREA).format(java.util.Date(notes?.confirmedAt ?: record.completedAt))}", fontSize = 16.sp)
                    Text("저장 단서이며 현재 위치·주차장 복귀 경로는 미확인입니다.", fontSize = 16.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button({ model.dismissParkingView(); model.openIndoorGuide() }, modifier = Modifier.heightIn(min = 64.dp)) { Text("5층 매장행 동선 보기") }
                    OutlinedButton({ model.dismissParkingView(); model.closeIndoorGuide() }, modifier = Modifier.heightIn(min = 64.dp)) { Text("기록 유지하고 닫기") }
                }
                if (record.resumed == null) OutlinedButton(edit, modifier = Modifier.heightIn(min = 64.dp)) { Text("주차 단서 직접 보완") }
            }
        }
    }
}

@Composable private fun ProductParkingEditor(state: AppState, model: AppModel, close: () -> Unit) {
    Dialog(close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Column(Modifier.safeDrawingPadding().padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("직접 확인한 주차 단서", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    OutlinedButton(close) { Text("닫기") }
                }
                Text("새 직접 입력입니다. 모르는 항목은 비워 두세요. 기존 저장 단서는 저장 완료 전까지 유지됩니다.")
                val d = state.parkingDraft
                OutlinedTextField(d.floor.orEmpty(), { model.editParkingDetails(d.copy(floor = it.take(64))) }, label = { Text("주차층") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d.pillar.orEmpty(), { model.editParkingDetails(d.copy(pillar = it.take(64))) }, label = { Text("기둥") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d.elevator.orEmpty(), { model.editParkingDetails(d.copy(elevator = it.take(64))) }, label = { Text("승강기") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d.memo, { model.editParkingDetails(d.copy(memo = it.take(280))) }, label = { Text("메모") }, modifier = Modifier.fillMaxWidth())
                Button(model::saveParkingDetails, enabled = state.parkingDraftDirty, modifier = Modifier.heightIn(min = 64.dp)) { Text("확인한 단서 저장") }
                Text(if (state.parkingSaving) "저장·재확인 중…" else if (state.parkingDraftDirty) "화면 변경 · 아직 저장되지 않았어요" else state.parkingSaveMessage)
                if (state.parkingError.isNotBlank()) { Text(state.parkingError, color = MaterialTheme.colorScheme.error); OutlinedButton(model::retryParkingSave) { Text("저장 다시 시도") } }
                Text("출처 · ${state.parkingDraftSource} / 입력 시각은 저장 완료 시 기록합니다. 현재 위치를 확정하지 않습니다.", fontSize = 15.sp)
            }
        }
    }
}

@Composable private fun ProductSettings(state: AppState, model: AppModel, close: () -> Unit) {
    OccupantSettingsScreen(state, model, close)
}
