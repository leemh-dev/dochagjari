package dev.leemh.dochagjari

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Each recovery starts with a GET; credentials stay in memory and never follow redirects. */
class HttpArrivalTransport : ArrivalTransport {
    override fun lookup(request: ConfirmedArrival, token: String): LookupResult {
        val (code, body) = exchange(request.endpoint + "/v1/arrivals/${request.sessionId}/${request.recipientId}", "GET", token)
        if (code == 404) return LookupResult.Missing
        checkCode(code)
        return LookupResult.Found(receipt(body))
    }
    override fun submit(request: ConfirmedArrival, token: String): DeliveryReceipt {
        val body = JSONObject().put("session_id", request.sessionId).put("recipient_id", request.recipientId)
            .put("latitude", if (request.position.latitude == 0.0) 0.0 else request.position.latitude)
            .put("longitude", if (request.position.longitude == 0.0) 0.0 else request.position.longitude)
            .put("clue", request.clue).put("stopped", true).put("confirmed", true)
            .put("input_source", request.source.name).put("delivery_mode", request.channel.serverMode)
        val (code, raw) = exchange(request.endpoint + "/v1/arrivals", "POST", token, body.toString())
        checkCode(code)
        return receipt(raw)
    }
    private fun checkCode(code: Int) {
        if (code !in 200..299) throw DeliveryException(when (code) { 401 -> DeliveryIssue.AUTH; 409 -> DeliveryIssue.MISMATCH; in 400..499 -> DeliveryIssue.REJECTED; else -> DeliveryIssue.NETWORK })
    }
    private fun receipt(raw: String): DeliveryReceipt = try {
        val j = JSONObject(raw)
        DeliveryReceipt(DeliveryStatus.valueOf(j.getString("status").uppercase(Locale.ROOT)), j.getString("request_hash"), j.getString("delivery_mode"), InputSource.valueOf(j.getString("input_source")))
    } catch (_: Exception) { throw DeliveryException(DeliveryIssue.MISMATCH) }

    private fun exchange(endpoint: String, method: String, token: String, body: String? = null): Pair<Int, String> {
        val url = URL(endpoint)
        require(url.userInfo == null && (url.protocol == "https" || url.protocol == "http" && url.host in setOf("127.0.0.1", "localhost", "10.0.2.2")))
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method; connection.connectTimeout = 10_000; connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            return code to raw
        } finally { connection.disconnect() }
    }
}
