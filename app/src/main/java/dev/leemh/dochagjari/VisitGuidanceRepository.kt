package dev.leemh.dochagjari

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GuidanceState(
    val loading: Boolean = true, val saving: Boolean = false, val catalogVersion: String = "",
    val matches: List<IndoorDestination> = emptyList(), val visit: VisitRecord? = null,
    val visitDestination: IndoorDestination? = null, val error: String = ""
)

/** Application-owned serialized IO. Success is published only after the transaction completes. */
class VisitGuidanceRepository(private val storage: GuidanceStorage, private val scope: CoroutineScope, private val seed: () -> String) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(GuidanceState())
    val state = _state.asStateFlow()
    private var catalog: FacilityCatalog? = null
    init { action { initialize() } }
    private fun initialize() {
        if (catalog == null) catalog = storage.installCatalog(seed())
        val visit = storage.currentVisit()
        _state.value = _state.value.copy(loading = false, catalogVersion = catalog!!.version, visit = visit,
            visitDestination = visit?.let { storage.destination(it.catalogVersion, it.destinationId) })
    }
    private fun action(saving: Boolean = false, operation: () -> Unit) {
        scope.launch {
            mutex.withLock {
                _state.value = _state.value.copy(saving = saving, error = "")
                try { operation() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { _state.value = _state.value.copy(error = if (saving) "방문 저장 실패 · 다시 시도하세요" else "매장 DB 조회 실패 · 다시 조회하세요") }
                finally { _state.value = _state.value.copy(loading = false, saving = false) }
            }
        }
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
}
