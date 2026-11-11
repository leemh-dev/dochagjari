package dev.leemh.dochagjari

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

enum class DeliveryStatus(val label: String) {
    DRAFT("대기"), QUEUED("확정 요청 저장됨 · 복구 대기"),
    PENDING("서버 처리 중 · 결과 확인 필요"), UNKNOWN("발송 결과 불명 · 자동 재발송 안 함"),
    MOCK("모의 발송 완료 · 실제 SMS 미발송"), ACCEPTED("공급자 수락 · 휴대폰 수신 미확인"),
    FAILED("발송 실패 · 휴대폰 수신 미확인"), LEGACY_REVIEW("이전 발송 기록 · 수동 확인 필요");
    val terminal: Boolean get() = this in setOf(MOCK, ACCEPTED, FAILED)
}
enum class DeliveryChannel(val label: String) { LOCAL_MOCK("앱 모의 발송"), SERVER_MOCK("서버 모의 발송"), SENS("실제 SENS 요청");
    val serverMode: String get() = if (this == SENS) "sens" else "mock"
}
enum class DeliveryIssue(val label: String) {
    NONE(""), TOKEN_REQUIRED("서버 접근 토큰을 입력한 뒤 결과 확인·복구를 실행하세요"),
    AUTH("서버 인증 실패 · 토큰 확인 필요"), NETWORK("통신 결과 미확인 · 같은 세션 결과를 조회하세요"),
    MISMATCH("서버의 발송 방식·요청 정보가 다릅니다 · 재발송 중지"),
    RESULT_MISSING("이미 확인한 서버 기록이 없습니다 · 수동 확인 필요"),
    REJECTED("서버가 요청을 거부했습니다 · 동일 세션 기록을 확인하세요"),
    CONSENT_REVOKED("동의 해제됨 · 결과 조회만 가능, 이미 시작된 전송은 취소할 수 없습니다"),
    STORAGE("발송 기록 저장 실패 · 전송/복구를 중지했습니다"),
    CORRUPT("발송 기록을 읽을 수 없습니다 · 기록 확인 전 새 세션을 만들지 않습니다"),
    LEGACY("이전 기록에 좌표·대상이 없어 자동 복구할 수 없습니다 · 서버 기록을 확인하세요"),
    UNRESOLVED("기존 세션의 발송 결과를 먼저 확인하세요 · 새 ID로 재발송하지 않습니다"),
    INVALID("유효한 위치·단서·수신자·서버 주소와 동의가 필요합니다")
}

