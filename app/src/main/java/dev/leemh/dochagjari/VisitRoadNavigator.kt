package dev.leemh.dochagjari

import android.content.Context
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/** SDK broadcasts have no request ID: receiving an event never confirms a pin or an entrance. */
class VisitRoadNavigator(private val context: Context, private val attempt: RoadAttempt,
    private val report: (RoadStatus, String, String) -> Unit) {
    private var helper: NaviHelper? = null
    private var active = true
    private fun sdk(kind: String) { if (active) report(RoadStatus.SDK_EVENT_RECEIVED, kind, "SDK $kind 수신 · 이 요청과의 일치/실제 핀/입차 미확인") }
    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) = sdk("onRouteStarted")
        override fun onRoutingCompleted() = sdk("onRoutingCompleted")
        override fun onDestinationInfo(destinationInfo: DestinationInfo) = sdk("onDestinationInfo")
        override fun onError(errorCode: NaviErrorCode) { if (active) report(RoadStatus.SDK_EVENT_RECEIVED, "SDK_ERROR", "SDK 오류 이벤트 $errorCode · 이 요청과의 일치/실제 핀 미확인") }
    }
    fun dispatch() {
        if (attempt.channel == RoadChannel.MOCK) {
            report(RoadStatus.MOCK_COMPLETED, "MOCK_RESULT", "모의 내비 계산 완료 · 실제 SDK/핀/입차 미확인")
            return
        }
        try {
            helper = NaviHelper(context).also { it.initialize(); it.addListener(listener) }
            val t = attempt.target
            helper!!.requestRoute(RouteInfo(latitude = t.latitude, longitude = t.longitude, poiId = "", poiName = t.name, poiSubId = "0", address = t.address))
            report(RoadStatus.SDK_CALL_RETURNED, "SDK_CALL_RETURNED", "SDK 요청 함수 반환 · 실제 핀/입차 미확인")
            helper!!.getDestinationInfo()
        } catch (e: Exception) { report(RoadStatus.FAILED, "SDK_EXCEPTION", "SDK 함수 예외 ${e.javaClass.simpleName} · 전달 결과/실제 핀/입차 미확인") }
    }
    fun close() { active = false; runCatching { helper?.removeListener(listener); helper?.release() }; helper = null }
}
