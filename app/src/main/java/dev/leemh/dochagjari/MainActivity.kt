package dev.leemh.dochagjari

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ArrivalApp() } }
}
private val Ink = Color(0xFF101C27)
private val Mint = Color(0xFF78E2C5)
private val scheme = darkColorScheme(primary = Mint, background = Color(0xFF09121B), surface = Ink, onPrimary = Color(0xFF072C26), secondary = Color(0xFFB0C9F3))

@Composable fun ArrivalApp(model: AppModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) model.analyze(uri) }
    val carPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.selectVehicle(false) }
    MaterialTheme(colorScheme = scheme, typography = Typography(bodyLarge = TextStyle(fontSize = 20.sp), bodyMedium = TextStyle(fontSize = 18.sp), labelLarge = TextStyle(fontSize = 18.sp))) {
        Surface(Modifier.fillMaxSize(), color = scheme.background) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("도착자리", fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("목적지 너머, 만날 자리까지", color = scheme.secondary) }
                    Column { Text(if (state.vehicle.stopped == true) "● 정차 확인" else if (state.vehicle.stopped == false) "● 주행 중" else "● 주행 상태 미확인", color = Mint); Text(state.vehicle.source.label, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("도착 안내", "좌석·조건", "영상 탐색", "만남·알림", "연동·테스트", "매장 방문").forEachIndexed { index, name ->
                        FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(name) })
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (tab) {
                        5 -> {
                            var query by remember { mutableStateOf("에뜨와 판교 현백") }
                            val guidance = state.guidance
                            Text("매장 방문 · 1단계", color = Mint)
                            Text("출발지 · 현대자동차 양재 사옥")
                            Text("사전 시설 DB 조회 · 실제 차량 출발 위치와 구분합니다.", color = scheme.secondary)
                            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("방문할 매장") }, enabled = state.vehicle.stopped == true)
                            Button(onClick = { model.searchStore(query) }, enabled = state.vehicle.stopped == true && !guidance.loading && !guidance.saving) { Text("저장된 매장 찾기") }
                            if (guidance.loading) Text("시설 DB 불러오는 중")
                            if (guidance.error.isNotBlank()) Text(guidance.error, color = Color(0xFFFFCD81))
                            Text("DB 버전 · ${guidance.catalogVersion.ifBlank { "미준비" }}", fontSize = 12.sp)
                            guidance.matches.forEach { destination ->
                                Panel {
                                    Text("${destination.facilityName} · ${destination.floor} ${destination.name}", fontWeight = FontWeight.Bold)
                                    Text("${destination.checkedAt} 공식 안내 확인 · 5층 보행 연결 ${destination.route.size}개 좌표", fontSize = 13.sp)
                                    Text(destination.sourceUrl, fontSize = 12.sp)
                                    Text("출발 단서 · ${destination.routeStartDescription}")
                                    Text("미확인 · ${destination.unverifiedFields.joinToString(" / ")}", color = scheme.secondary, fontSize = 13.sp)
                                    Button(onClick = { model.prepareVisit(destination.id) }, enabled = state.vehicle.stopped == true && !guidance.saving) { Text("방문 준비 · 저장") }
                                }
                            }
                            if (!guidance.loading && guidance.matches.isEmpty()) Text("조회한 매장이 없으면 이름·지점을 확인해주세요.")
                            if (guidance.saving) Text("방문 저장 중")
                            guidance.visit?.let { visit ->
                                Panel {
                                    Text("저장된 방문 · ${guidance.visitDestination?.name ?: "자료 확인 필요"}", color = Mint)
                                    Text("${guidance.visitDestination?.facilityName.orEmpty()} · ${guidance.visitDestination?.floor.orEmpty()}")
                                    Text("방문 ID · ${visit.id}", fontSize = 12.sp)
                                    Text("저장 상태 · ${visit.phase} / ${visit.visibility}")
                                    Text("사용 자료 · ${visit.catalogVersion}", fontSize = 12.sp)
                                    Text("주차 위치 · ${visit.parking?.pillar ?: "미확인 · 아직 저장하지 않음"}")
                                }
                            }
                            Text("지도 애니메이션·내비 전달·입차/관측 연결은 다음 단계입니다. 이 화면은 조회와 방문 저장/복원을 확인합니다.", color = scheme.secondary)
                        }

                        0 -> {
                            Text("01  어디에서 만날까요?", color = Mint)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.facilities.forEachIndexed { i,f -> FilterChip(state.facilityIndex == i, { model.chooseFacility(i) }, { Text(f.name) }) } }
                            Text("만날 문 · ${state.facility.entrance}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            Text("시설·허용 조건·보행 경로는 가상 시연입니다. 내비 좌표의 실제 승하차 허용 여부는 미검증입니다.", color = scheme.secondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (if (state.facilityIndex == 0) listOf(VisitMode.PICKUP, VisitMode.PARK) else listOf(VisitMode.DROPOFF, VisitMode.TOGETHER)).forEach { mode -> FilterChip(state.mode == mode, { model.mode(mode) }, { Text(mode.label) }) }
                            }
                            Panel {
                                Text("차량 도착점", color = Mint)
                                Text(state.recommendation.point?.name ?: "후보 없음", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                Text(state.recommendation.reason)
                                state.recommendation.point?.let { point ->
                                    Text(point.walking, color = scheme.secondary)
                                    Text("${point.position.latitude}, ${point.position.longitude} · 높이 ${point.heightM}m", fontSize = 13.sp)
                                    state.observations[point.id]?.let { observation -> Text("현재 점유 판단: ${observation.effective(System.currentTimeMillis())} · ${observation.source.label} · 관측 후 ${(System.currentTimeMillis() - observation.observedAt) / 1000}초") }
                                    if (point.stepFree == Evidence.UNKNOWN) Text("무단차 전체 경로 미확인", color = Color(0xFFFFCD81))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = model::navigate, enabled = state.recommendation.point != null) { Text("내비로 안내") }
                                    OutlinedButton(onClick = model::blocked, enabled = state.vehicle.stopped == true && state.recommendation.point != null) { Text("이 지점 이용 불가 · 우회") }
                                    TextButton(onClick = model::resetBlocked) { Text("제외 초기화") }
                                }
                                Text(state.naviStatus)
                            }
                            if (state.mode == VisitMode.DROPOFF && state.recommendation.point?.kind == "dropoff") OutlinedButton(onClick = { model.mode(VisitMode.TOGETHER); model.navigate() }, enabled = state.vehicle.stopped == true) { Text("하차 완료 · 후속 주차장 안내") }
                            state.recommendation.excluded.forEach { Text("제외 · $it", color = scheme.secondary) }
                            Text("실제 병원 자료: 용인세브란스 B1~B3·엘리베이터 공개 안내만 조사됨. 하차 허용·전체 무단차 경로는 미확인.", fontSize = 13.sp)
                        }
                        1 -> {
                            Text("02  좌석과 이동 조건", color = Mint, fontSize = 24.sp)
                            Text("뒷좌석 왼쪽 ${state.vehicle.left}   /   오른쪽 ${state.vehicle.right}", fontSize = 22.sp)
                            Text(state.vehicle.status)
                            if (state.vehicle.left == Occupancy.OCCUPIED || state.vehicle.right == Occupancy.OCCUPIED) Text("탑승 좌석의 저장된 문 여유·유모차 옵션을 확인해 적용하세요.", color = Mint)
                            val enabled = state.vehicle.stopped == true && state.profileSave.status != ProfileSaveStatus.LOADING
                            if (!enabled) Text("정차 상태가 확인되면 설정할 수 있습니다.")
                            Text(state.profileSave.status.label, color = if (state.profileSave.status == ProfileSaveStatus.ERROR) scheme.error else Mint)
                            if (state.profileSave.status == ProfileSaveStatus.ERROR) OutlinedButton(model::retryProfile, enabled = enabled) { Text("설정 다시 저장") }
                            Toggle("왼쪽 카시트 문 여유", state.profile.leftDoor, enabled) { model.profile(state.profile.copy(leftDoor = it)) }
                            Toggle("오른쪽 카시트 문 여유", state.profile.rightDoor, enabled) { model.profile(state.profile.copy(rightDoor = it)) }
                            Toggle("유모차 · 무단차 필요", state.profile.stroller, enabled) { model.profile(state.profile.copy(stroller = it)) }
                            Toggle("휠체어 · 무단차 필요", state.profile.wheelchair, enabled) { model.profile(state.profile.copy(wheelchair = it)) }
                            Toggle("보호자 동행 유지", state.profile.companion, enabled) { model.profile(state.profile.copy(companion = it)) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("차량 높이 ${"%.1f".format(state.profile.heightM)}m · 추가 장비 포함", modifier = Modifier.padding(top = 12.dp))
                                OutlinedButton({ model.profile(state.profile.copy(heightM = (kotlin.math.round(state.profile.heightM * 10 - 1) / 10).coerceAtLeast(1.0))) }, enabled = enabled) { Text("− 0.1m") }
                                OutlinedButton({ model.profile(state.profile.copy(heightM = (kotlin.math.round(state.profile.heightM * 10 + 1) / 10).coerceAtMost(4.0))) }, enabled = enabled) { Text("+ 0.1m") }
                            }
                            Text("설정은 이 단말에 저장됩니다. 좌석 점유로 탑승자의 신원·보조기구를 추론하지 않습니다.")
                        }
                        2 -> {
                            Text("03  등록 후보 구간 영상 분석", color = Mint, fontSize = 24.sp)
                            Text("분석 대상: ${state.recommendation.point?.name ?: "후보 없음"} · 중앙 80%를 후보 영역으로 대응하는 시연입니다.")
                            Text("순정 외부 카메라 미연동 · 사전 영상에서 EfficientDet 객체 탐지와 OCR을 실행합니다. 생성 영상은 등록된 빈 기준영상과도 비교합니다.")
                            val enabled = state.vehicle.stopped == true && !state.analyzing
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button({ picker.launch(arrayOf("video/*")) }, enabled = enabled) { Text("영상 선택") }
                                OutlinedButton({ model.analyze(null, "public-obstacles.mp4", InputSource.PUBLIC_VIDEO) }, enabled = enabled) { Text("공개 장애물 영상") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton({ model.analyze(null, "generated-empty.mp4", InputSource.GENERATED) }, enabled = enabled) { Text("생성 빈 구간") }
                                OutlinedButton({ model.analyze(null, "generated-blocked.mp4", InputSource.GENERATED) }, enabled = enabled) { Text("생성 장애물·C12") }
                            }
                            if (state.vehicle.stopped == true) Panel { Text(state.analysis); if (state.analyzing) LinearProgressIndicator(Modifier.fillMaxWidth()) }
                            else Text("주행 중 또는 주행 상태 미확인 · 영상 결과와 카메라 화면을 숨깁니다.")
                            Text("점유 관측은 30초 후 미확인으로 바뀝니다. 빈 영상은 정차 허용의 근거가 아닙니다.")
                        }
                        3 -> {
                            val enabled = state.vehicle.stopped == true
                            val locked = state.delivery.record.request != null
                            val editable = enabled && !locked && state.session.notificationStatus == DeliveryStatus.DRAFT
                            Text("04  실제 만남 위치 확인", color = Mint, fontSize = 24.sp)
                            Text("세션 ${state.session.id}", fontSize = 12.sp)
                            Text("좌표와 층·구역·기둥을 확인하면 사전 동의 설정에 따라 자동 알림이 한 번 실행됩니다.")
                            Text("알림 대상 ID: ${state.recipientId} · ${if (!state.useServer) DeliveryChannel.LOCAL_MOCK.label else if (state.sensServer) DeliveryChannel.SENS.label else DeliveryChannel.SERVER_MOCK.label}", color = Mint)
                            if (locked) Text("저장된 확정 요청 · 위치/대상/발송 방식 고정", color = Mint)
                            OutlinedTextField(state.clue, { model.editLocation(clue = it) }, label = { Text("만남 단서 · 영상 제안은 직접 확인") }, enabled = editable, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(state.latitude, { model.editLocation(latitude = it) }, label = { Text("실제 정차점 위도") }, enabled = editable, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                                OutlinedTextField(state.longitude, { model.editLocation(longitude = it) }, label = { Text("실제 정차점 경도") }, enabled = editable, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                            }
                            TextButton(model::fillDemoPosition, enabled = editable) { Text("시연용 추천 좌표 채우기 · 실제 위치 아님") }
                            Toggle("테스트 수신자 알림 사전 동의", state.consent, enabled && (!locked || state.consent)) { model.configureNotify(consent = it) }
                            Text(if (state.useServer) "SMS 서버 사용 · 설정된 수신자 ID로 요청" else "모의 발송 · 실제 문자는 전송하지 않습니다", color = Mint)
                            Button(model::confirmLocation, enabled = editable) { Text("여기서 만나요 · 위치 확인") }
                            Text(state.session.notification, fontSize = 20.sp)
                            if (state.delivery.issue != DeliveryIssue.NONE) Text(state.delivery.issue.label, color = scheme.error)
                            if (locked) OutlinedButton(model::recoverNotification, enabled = enabled && !state.delivery.busy) { Text("발송 결과 확인·복구") }
                            if (state.delivery.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (state.error.isNotBlank()) Text(state.error, color = scheme.error)
                            if (state.session.confirmed && enabled) {
                                val text = state.session.shareText()
                                Text(text)
                                ShareQr(text)
                                Text("휴대폰으로 QR을 스캔해 지도 링크를 열고 아래 문구를 카카오톡에 공유하세요. 카카오톡 수신자는 휴대폰에서 직접 선택합니다.")
                                val context = androidx.compose.ui.platform.LocalContext.current
                                OutlinedButton({ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "만남 위치 공유")) }) { Text("이 기기 공유 앱 열기 · 수신 미확인") }
                            }
                            OutlinedButton({ model.newSession() }, enabled = enabled && !state.delivery.busy && state.delivery.storageHealthy && (state.session.notificationStatus == DeliveryStatus.DRAFT || state.session.notificationStatus.terminal)) { Text("새 픽업 세션 시작") }
                        }
                        4 -> {
                            Text("개발·연동 상태", color = Mint, fontSize = 24.sp)
                            Toggle("차량 신호 테스트 입력", state.testVehicle) { if (it) model.selectVehicle(true) else carPermission.launch("android.car.permission.CAR_SPEED") }
                            Toggle("내비 모의 요청", state.testNavi) { model.selectNavi(it) }
                            Text("실제 모드는 공개 API만 사용합니다. 권한 거부·속성 미지원은 그대로 표시합니다.")
                            if (state.testVehicle) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button({ model.testStopped(true) }) { Text("정차") }; OutlinedButton({ model.testStopped(false) }) { Text("주행") }; OutlinedButton({ model.testStopped(null) }) { Text("주행 미확인") }
                                }
                                listOf(true, false).forEach { left -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (left) "뒷좌석 좌" else "뒷좌석 우")
                                    Occupancy.entries.forEach { o -> OutlinedButton({ model.testSeat(left, o) }) { Text(o.name) } }
                                } }
                                Row { TextButton({ model.testUnavailable("권한 없음") }) { Text("권한 없음 재현") }; TextButton({ model.testUnavailable("미지원") }) { Text("미지원 재현") } }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton({ model.testObservation(Occupancy.OCCUPIED) }) { Text("점유 상태 입력") }
                                OutlinedButton({ model.testObservation(Occupancy.OCCUPIED, low = true) }) { Text("저신뢰") }
                                OutlinedButton({ model.testObservation(Occupancy.OCCUPIED, stale = true) }) { Text("만료") }
                            }
                            HorizontalDivider()
                            val enabled = state.vehicle.stopped == true
                            val configure = enabled && state.delivery.record.request == null && state.session.notificationStatus == DeliveryStatus.DRAFT
                            Toggle("알림 서버 사용", state.useServer, configure) { model.configureNotify(useServer = it) }
                            if (state.useServer) Toggle("실제 SENS 서버 모드 · 기본은 서버 모의 발송", state.sensServer, configure) { model.configureNotify(sensServer = it) }
                            OutlinedTextField(state.serverUrl, { model.configureNotify(url = it) }, enabled = configure, label = { Text("서버 URL · 운영 연결은 HTTPS") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(state.token, { model.configureNotify(token = it) }, enabled = enabled, label = { Text("서버 접근 토큰 · 메모리에만 유지") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(state.recipientId, { model.configureNotify(recipient = it) }, enabled = configure, label = { Text("사전 등록 수신자 ID") })
                            Text("설정 변경 전 수신자 동의를 확인하세요. 공급자 수락과 실제 휴대폰 수신은 다릅니다.")
                            state.log.forEach { Text(it, fontSize = 12.sp) }
                        }
                    }
                }
                Text("시연 입력은 실차 검증과 구분됩니다  •  v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = scheme.secondary, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Ink, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}
@Composable private fun Toggle(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, modifier = Modifier.weight(1f).padding(vertical = 12.dp)); Switch(checked, onChange, enabled = enabled) }
}
@Composable private fun ShareQr(text: String) {
    val bitmap = remember(text) {
        val bits = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 320, 320, mapOf(com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8", com.google.zxing.EncodeHintType.MARGIN to 2))
        Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888).also { b -> for (y in 0 until 320) for (x in 0 until 320) b.setPixel(x, y, if (bits[x,y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE) }
    }
    Image(bitmap.asImageBitmap(), "휴대폰으로 스캔할 만남 위치 QR", Modifier.size(200.dp).background(Color.White))
}
