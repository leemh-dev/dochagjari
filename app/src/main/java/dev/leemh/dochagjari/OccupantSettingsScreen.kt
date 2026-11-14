package dev.leemh.dochagjari

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.UUID

internal val CabinInk = Color(0xFF101722)
internal val CabinPanel = Color(0xFF1C2737)
internal val CabinBlue = Color(0xFF448AFF)
internal val CabinMuted = Color(0xFFBAC8DB)

@Composable fun OccupantSettingsScreen(state: AppState, model: AppModel, close: () -> Unit) {
    val save = state.occupants
    val enabled = state.vehicle.stopped == true && save.persisted != null && save.status != ProfileSaveStatus.LOADING
    var editor by remember { mutableStateOf<OccupantPreset?>(null) }
    var once by remember { mutableStateOf(false) }
    var command by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf(OccupantInput.TEXT) }
    var preview by remember { mutableStateOf<OccupantCommand?>(null) }
    var voiceIssue by remember { mutableStateOf("") }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
                command = it.take(160); origin = OccupantInput.SPEECH
                preview = OccupantCommands.parse(command, save.book)
            }
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = CabinBlue, background = CabinInk, surface = CabinPanel,
        onSurface = Color.White, onBackground = Color.White), typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 20.sp))) {
        Dialog(close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize().testTag("occupant-settings"), color = CabinInk) {
                Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("오늘의 탑승", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("탑승 역할 · 선택 사항", color = CabinMuted, fontSize = 16.sp)
                        }
                        OutlinedButton(close) { Text("닫기") }
                    }
                    Text("목적지와 별개로 설정해요. 이번만 바뀌어도 저장한 모드는 그대로예요.", color = CabinMuted)
                    Text(save.status.label, color = if (save.status == ProfileSaveStatus.ERROR) Color(0xFFFFBA83) else CabinBlue,
                        modifier = Modifier.testTag("occupant-save-status"))
                    if (save.status == ProfileSaveStatus.ERROR) {
                        Text(save.issue, color = Color(0xFFFFBA83)); OutlinedButton(model::retryOccupants, enabled = state.vehicle.stopped == true) { Text("탑승 구성 저장 재시도") }
                    }
                    val active = save.book.active
                    if (active != null) CabinCard {
                        Text(if (save.active != null) "이번 탑승 · ${active.name}" else "지난 구성 · ${active.name}", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("${active.source.label} · ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(active.confirmedAt))}", color = CabinMuted, fontSize = 14.sp)
                        Text(if (save.active != null) "저장·재확인 완료 · 이번 안내 조건에 적용해요" else "다시 실행했어요. 이번에도 같은 구성인지 확인해 주세요.", color = CabinMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (save.active == null) Button({ model.applyOccupants(OccupantPreset(active.presetId ?: "trip-once", active.name, active.mode, active.seats, active.needs), OccupantInput.USER) }, enabled = enabled) { Text("이번에도 같은 구성") }
                            OutlinedButton({ once = true; editor = OccupantPreset(active.presetId ?: "trip-once", active.name, active.mode, active.seats, active.needs) }, enabled = enabled) { Text("이번만 변경") }
                            OutlinedButton(model::endOccupantTrip, enabled = enabled) { Text("이번 탑승 종료") }
                        }
                    }
                    Text("저장한 탑승 모드", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth < 600.dp) 1 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    save.book.presets.chunked(columns).forEach { presets ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            presets.forEach { p ->
                                CabinCard(Modifier.weight(1f).testTag("preset-${p.id}")) {
                                    Text(p.name, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                    Text("왼쪽 ${p.seats.left.role.label} · 오른쪽 ${p.seats.right.role.label}", color = CabinMuted, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button({ model.applyOccupants(p) }, enabled = enabled) { Text("${p.name} 적용") }
                                        TextButton({ once = false; editor = p }, enabled = enabled) { Text("편집") }
                                    }
                                }
                            }
                            if (columns == 2 && presets.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    } }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton({ once = false; editor = OccupantPreset(UUID.randomUUID().toString(), "", OccupantMode.CUSTOM) }, enabled = enabled && save.book.presets.size < 20) { Text("새 프리셋 추가") }
                        TextButton(model::skipOccupants, enabled = enabled) { Text("탑승 설정 건너뛰기") }
                    }
                    if (save.book.skipped) Text("설정을 건너뛰었어요. 모드는 보관하고 기본 안내를 이용해요.", color = CabinMuted)
                    CabinCard {
                        Text("말하거나 짧게 입력해요", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(command, { command = it.take(160); origin = OccupantInput.TEXT; preview = null },
                            label = { Text("예: 오늘 혼자야 / 아이 오른쪽 뒤에 탔어") }, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("occupant-command"))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button({ preview = OccupantCommands.parse(command, save.book) }, enabled = enabled && command.isNotBlank()) { Text("구성 확인") }
                            OutlinedButton({
                                voiceIssue = ""
                                runCatching { speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR").putExtra(RecognizerIntent.EXTRA_PROMPT, "오늘의 탑승 구성을 말해 주세요")) }
                                    .onFailure { voiceIssue = "이 환경의 음성 입력을 열 수 없어요. 텍스트나 모드 선택을 이용해 주세요." }
                            }, enabled = enabled) { Text("음성으로 입력") }
                        }
                        if (voiceIssue.isNotBlank()) Text(voiceIssue, color = CabinMuted)
                        preview?.let { result ->
                            Text("${origin.label} · ${result.message}", color = CabinMuted)
                            result.candidates.forEach { p ->
                                Text("${p.name}: 왼쪽 ${p.seats.left.role.label} / 오른쪽 ${p.seats.right.role.label}")
                                Button({ model.applyOccupants(p, origin); preview = null; command = "" }, enabled = enabled) { Text("${p.name} 확인 적용") }
                            }
                        }
                    }
                    Text("좌석 관측 · ${state.vehicle.source.label}", fontSize = 19.sp)
                    Text("뒷좌석 왼쪽 ${state.vehicle.left} / 오른쪽 ${state.vehicle.right}", color = CabinMuted)
                    Text(OccupantSuggestions.describe(state.vehicle.left, state.vehicle.right), color = CabinMuted)
                    Text("센서는 점유만 확인해요. 아이·환자·앞좌석 상태를 자동 판정하지 않아요.", color = CabinMuted, fontSize = 14.sp)
                }
            }
        }
        editor?.let { original -> OccupantEditor(original, once, save.book.presets.size > 1 && save.book.presets.any { it.id == original.id }, enabled,
            { edited -> if (once) model.applyOccupants(edited, OccupantInput.USER) else model.saveOccupantPreset(edited); editor = null },
            { model.deleteOccupantPreset(original.id); editor = null }, { editor = null }) }
    }
}

