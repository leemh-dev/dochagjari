package dev.leemh.dochagjari

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GuidanceState(
    val loading: Boolean = true, val saving: Boolean = false, val catalogVersion: String = "",
    val matches: List<IndoorDestination> = emptyList(), val visit: VisitRecord? = null,
    val visitDestination: IndoorDestination? = null, val error: String = ""
)

/** Application-owned serialized IO. Success is published only after the transaction completes. */
class VisitGuidanceRepository(private val storage: GuidanceStorage, private val scope: CoroutineScope, private val seed: () -> String) {
    private data class Command(val saving: Boolean, val operation: () -> Unit)
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(GuidanceState())
    val state = _state.asStateFlow()
    private var catalog: FacilityCatalog? = null
    init {
        scope.launch {
            for ((saving, operation) in commands) {
                _state.value = _state.value.copy(saving = saving, error = "")
                try { operation() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { _state.value = _state.value.copy(error = if (saving) "방문 저장 실패 · 다시 시도하세요" else "매장 DB 조회 실패 · 다시 조회하세요") }
                finally { _state.value = _state.value.copy(loading = false, saving = false) }
            }
        }
        action { initialize() }
    }
    private fun initialize() {
        if (catalog == null) catalog = storage.installCatalog(seed())
        val visit = storage.currentVisit()
        _state.value = _state.value.copy(loading = false, catalogVersion = catalog!!.version, visit = visit,
            visitDestination = visit?.let { storage.destination(it.catalogVersion, it.destinationId) })
    }
    private fun action(saving: Boolean = false, operation: () -> Unit) {
        check(commands.trySend(Command(saving, operation)).isSuccess)
    }
    private suspend fun <T> transaction(operation: () -> T): T {
        val result = CompletableDeferred<T>()
        action(saving = true) {
            try { result.complete(operation()) }
            catch (e: Exception) { result.completeExceptionally(e); throw e }
        }
        return result.await()
    }
    private fun current(id: String): VisitRecord {
        initialize()
        return requireNotNull(storage.currentVisit()).also { require(it.id == id) }
    }
    private fun persist(v: VisitRecord) { storage.saveVisit(v); _state.value = _state.value.copy(visit = v) }
    suspend fun prepareRoad(visitId: String, target: RoadTarget, channel: RoadChannel): RoadAttempt = transaction {
        val v = current(visitId)
        require(storage.destination(v.catalogVersion, v.destinationId)?.facilityId == target.facilityId)
        VisitJourneyRules.prepare(v, target, channel).also(::persist).journey.latest!!
    }
    suspend fun claimRoad(visitId: String, requestId: String): RoadAttempt? = transaction {
        VisitJourneyRules.claim(current(visitId), requestId)?.also(::persist)?.journey?.latest
    }
    fun roadEvent(visitId: String, requestId: String, status: RoadStatus, kind: String, detail: String) = action(saving = true) {
        persist(VisitJourneyRules.event(current(visitId), requestId, status, kind, detail))
    }
    fun cancelRoad(visitId: String) = action(saving = true) { persist(VisitJourneyRules.cancel(current(visitId))) }
    suspend fun enter(visitId: String, eventId: String, source: GuidanceInputSource): Boolean = transaction {
        val (v, created) = VisitJourneyRules.enter(current(visitId), eventId, source)
        if (created) persist(v)
        created
    }
    suspend fun beginObservation(visitId: String, source: VisitFrameSource): VisitScan = transaction {
        VisitObservationRules.begin(current(visitId), source).also(::persist).observation.latest!!
    }
    fun appendObservation(visitId: String, runId: String, sample: VisitSample) = action(saving = true) {
        persist(VisitObservationRules.append(current(visitId), runId, sample))
    }
    fun stopObservation(visitId: String, runId: String, reason: String) = action(saving = true) {
        persist(VisitObservationRules.stop(current(visitId), runId, reason))
    }
    fun search(query: String) = action {
        initialize()
        _state.value = _state.value.copy(matches = storage.search(catalog!!.version, query))
    }
    fun prepare(destinationId: String) = action(saving = true) {
        initialize()
        val saved = storage.beginOrResume(catalog!!.version, destinationId)
        _state.value = _state.value.copy(visit = saved, visitDestination = storage.destination(saved.catalogVersion, saved.destinationId))
    }
    fun setVisibility(visibility: GuideVisibility) = action(saving = true) {
        initialize()
        val current = requireNotNull(storage.currentVisit()) { "No prepared visit" }
        val saved = current.copy(visibility = visibility)
        storage.saveVisit(saved)
        _state.value = _state.value.copy(visit = saved)
    }

}
