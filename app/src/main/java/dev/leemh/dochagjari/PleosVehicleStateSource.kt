package dev.leemh.dochagjari

import android.content.Context
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Seat
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener

/** Pleos seat domain with AAOS motion. Capability failures remain visible. */
class PleosVehicleStateSource(private val context: Context, private val emit: (VehicleSnapshot) -> Unit) : VehicleStateSource {
    private var vehicle: Vehicle? = null
    private var seat: Seat? = null
    private var motion: AaosVehicleStateSource? = null
    private var active = false
    private var snapshot = VehicleSnapshot()
    private var seatStatus = "Pleos 좌석 초기값 대기"
    private var motionStatus = "AAOS 주행 초기값 대기"
    private val subscriptions = mutableListOf<Pair<SeatArea, SeatOccupancyListener>>()
    @Synchronized private fun publish() {
        if (!active) return
        snapshot = snapshot.copy(status = "$seatStatus · $motionStatus", observedAt = System.currentTimeMillis())
        android.util.Log.i("DochagjariPleosSeat", snapshot.toString()); emit(snapshot)
    }
    @Synchronized private fun occupancy(left: Boolean, value: Boolean?) {
        val o = when(value) { true -> Occupancy.OCCUPIED; false -> Occupancy.EMPTY; null -> Occupancy.UNKNOWN }
        snapshot = if (left) snapshot.copy(left = o) else snapshot.copy(right = o)
        seatStatus = "Pleos SDK 좌석 수신"; publish()
    }
    @Synchronized private fun failure(message: String, left: Boolean? = null) {
        if (left != null) snapshot = if (left) snapshot.copy(left = Occupancy.UNKNOWN) else snapshot.copy(right = Occupancy.UNKNOWN)
        else snapshot = snapshot.copy(left = Occupancy.UNKNOWN, right = Occupancy.UNKNOWN)
        seatStatus = message; publish()
    }
    override fun start() {
        active = true
        try {
            motion = AaosVehicleStateSource(context) { value ->
                synchronized(this) { snapshot = snapshot.copy(stopped = value.stopped); motionStatus = value.status; publish() }
            }.also { it.start() }
            vehicle = Vehicle(context).also { it.initialize() }
            val domain = vehicle!!.getSeat(); seat = domain
            domain.checkSeatOccupancyCapabilities(onComplete = { config, areas ->
                if (active) {
                    if (config.accessType !in listOf(AccessType.READ, AccessType.READ_WRITE)) {
                        failure("Pleos 좌석 읽기 미지원: ${config.accessType}")
                        return@checkSeatOccupancyCapabilities
                    }
                    listOf(true to SeatValue.ROW_2_LEFT, false to SeatValue.ROW_2_RIGHT).forEach { (left, value) ->
                        val area = areas.firstOrNull { it.values.contains(value) && it.values.size == 1 }
                        if (area == null) failure("Pleos 좌석 미지원: $value", left)
                        else {
                            val listener = object : SeatOccupancyListener {
                                override fun onSeatOccupancyUpdated(occupied: Boolean?) { if (active) occupancy(left, occupied) }
                                override fun onFailed(exception: Exception) { if (active) failure("Pleos 좌석 구독 실패: ${exception.javaClass.simpleName}: ${exception.message}", left) }
                            }
                            try {
                                domain.registerSeatOccupancy(area, listener); subscriptions += area to listener
                                domain.getCurrentSeatOccupancy(area, onComplete = { occupied -> if (active) occupancy(left, occupied) }, onFailed = { e -> if (active) failure("Pleos 좌석 초기값 실패: ${e.message}", left) })
                            } catch (e: Exception) { failure("Pleos 좌석 권한·연결 오류: ${e.message}", left) }
                        }
                    }
                }
            }, onFailed = { e -> if (active) failure("Pleos 좌석 capability 실패: ${e.javaClass.simpleName}: ${e.message}") })
        } catch (e: Exception) { failure("Pleos Vehicle 연결 실패: ${e.javaClass.simpleName}: ${e.message}") }
        catch (e: LinkageError) { failure("차량 라이브러리 미지원: ${e.javaClass.simpleName}") }
    }
    override fun close() {
        active = false
        subscriptions.forEach { (area, listener) -> runCatching { seat?.unregisterSeatOccupancy(area, listener) } }
        subscriptions.clear(); motion?.close(); runCatching { vehicle?.release() }; seat = null; vehicle = null
    }
}
