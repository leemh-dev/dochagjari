package dev.leemh.dochagjari

/** User choices; neither occupancy nor video analysis can establish a passenger's role. */
enum class SeatRole(val label: String) { UNSPECIFIED("미지정"), ADULT("성인"), CHILD("어린이"), INFANT("영유아") }
enum class SeatSetup(val label: String) { NOT_SET("아직 설정하지 않음"), CONFIGURED("저장한 역할 사용"), SKIPPED("설정 없이 안내") }
data class SeatAssignment(val role: SeatRole = SeatRole.UNSPECIFIED, val childSeat: Boolean = false)
data class SeatPreferences(val setup: SeatSetup = SeatSetup.NOT_SET, val left: SeatAssignment = SeatAssignment(), val right: SeatAssignment = SeatAssignment()) {
    fun suggestion(leftSide: Boolean, occupancy: Occupancy): String? {
        if (setup != SeatSetup.CONFIGURED || occupancy != Occupancy.OCCUPIED) return null
        val seat = if (leftSide) left else right
        if (seat.role == SeatRole.UNSPECIFIED && !seat.childSeat) return null
        return "${if (leftSide) "왼쪽" else "오른쪽"} 저장 선택 · ${seat.role.label}${if (seat.childSeat) " · 카시트 사용" else ""}. 이번 탑승에도 맞는지 확인하세요."
    }
}

/** A draft or stale write acknowledgement must never be called a saved passenger choice. */
fun ProfileState.seatSuggestion(leftSide: Boolean, occupancy: Occupancy): String? {
    if (status != ProfileSaveStatus.SAVED || revision != savedRevision) return null
    return persisted?.seats?.suggestion(leftSide, occupancy)
}
