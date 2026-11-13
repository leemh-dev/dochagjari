package dev.leemh.dochagjari

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun SeatRoleSettings(save: ProfileState, leftOccupancy: Occupancy, rightOccupancy: Occupancy, enabled: Boolean, onChange: (Profile) -> Unit) {
    val profile = save.profile
    val seats = profile.seats
    Text("탑승 역할 · 선택 사항", fontSize = 22.sp)
    Text("설정하지 않아도 매장 안내를 이용할 수 있습니다. 이름은 입력하지 않습니다.", fontSize = 15.sp)
    Text("사용자 선택: ${seats.setup.label}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ onChange(profile.copy(seats = seats.copy(setup = SeatSetup.SKIPPED))) }, enabled = enabled) { Text("건너뛰기 · 설정 없이 안내") }
        OutlinedButton({ onChange(profile.copy(seats = seats.copy(setup = SeatSetup.CONFIGURED))) }, enabled = enabled) { Text("저장한 역할 사용") }
    }
    Text("건너뛰기는 역할 사용만 중지합니다. 기존 역할·이동 조건·방문 기록은 지우지 않습니다.", fontSize = 14.sp)
    listOf(true to leftOccupancy, false to rightOccupancy).forEach { (left, occupancy) ->
        val name = if (left) "왼쪽" else "오른쪽"
        val assignment = if (left) seats.left else seats.right
        fun save(value: SeatAssignment) = onChange(profile.copy(seats = if (left) seats.copy(setup = SeatSetup.CONFIGURED, left = value) else seats.copy(setup = SeatSetup.CONFIGURED, right = value)))
        Text("뒷좌석 $name · 사용자 지정", fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeatRole.entries.forEach { role ->
                FilterChip(selected = assignment.role == role, enabled = enabled, onClick = { save(assignment.copy(role = role)) }, label = { Text("$name ${role.label}") })
            }
        }
        Row(Modifier.toggleable(value = assignment.childSeat, enabled = enabled, role = Role.Switch, onValueChange = { save(assignment.copy(childSeat = it)) }), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("$name 카시트 사용", Modifier.padding(top = 10.dp))
            Switch(checked = assignment.childSeat, onCheckedChange = null, enabled = enabled)
        }
        Text("점유 신호: $occupancy · 역할/카시트를 판별하는 값이 아닙니다.", fontSize = 14.sp)
        save.seatSuggestion(left, occupancy)?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp) }
    }
}
