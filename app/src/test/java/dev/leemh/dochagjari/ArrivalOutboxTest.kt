package dev.leemh.dochagjari

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ArrivalOutboxTest {
    @get:Rule val temporary = TemporaryFolder()
    private val file: File get() = File(temporary.root, "outbox.json")
    private fun request(outbox: ArrivalOutbox, channel: DeliveryChannel = DeliveryChannel.SENS) = ConfirmedArrival(
        outbox.state.value.record.sessionId, Position(37.48, 127.03), "B1 C12 · 서쪽 문", "demo", channel,
        if (channel == DeliveryChannel.LOCAL_MOCK) "" else "https://example.invalid", InputSource.TEST, 200, 100)

    private class Transport : ArrivalTransport {
        val events = mutableListOf<String>()
        val submissions = mutableListOf<ConfirmedArrival>()
        var stored: DeliveryReceipt? = null
        var loseResponse = false
        var lookupFailure = false
        var lookupHook: () -> Unit = {}
        override fun lookup(request: ConfirmedArrival, token: String): LookupResult {
            events += "GET"; lookupHook()
            if (lookupFailure) throw IOException("network unavailable")
            return stored?.let { LookupResult.Found(it) } ?: LookupResult.Missing
        }
        override fun submit(request: ConfirmedArrival, token: String): DeliveryReceipt {
            events += "POST"; submissions += request
            stored = DeliveryReceipt(if (request.channel == DeliveryChannel.SENS) DeliveryStatus.ACCEPTED else DeliveryStatus.MOCK, request.fingerprint(), request.channel.serverMode, request.source)
            if (loseResponse) throw IOException("response lost after acceptance")
            return stored!!
        }
    }
    @Test fun terminationBeforeWorkerRestoresExactRequestThenLooksUpBeforeSending() = runTest {
        val transport = Transport(); val dispatcher = StandardTestDispatcher(testScheduler)
        val first = ArrivalOutbox(FileArrivalJournal(file), transport, dispatcher)
        val frozen = request(first)
        assertTrue(first.prepare(frozen))
        val restarted = ArrivalOutbox(FileArrivalJournal(file), transport, dispatcher)
        assertEquals(frozen, restarted.state.value.record.request)
        restarted.recover("token-in-memory")
        assertEquals(listOf("GET", "POST"), transport.events)
        assertEquals(listOf(frozen), transport.submissions)
        assertEquals(DeliveryStatus.ACCEPTED, restarted.state.value.record.status)
        assertFalse(file.readText().contains("token-in-memory"))
        assertFalse(file.readText().contains("phone"))
    }
    @Test fun lostPostResponseIsRecoveredByGetWithoutAnotherPost() = runTest {
        val t = Transport().apply { loseResponse = true }; val d = StandardTestDispatcher(testScheduler)
        val first = ArrivalOutbox(FileArrivalJournal(file), t, d)
        first.prepare(request(first)); first.recover("token")
        assertEquals(DeliveryIssue.NETWORK, first.state.value.issue)
        val restored = ArrivalOutbox(FileArrivalJournal(file), t, d)
        restored.recover("token")
        assertEquals(DeliveryStatus.ACCEPTED, restored.state.value.record.status)
        assertEquals(1, t.submissions.size)
        assertEquals(listOf("GET", "POST", "GET"), t.events)
    }
    @Test fun concurrentAndRepeatedRecoveryNeverSubmitsAnotherRequest() = runTest {
        val t = Transport(); val outbox = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        outbox.prepare(request(outbox))
        List(12) { launch { outbox.recover("token") } }.forEach { it.join() }
        assertEquals(1, t.submissions.size)
        assertEquals(DeliveryStatus.ACCEPTED, outbox.state.value.record.status)
    }
    @Test fun missingTokenAndFailedLookupNeverPostOrCreateNewId() = runTest {
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        val r = request(o); o.prepare(r); o.recover("")
        assertEquals(DeliveryIssue.TOKEN_REQUIRED, o.state.value.issue)
        assertTrue(t.events.isEmpty())
        t.lookupFailure = true; o.recover("token")
        assertEquals(DeliveryIssue.NETWORK, o.state.value.issue)
        assertFalse(o.newSession()); assertEquals(r.sessionId, o.state.value.record.sessionId)
        assertTrue(t.submissions.isEmpty())
    }
    @Test fun serverPendingUnknownAndFailedAreNotReposted() = runTest {
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        val r = request(o); o.prepare(r)
        for (status in listOf(DeliveryStatus.PENDING, DeliveryStatus.UNKNOWN, DeliveryStatus.FAILED)) {
            t.stored = DeliveryReceipt(status, r.fingerprint(), "sens", InputSource.TEST)
            o.recover("token"); assertEquals(status, o.state.value.record.status)
            if (!status.terminal) assertFalse(o.newSession())
        }
        assertTrue(t.submissions.isEmpty())
    }
    @Test fun disappearanceOfPreviouslyObservedReservationNeverAllowsResend() = runTest {
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        val r = request(o); o.prepare(r)
        t.stored = DeliveryReceipt(DeliveryStatus.PENDING, r.fingerprint(), "sens", InputSource.TEST)
        o.recover("token"); t.stored = null
        val restarted = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        restarted.recover("token")
        assertEquals(DeliveryIssue.RESULT_MISSING, restarted.state.value.issue)
        assertTrue(t.submissions.isEmpty())
    }
    @Test fun changedRecipientPositionAndModeCannotReplaceFrozenRequest() = runTest {
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        val original = request(o); o.prepare(original)
        listOf(original.copy(recipientId = "other"), original.copy(position = Position(36.0, 126.0)), original.copy(channel = DeliveryChannel.SERVER_MOCK)).forEach {
            assertFalse(o.prepare(it)); assertEquals(original, o.state.value.record.request)
        }
        o.recover("token"); assertEquals(original, t.submissions.single())
    }
    @Test fun responseWithDifferentModeOrHashDoesNotCompleteOrResend() = runTest {
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        val r = request(o); o.prepare(r)
        t.stored = DeliveryReceipt(DeliveryStatus.MOCK, r.fingerprint(), "mock", InputSource.TEST)
        o.recover("token"); assertEquals(DeliveryIssue.MISMATCH, o.state.value.issue)
        t.stored = DeliveryReceipt(DeliveryStatus.ACCEPTED, "wrong", "sens", InputSource.TEST)
        o.recover("token"); assertEquals(DeliveryIssue.MISMATCH, o.state.value.issue)
        t.stored = null
        o.recover("token"); assertEquals(DeliveryIssue.RESULT_MISSING, o.state.value.issue)
        assertTrue(t.submissions.isEmpty())
    }
    @Test fun revocationWhileLookupIsInFlightIsDurableAndPreventsPost() = runTest {
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        o.prepare(request(o)); t.lookupHook = { assertTrue(o.revokeConsent()) }
        o.recover("token")
        assertEquals(DeliveryIssue.CONSENT_REVOKED, o.state.value.issue)
        assertFalse(FileArrivalJournal(file).load()!!.consentActive)
        t.lookupHook = {}
        ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler)).recover("token")
        assertTrue(t.submissions.isEmpty())
    }
    @Test fun localMockRestartNeverContactsServer() = runTest {
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        o.prepare(request(o, DeliveryChannel.LOCAL_MOCK))
        val restored = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        restored.recover(""); restored.recover("irrelevant")
        assertEquals(DeliveryStatus.MOCK, restored.state.value.record.status)
        assertTrue(t.events.isEmpty())
    }
    @Test fun journalSaveFailurePreventsAnyNetworkAttempt() = runTest {
        val t = Transport()
        val journal = object : ArrivalJournal {
            override fun load(): ArrivalRecord? = null
            override fun save(record: ArrivalRecord) { throw IOException("disk full") }
        }
        val o = ArrivalOutbox(journal, t, StandardTestDispatcher(testScheduler))
        assertFalse(o.prepare(request(o))); o.recover("token")
        assertEquals(DeliveryIssue.STORAGE, o.state.value.issue)
        assertTrue(t.events.isEmpty())
    }
    @Test fun corruptAndLegacyUnknownRecordsBlockAutomaticNewSession() = runTest {
        file.writeText("broken")
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        assertEquals(DeliveryIssue.CORRUPT, o.state.value.issue); assertFalse(o.prepare(request(o))); assertFalse(o.newSession())
        file.delete()
        val raw = """{"id":"11111111-1111-4111-8111-111111111111","notification":"발송 확인 필요 · 요청 중","clue":"C12"}"""
        val legacy = ArrivalOutbox(FileArrivalJournal(file) { raw }, t, StandardTestDispatcher(testScheduler))
        legacy.recover("token"); assertEquals(DeliveryIssue.LEGACY, legacy.state.value.issue)
        assertFalse(legacy.newSession()); assertTrue(t.events.isEmpty())
    }
    @Test fun invalidEndpointConsentAndCoordinatesAreNotJournaled() = runTest {
        val t = Transport(); val o = ArrivalOutbox(FileArrivalJournal(file), t, StandardTestDispatcher(testScheduler))
        listOf(request(o).copy(endpoint = "https://user:secret@example.invalid"), request(o).copy(endpoint = "https://example.invalid?token=secret"),
            request(o).copy(consentedAt = 0), request(o).copy(position = Position(Double.NaN, 127.0))).forEach { assertFalse(o.prepare(it)) }
        assertFalse(file.exists())
    }
    @Test fun fingerprintMatchesPythonAndJsonRoundtripDoesNotChangeIt() = runTest {
        val o = ArrivalOutbox(FileArrivalJournal(file), Transport(), StandardTestDispatcher(testScheduler))
        val r = request(o).copy(sessionId = "11111111-1111-4111-8111-111111111111")
        assertEquals("519567516c037555e7ec9535c3b6c0ddb7f93e66e8dfd664acb49d2157fd786d", r.fingerprint())
        val record = ArrivalRecord(r.sessionId, r, DeliveryStatus.QUEUED, true)
        assertEquals(record, ArrivalRecordCodec.decode(ArrivalRecordCodec.encode(record)))
        assertNotEquals(r.fingerprint(), r.copy(clue = "다른 위치").fingerprint())
    }
}
