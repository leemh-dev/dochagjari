package dev.leemh.dochagjari

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

interface ArrivalNotifier { fun notify(session: PickupSession, recipientId: String): String }
class MockArrivalNotifier : ArrivalNotifier {
    override fun notify(session: PickupSession, recipientId: String): String {
        require(session.stopped && session.confirmed && session.position != null)
        return "모의 발송 완료 · 실제 SMS 미발송"
    }
}
class HttpArrivalNotifier(private val endpoint: String, private val token: String) : ArrivalNotifier {
    override fun notify(session: PickupSession, recipientId: String): String {
        val url = URL(endpoint.trimEnd('/') + "/v1/arrivals")
        require(url.protocol == "https" || url.host in listOf("127.0.0.1", "localhost", "10.0.2.2")) { "HTTPS 주소가 필요합니다" }
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.connectTimeout = 10_000; connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            val payload = JSONObject().put("session_id", session.id).put("recipient_id", recipientId)
                .put("latitude", session.position!!.latitude).put("longitude", session.position.longitude)
                .put("clue", session.clue).put("stopped", session.stopped).put("confirmed", session.confirmed)
                .put("input_source", session.source.name)
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) return "발송 확인 필요 · 서버 HTTP $code · 같은 세션 ID로 서버 기록을 확인하세요"
            val result = JSONObject(raw)
            return when (result.optString("status")) {
                "mock" -> "서버 모의 발송 완료 · 실제 SMS 미발송"
                "accepted" -> "공급자 수락 · 휴대폰 수신 미확인"
                "received" -> "휴대폰 수신 확인"
                "failed" -> "발송 실패 · 서버 기록 확인 필요"
                else -> "발송 확인 필요 · 서버 처리 중 또는 결과 불명"
            }
        } finally { connection.disconnect() }
    }
}