/** Frozen consent and payload. Never contains an API token or a phone number. */
data class ConfirmedArrival(
    val sessionId: String, val position: Position, val clue: String, val recipientId: String,
    val channel: DeliveryChannel, val endpoint: String, val source: InputSource,
    val confirmedAt: Long, val consentedAt: Long
) {
    fun valid(): Boolean = runCatching {
        require(UUID.fromString(sessionId).toString() == sessionId)
        require(position.latitude.isFinite() && position.latitude in -90.0..90.0)
        require(position.longitude.isFinite() && position.longitude in -180.0..180.0)
        require(clue.isNotBlank() && clue.length <= 180)
        require(recipientId.matches(Regex("[a-zA-Z0-9_-]{1,64}")))
        require(source == InputSource.VEHICLE || source == InputSource.TEST)
        require(consentedAt > 0 && confirmedAt >= consentedAt)
        if (channel == DeliveryChannel.LOCAL_MOCK) require(endpoint.isEmpty()) else {
            val uri = URI(endpoint)
            require(uri.host != null && uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null)
            require(uri.scheme == "https" || uri.scheme == "http" && uri.host in setOf("127.0.0.1", "localhost", "10.0.2.2"))
        }
    }.isSuccess

    /** arrival-v2: UTF-8 byte-length-prefixed fields, coordinates as big-endian IEEE754 hex. */
    fun fingerprint(): String {
        fun bits(value: Double): String = java.lang.Long.toUnsignedString(java.lang.Double.doubleToLongBits(if (value == 0.0) 0.0 else value), 16).padStart(16, '0')
        val fields = listOf("arrival-v2", sessionId, recipientId, bits(position.latitude), bits(position.longitude), clue, "true", "true", source.name, channel.serverMode)
        val digest = MessageDigest.getInstance("SHA-256")
        fields.forEach { field -> val b = field.toByteArray(Charsets.UTF_8); digest.update("${b.size}:".toByteArray(Charsets.US_ASCII)); digest.update(b) }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

data class ArrivalRecord(
    val sessionId: String = UUID.randomUUID().toString(), val request: ConfirmedArrival? = null,
    val status: DeliveryStatus = DeliveryStatus.DRAFT, val consentActive: Boolean = false,
    val serverObserved: Boolean = false, val legacyClue: String = ""
)
data class OutboxState(val record: ArrivalRecord, val busy: Boolean = false, val issue: DeliveryIssue = DeliveryIssue.NONE, val storageHealthy: Boolean = true)
data class DeliveryReceipt(val status: DeliveryStatus, val fingerprint: String, val mode: String, val source: InputSource)
sealed interface LookupResult { data class Found(val receipt: DeliveryReceipt) : LookupResult; data object Missing : LookupResult }
class DeliveryException(val issue: DeliveryIssue) : Exception(issue.name)
interface ArrivalTransport {
    fun lookup(request: ConfirmedArrival, token: String): LookupResult
    fun submit(request: ConfirmedArrival, token: String): DeliveryReceipt
}
interface ArrivalJournal { fun load(): ArrivalRecord?; fun save(record: ArrivalRecord) }

object ArrivalRecordCodec {
    fun encode(record: ArrivalRecord): String = JSONObject().apply {
        put("schema", 2); put("sessionId", record.sessionId); put("status", record.status.name)
        put("consentActive", record.consentActive); put("serverObserved", record.serverObserved); put("legacyClue", record.legacyClue)
        record.request?.let { r -> put("request", JSONObject().apply {
            put("sessionId", r.sessionId); put("latitude", r.position.latitude); put("longitude", r.position.longitude)
            put("clue", r.clue); put("recipientId", r.recipientId); put("channel", r.channel.name)
            put("endpoint", r.endpoint); put("source", r.source.name); put("confirmedAt", r.confirmedAt); put("consentedAt", r.consentedAt)
        }) }
    }.toString()
    fun decode(raw: String): ArrivalRecord {
        val j = JSONObject(raw)
        require(j.getInt("schema") == 2)
        val request = j.optJSONObject("request")?.let { r -> ConfirmedArrival(
            r.getString("sessionId"), Position(r.getDouble("latitude"), r.getDouble("longitude")), r.getString("clue"),
            r.getString("recipientId"), DeliveryChannel.valueOf(r.getString("channel")), r.getString("endpoint"),
            InputSource.valueOf(r.getString("source")), r.getLong("confirmedAt"), r.getLong("consentedAt")
        ).also { require(it.valid()) } }
        return ArrivalRecord(j.getString("sessionId"), request, DeliveryStatus.valueOf(j.getString("status")), j.getBoolean("consentActive"), j.getBoolean("serverObserved"), j.optString("legacyClue")).also {
            require(UUID.fromString(it.sessionId).toString() == it.sessionId)
            require(request == null || request.sessionId == it.sessionId && it.status != DeliveryStatus.DRAFT)
            require(request != null || it.status in setOf(DeliveryStatus.DRAFT, DeliveryStatus.MOCK, DeliveryStatus.ACCEPTED, DeliveryStatus.LEGACY_REVIEW))
        }
    }
    fun migrateLegacy(raw: String): ArrivalRecord {
        val j = JSONObject(raw); val label = j.getString("notification")
        val status = when {
            label == "대기" -> DeliveryStatus.DRAFT
            label.startsWith("모의 발송 완료") || label.startsWith("서버 모의 발송 완료") -> DeliveryStatus.MOCK
            label.startsWith("공급자 수락") -> DeliveryStatus.ACCEPTED
            else -> DeliveryStatus.LEGACY_REVIEW
        }
        return ArrivalRecord(sessionId = j.getString("id"), status = status, legacyClue = j.optString("clue")).also { UUID.fromString(it.sessionId) }
    }
}

/** Same-directory atomic replacement: a failed write never replaces the previous record. */
class FileArrivalJournal(private val file: File, private val legacy: () -> String? = { null }) : ArrivalJournal {
    override fun load(): ArrivalRecord? {
        if (file.exists()) return ArrivalRecordCodec.decode(file.readText(Charsets.UTF_8))
        return legacy()?.let { ArrivalRecordCodec.migrateLegacy(it).also(::save) }
    }
    override fun save(record: ArrivalRecord) {
        val directory = file.parentFile!!
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create journal directory" }
        val temp = File.createTempFile("arrival-", ".tmp", directory)
        try {
            FileOutputStream(temp).use { stream -> stream.write(ArrivalRecordCodec.encode(record).toByteArray(Charsets.UTF_8)); stream.fd.sync() }
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { temp.delete() }
    }
}

/** Serializes network reconciliation; the short journal lock is never held during HTTP. */
class ArrivalOutbox(private val journal: ArrivalJournal, private val transport: ArrivalTransport, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val guard = Any()
    private val network = Mutex()
    private val initial = runCatching { journal.load() ?: ArrivalRecord() }
    private val _state = MutableStateFlow(OutboxState(initial.getOrDefault(ArrivalRecord()), issue = if (initial.isFailure) DeliveryIssue.CORRUPT else DeliveryIssue.NONE, storageHealthy = initial.isSuccess))
    val state = _state.asStateFlow()

    private fun save(record: ArrivalRecord): Boolean = try {
        journal.save(record)
        _state.value = _state.value.copy(record = record, issue = if (record.request != null && !record.consentActive && !record.status.terminal) DeliveryIssue.CONSENT_REVOKED else DeliveryIssue.NONE)
        true
    } catch (_: Exception) { _state.value = _state.value.copy(issue = DeliveryIssue.STORAGE); false }
    private fun issue(value: DeliveryIssue) { synchronized(guard) { _state.value = _state.value.copy(issue = value) } }

    fun prepare(request: ConfirmedArrival): Boolean = synchronized(guard) {
        val s = _state.value
        if (!s.storageHealthy) return false
        if (!request.valid() || request.sessionId != s.record.sessionId) { issue(DeliveryIssue.INVALID); return false }
        if (s.record.status != DeliveryStatus.DRAFT || s.record.request != null) { issue(DeliveryIssue.UNRESOLVED); return false }
        save(ArrivalRecord(request.sessionId, request, DeliveryStatus.QUEUED, consentActive = true))
    }
    fun newSession(): Boolean = synchronized(guard) {
        val s = _state.value
        if (!s.storageHealthy) return false
        if (!s.record.status.terminal && s.record.status != DeliveryStatus.DRAFT) { issue(DeliveryIssue.UNRESOLVED); return false }
        save(ArrivalRecord())
    }
    fun revokeConsent(): Boolean = synchronized(guard) {
        if (!_state.value.storageHealthy) return false
        save(_state.value.record.copy(consentActive = false))
    }

    suspend fun recover(token: String) = network.withLock {
        withContext(io) {
            val current = _state.value
            if (!current.storageHealthy) return@withContext
            val request = current.record.request ?: run {
                if (current.record.status == DeliveryStatus.LEGACY_REVIEW) issue(DeliveryIssue.LEGACY)
                return@withContext
            }
            if (!request.valid()) { issue(DeliveryIssue.INVALID); return@withContext }
            synchronized(guard) { _state.value = _state.value.copy(busy = true) }
            try {
                if (request.channel == DeliveryChannel.LOCAL_MOCK) {
                    synchronized(guard) {
                        val record = _state.value.record
                        if (record.sessionId == request.sessionId && record.consentActive && record.status == DeliveryStatus.QUEUED) save(record.copy(status = DeliveryStatus.MOCK))
                    }
                } else if (token.isBlank()) issue(DeliveryIssue.TOKEN_REQUIRED)
                else when (val result = transport.lookup(request, token)) {
                    is LookupResult.Found -> accept(request, result.receipt)
                    LookupResult.Missing -> {
                        val canSubmit = synchronized(guard) {
                            val record = _state.value.record
                            when {
                                record.sessionId != request.sessionId -> false
                                record.serverObserved || record.status.terminal -> { issue(DeliveryIssue.RESULT_MISSING); false }
                                !record.consentActive -> { issue(DeliveryIssue.CONSENT_REVOKED); false }
                                record.status !in setOf(DeliveryStatus.QUEUED, DeliveryStatus.UNKNOWN) -> false
                                else -> save(record.copy(status = DeliveryStatus.QUEUED))
                            }
                        }
                        if (canSubmit) accept(request, transport.submit(request, token))
                    }
                }
            } catch (e: Exception) {
                if (e is DeliveryException && e.issue == DeliveryIssue.MISMATCH) synchronized(guard) {
                    if (_state.value.record.sessionId == request.sessionId) save(_state.value.record.copy(serverObserved = true))
                }
                issue(if (e is DeliveryException) e.issue else DeliveryIssue.NETWORK)
            } finally { synchronized(guard) { _state.value = _state.value.copy(busy = false) } }
        }
    }
    private fun accept(request: ConfirmedArrival, receipt: DeliveryReceipt): Unit = synchronized(guard) {
        if (_state.value.record.sessionId != request.sessionId) return
        if (receipt.fingerprint != request.fingerprint() || receipt.mode != request.channel.serverMode || receipt.source != request.source ||
            receipt.status !in setOf(DeliveryStatus.PENDING, DeliveryStatus.UNKNOWN, DeliveryStatus.MOCK, DeliveryStatus.ACCEPTED, DeliveryStatus.FAILED) ||
            receipt.status == DeliveryStatus.MOCK && receipt.mode != "mock" || receipt.status == DeliveryStatus.ACCEPTED && receipt.mode != "sens") {
            save(_state.value.record.copy(serverObserved = true))
            issue(DeliveryIssue.MISMATCH); return
        }
        save(_state.value.record.copy(status = receipt.status, serverObserved = true))
        Unit
    }
}