@Composable internal fun CabinCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = CabinPanel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable private fun OccupantEditor(original: OccupantPreset, once: Boolean, deletable: Boolean, enabled: Boolean,
    save: (OccupantPreset) -> Unit, delete: () -> Unit, close: () -> Unit) {
    var name by remember(original) { mutableStateOf(original.name) }
    var seats by remember(original) { mutableStateOf(original.seats.copy(setup = SeatSetup.CONFIGURED)) }
    var needs by remember(original) { mutableStateOf(original.needs) }
    var deleting by remember { mutableStateOf(false) }
    Dialog(close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("occupant-editor"), color = CabinInk) {
            Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (once) "이번 탑승만 변경" else "프리셋 편집", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    OutlinedButton(close) { Text("취소") }
                }
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("구성 이름") }, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("occupant-preset-name"))
                listOf(true, false).forEach { left ->
                    val a = if (left) seats.left else seats.right
                    val side = if (left) "왼쪽" else "오른쪽"
                    Text("뒷좌석 $side", fontSize = 22.sp)
                    SeatRole.entries.chunked(2).forEach { roles -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        roles.forEach { role -> FilterChip(a.role == role, {
                            seats = if (left) seats.copy(left = a.copy(role = role)) else seats.copy(right = a.copy(role = role))
                        }, label = { Text("$side ${role.label}") }, enabled = enabled) }
                    } }
                    CabinToggle("$side 카시트", a.childSeat, enabled) { value -> seats = if (left) seats.copy(left = a.copy(childSeat = value)) else seats.copy(right = a.copy(childSeat = value)) }
                }
                HorizontalDivider()
                Text("이동 조건 · 직접 선택", fontSize = 22.sp)
                CabinToggle("왼쪽 문 여유", needs.leftDoor, enabled) { needs = needs.copy(leftDoor = it) }
                CabinToggle("오른쪽 문 여유", needs.rightDoor, enabled) { needs = needs.copy(rightDoor = it) }
                CabinToggle("유모차 · 무단차 필요", needs.stroller, enabled) { needs = needs.copy(stroller = it) }
                CabinToggle("휠체어 · 무단차 필요", needs.wheelchair, enabled) { needs = needs.copy(wheelchair = it) }
                CabinToggle("보호자 동행 유지", needs.companion, enabled) { needs = needs.copy(companion = it) }
                Text(if (once) "이번 구성만 바꿔요. 저장 프리셋은 그대로예요." else "프리셋 저장은 이번 탑승을 자동으로 바꾸지 않아요.", color = CabinMuted)
                Button({ save(original.copy(name = name.trim(), seats = seats, needs = needs,
                    mode = if (seats != original.seats || needs != original.needs) OccupantMode.CUSTOM else original.mode)) }, enabled = enabled && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (once) "이번 탑승만 적용" else "프리셋 저장")
                }
                if (deletable && !once) TextButton({ deleting = true }, enabled = enabled) { Text("이 프리셋 삭제") }
            }
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("저장 프리셋을 삭제할까요?") },
        text = { Text("이번 탑승 기록과 기존 방문·알림은 유지해요.") },
        confirmButton = { TextButton(delete) { Text("삭제") } }, dismissButton = { TextButton({ deleting = false }) { Text("취소") } })
}

@Composable private fun CabinToggle(label: String, value: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f).padding(top = 12.dp)); Switch(value, change, enabled = enabled)
    }
}
