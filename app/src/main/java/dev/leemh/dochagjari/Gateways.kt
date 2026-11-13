package dev.leemh.dochagjari

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

interface NavigationGateway { fun route(point: AccessPoint); fun close() }
class PleosNavigationGateway(context: Context, private val report: (String) -> Unit) : NavigationGateway {
    private var helper: NaviHelper? = null
    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) { report("실제 SDK 경로 시작: $routeStartInfo") }
        override fun onRoutingCompleted() { report("실제 SDK 경로 계산 완료 · 핀은 순정 내비에서 확인하세요") }
        override fun onDestinationInfo(destinationInfo: DestinationInfo) { report("실제 SDK 목적지: $destinationInfo") }
        override fun onError(errorCode: NaviErrorCode) { report("실제 SDK 오류: $errorCode") }
    }
    init {
        try { helper = NaviHelper(context).also { it.initialize(); it.addListener(listener) }; report("NaviHelper 연결 요청 완료") }
        catch (e: Exception) { report("NaviHelper 연결 불가: ${e.javaClass.simpleName}: ${e.message}"); close() }
    }
    override fun route(point: AccessPoint) {
        val navi = helper ?: return report("NaviHelper 미연결 · 테스트 입력을 선택하거나 Pleos에서 재실행하세요")
        try {
            report("실제 SDK 요청: ${point.name} ${point.position}")
            navi.requestRoute(RouteInfo(latitude = point.position.latitude, longitude = point.position.longitude, poiId = "", poiName = point.name, poiSubId = "0", address = "시연용 접근점"))
            navi.getDestinationInfo()
        } catch (e: Exception) { report("실제 SDK 요청 실패: ${e.javaClass.simpleName}: ${e.message}") }
    }
    override fun close() { runCatching { helper?.removeListener(listener); helper?.release() }; helper = null }
}
class TestNavigationGateway(private val report: (String) -> Unit) : NavigationGateway {
    override fun route(point: AccessPoint) { report("모의 내비 요청: ${point.name} ${point.position} · 실제 핀 미검증") }
    override fun close() {}
}

data class VehicleSnapshot(
    val left: Occupancy = Occupancy.UNKNOWN, val right: Occupancy = Occupancy.UNKNOWN,
    val stopped: Boolean? = null, val source: InputSource = InputSource.VEHICLE,
    val status: String = "초기값 대기", val observedAt: Long = System.currentTimeMillis()
)
interface VehicleStateSource { fun start(); fun close() }
class TestVehicleStateSource(private val emit: (VehicleSnapshot) -> Unit) : VehicleStateSource {
    private var snapshot = VehicleSnapshot(source = InputSource.TEST, status = "테스트 입력", stopped = true)
    override fun start() { emit(snapshot) }
    fun set(left: Occupancy = snapshot.left, right: Occupancy = snapshot.right, stopped: Boolean? = snapshot.stopped, status: String = "테스트 입력") {
        snapshot = VehicleSnapshot(left, right, stopped, InputSource.TEST, status); emit(snapshot)
    }
    override fun close() {}
}

/** Public AAOS path only. A privileged permission is never bypassed. */
class AaosVehicleStateSource(private val context: Context, private val emit: (VehicleSnapshot) -> Unit) : VehicleStateSource {
    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private var snapshot = VehicleSnapshot()
    private var speed: Float? = null
    private var parked: Boolean? = null
    private var active = false
    private val issues = linkedSetOf<String>()
    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) { consume(value) }
        override fun onErrorEvent(propId: Int, zone: Int) {
            speed = null; parked = null; snapshot = snapshot.copy(stopped = null)
            publish("속성 오류 $propId / $zone")
        }
    }
    private fun publish(status: String) {
        snapshot = snapshot.copy(status = status, observedAt = System.currentTimeMillis())
        Log.i("DochagjariVehicle", snapshot.toString()); emit(snapshot)
    }
    override fun start() {
        active = true
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (!active) { connected.disconnect(); return@createCar }
                if (!ready) { speed = null; parked = null; snapshot = VehicleSnapshot(); publish("차량 서비스 연결 끊김"); return@createCar }
                manager = connected.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                val m = manager ?: return@createCar publish("차량 속성 서비스 미지원")
                listOf(VehiclePropertyIds.PERF_VEHICLE_SPEED, VehiclePropertyIds.GEAR_SELECTION).forEach { id ->
                    try {
                        val config = m.getCarPropertyConfig(id)
                        if (config == null) issues += "속성 미지원 $id" else {
                            if (!m.registerCallback(callback, id, CarPropertyManager.SENSOR_RATE_ONCHANGE)) issues += "구독 실패 $id"
                            config.areaIds.forEach { area -> runCatching { consume(m.getProperty<Any>(id, area)) }.onFailure { issues += "초기값 미수신 $id/$area" } }
                        }
                    } catch (e: SecurityException) { issues += "권한 없음 $id" }
                    catch (e: Exception) { issues += "접근 실패 $id: ${e.javaClass.simpleName}" }
                }
                publish(if (issues.isEmpty()) "차량 API 구독 중" else issues.joinToString(" · "))
            }
        } catch (e: Throwable) {
            if (e !is LinkageError && e !is Exception) throw e
            publish("차량 API 사용 불가: ${e.javaClass.simpleName}")
        }
    }
    private fun consume(value: CarPropertyValue<*>) {
        if (!active) return
        val available = value.status == CarPropertyValue.STATUS_AVAILABLE
        when (value.propertyId) {
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> speed = if (available) (value.value as? Number)?.toFloat() else null
            VehiclePropertyIds.GEAR_SELECTION -> parked = if (available) (value.value as? Number)?.toInt() == 4 else null
        }
        snapshot = snapshot.copy(stopped = if (speed == null || parked == null) null else kotlin.math.abs(speed!!) < .1f && parked == true)
        publish(if (issues.isEmpty()) "차량 API 수신" else issues.joinToString(" · "))
    }
    override fun close() { active = false; runCatching { manager?.unregisterCallback(callback) }; runCatching { car?.disconnect() }; manager = null; car = null }
}
