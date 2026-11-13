package dev.leemh.dochagjari

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HttpArrivalTransportTest {
    private fun request(server: MockWebServer) = ConfirmedArrival("11111111-1111-4111-8111-111111111111", Position(37.48, 127.03), "B1 C12 · 서쪽 문", "demo", DeliveryChannel.SERVER_MOCK, server.url("/").toString().trimEnd('/'), InputSource.TEST, 200, 100)
    @Test fun lookup404AndPostUseFrozenPayloadAndParseReceipt() {
        MockWebServer().use { server ->
            server.start(); val frozen = request(server)
            server.enqueue(MockResponse().setResponseCode(404))
            val receipt = JSONObject().put("status", "mock").put("request_hash", frozen.fingerprint()).put("delivery_mode", "mock").put("input_source", "TEST")
            server.enqueue(MockResponse().setBody(receipt.toString()))
            val transport = HttpArrivalTransport()
            assertEquals(LookupResult.Missing, transport.lookup(frozen, "test-only-token"))
            assertEquals(DeliveryStatus.MOCK, transport.submit(frozen, "test-only-token").status)
            val lookup = server.takeRequest(); val post = server.takeRequest()
            assertEquals("GET", lookup.method); assertEquals("POST", post.method)
            assertEquals("/v1/arrivals/${frozen.sessionId}/demo", lookup.path)
            assertEquals("Bearer test-only-token", post.getHeader("Authorization"))
            val body = JSONObject(post.body.readUtf8())
            assertEquals(frozen.sessionId, body.getString("session_id"))
            assertEquals(frozen.clue, body.getString("clue"))
            assertEquals("mock", body.getString("delivery_mode"))
            assertTrue(body.getBoolean("confirmed")); assertTrue(body.getBoolean("stopped"))
        }
    }
    @Test fun redirectsDoNotForwardTokenAndLegacyReceiptDoesNotBecomeSuccess() {
        MockWebServer().use { server ->
            server.start(); val frozen = request(server)
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", server.url("/redirected")))
            val transport = HttpArrivalTransport()
            assertThrows(DeliveryException::class.java) { transport.lookup(frozen, "test-token") }
            assertEquals(1, server.requestCount)
            server.enqueue(MockResponse().setBody("""{"status":"accepted","provider_id":"old"}"""))
            assertEquals(DeliveryIssue.MISMATCH, assertThrows(DeliveryException::class.java) { transport.lookup(frozen, "test-token") }.issue)
            assertEquals(2, server.requestCount)
        }
    }
}
