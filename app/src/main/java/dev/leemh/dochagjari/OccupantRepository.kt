package dev.leemh.dochagjari

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface OccupantStorage { suspend fun read(): OccupantBook; suspend fun write(book: OccupantBook) }

/** One shared DataStore instance; atomically add one key without rewriting old preferences. */
class DataStoreOccupantStorage(private val store: DataStore<Preferences>) : OccupantStorage {
    private val key = stringPreferencesKey("occupant_book_v1")
    override suspend fun read(): OccupantBook {
        var result: OccupantBook? = null
        store.edit { values ->
            result = values[key]?.let(OccupantCodec::decode) ?: OccupantBook.migrate(DataStoreProfileStorage.decode(values)).also {
                values[key] = OccupantCodec.encode(it)
            }
        }
        return requireNotNull(result)
    }
    override suspend fun write(book: OccupantBook) { val raw = OccupantCodec.encode(book); store.edit { it[key] = raw } }
}

data class OccupantState(val book: OccupantBook = OccupantBook.migrate(Profile()), val persisted: OccupantBook? = null,
    val status: ProfileSaveStatus = ProfileSaveStatus.LOADING, val revision: Long = 0, val savedRevision: Long = -1,
    val confirmedThisRun: Boolean = false, val issue: String = "") {
    val saved: Boolean get() = status == ProfileSaveStatus.SAVED && revision == savedRevision
    val active: ActiveOccupants? get() = if (saved && confirmedThisRun && persisted?.skipped == false) persisted.active else null
}

class OccupantRepository(private val storage: OccupantStorage, scope: CoroutineScope) {
    private data class Write(val revision: Long, val book: OccupantBook, val confirm: Boolean)
    private val guard = Any()
    private val requests = Channel<Write?>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(OccupantState())
    val state = _state.asStateFlow()
    private var confirmed: Pair<String, Long>? = null
    private var pendingConfirmation: Pair<String, Long>? = null
    init {
        requests.trySend(null)
        scope.launch {
            for (request in requests) {
                try {
                    if (request == null) {
                        val book = storage.read()
                        synchronized(guard) { _state.value = OccupantState(book, book, ProfileSaveStatus.SAVED, savedRevision = 0) }
                    } else {
                        storage.write(request.book)
                        val actual = storage.read()
                        check(actual == request.book) { "Occupant readback mismatch" }
                        synchronized(guard) {
                            if (request.confirm) confirmed = actual.active?.let { it.id to it.confirmedAt }
                            val s = _state.value
                            _state.value = s.copy(persisted = actual, savedRevision = request.revision,
                                status = if (s.revision == request.revision) ProfileSaveStatus.SAVED else ProfileSaveStatus.SAVING,
                                confirmedThisRun = actual.active?.let { (it.id to it.confirmedAt) == confirmed } == true, issue = "")
                        }
                    }
                } catch (_: Exception) {
                    synchronized(guard) {
                        if (request == null || _state.value.revision == request.revision)
                            _state.value = _state.value.copy(status = ProfileSaveStatus.ERROR, issue = "탑승 구성 저장·재확인 실패 · 이전 저장값은 유지돼요")
                    }
                }
            }
        }
    }
    private fun update(confirm: Boolean = false, change: (OccupantBook) -> OccupantBook): Boolean = synchronized(guard) {
        val s = _state.value
        if (s.persisted == null || s.status == ProfileSaveStatus.LOADING) return@synchronized false
        val book = try { change(s.book).validate() } catch (_: IllegalArgumentException) { return@synchronized false }
        val rev = s.revision + 1
        if (confirm) pendingConfirmation = book.active?.let { it.id to it.confirmedAt }
        _state.value = s.copy(book = book, revision = rev, status = ProfileSaveStatus.SAVING, issue = "")
        if (!requests.trySend(Write(rev, book, confirm)).isSuccess) {
            _state.value = _state.value.copy(status = ProfileSaveStatus.ERROR); return@synchronized false
        }
        true
    }
    fun savePreset(preset: OccupantPreset) = update { book ->
        book.copy(presets = book.presets.filterNot { it.id == preset.id } + preset.copy(name = preset.name.trim()))
    }
    fun deletePreset(id: String) = update { it.copy(presets = it.presets.filterNot { p -> p.id == id }) }
    fun activate(preset: OccupantPreset, source: OccupantInput = OccupantInput.PRESET, now: Long = System.currentTimeMillis()) =
        update(true) { it.select(preset, source, now) }
    fun endTrip() = update { it.copy(active = null) }
    fun skip() = update { it.copy(skipped = true, active = null) }
    fun retry() = synchronized(guard) {
        if (_state.value.persisted == null) { _state.value = _state.value.copy(status = ProfileSaveStatus.LOADING); requests.trySend(null) }
        else update(_state.value.book.active?.let { (it.id to it.confirmedAt) == pendingConfirmation } == true) { it }
    }
    suspend fun readStored(): OccupantBook = storage.read()
}
