package dev.leemh.dochagjari

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ProfileSaveStatus(val label: String) { LOADING("설정 불러오는 중"), SAVING("설정 저장 중"), SAVED("설정 저장 완료"), ERROR("설정 저장 실패 · 다시 저장하세요") }
data class ProfileState(val profile: Profile = Profile(), val persisted: Profile? = null, val revision: Long = 0, val savedRevision: Long = -1, val status: ProfileSaveStatus = ProfileSaveStatus.LOADING)
interface ProfileStorage { suspend fun read(): Profile; suspend fun write(profile: Profile) }

class DataStoreProfileStorage(private val store: DataStore<Preferences>) : ProfileStorage {
    companion object {
        fun createStore(context: Context, scope: CoroutineScope) = PreferenceDataStoreFactory.create(scope = scope, produceFile = { context.preferencesDataStoreFile("profile") })
        fun create(context: Context, scope: CoroutineScope) = DataStoreProfileStorage(createStore(context, scope))
        fun decode(p: Preferences): Profile = Profile(p[booleanPreferencesKey("left")] ?: false, p[booleanPreferencesKey("right")] ?: false,
            p[booleanPreferencesKey("stroller")] ?: false, p[booleanPreferencesKey("wheelchair")] ?: false,
            p[booleanPreferencesKey("companion")] ?: false, p[doublePreferencesKey("height")] ?: 1.8,
            SeatPreferences(enumValue(p[stringPreferencesKey("seat_setup")], SeatSetup.NOT_SET),
                SeatAssignment(enumValue(p[stringPreferencesKey("seat_left_role")], SeatRole.UNSPECIFIED), p[booleanPreferencesKey("seat_left_child_seat")] ?: false),
                SeatAssignment(enumValue(p[stringPreferencesKey("seat_right_role")], SeatRole.UNSPECIFIED), p[booleanPreferencesKey("seat_right_child_seat")] ?: false)))
        private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T = enumValues<T>().firstOrNull { it.name == raw } ?: fallback
    }
    override suspend fun read(): Profile = decode(store.data.first())
    override suspend fun write(profile: Profile) {
        store.edit { p ->
            mapOf("left" to profile.leftDoor, "right" to profile.rightDoor, "stroller" to profile.stroller,
                "wheelchair" to profile.wheelchair, "companion" to profile.companion).forEach { (key, value) -> p[booleanPreferencesKey(key)] = value }
            p[doublePreferencesKey("height")] = profile.heightM
            p[intPreferencesKey("seat_schema")] = 1
            p[stringPreferencesKey("seat_setup")] = profile.seats.setup.name
            p[stringPreferencesKey("seat_left_role")] = profile.seats.left.role.name
            p[stringPreferencesKey("seat_right_role")] = profile.seats.right.role.name
            p[booleanPreferencesKey("seat_left_child_seat")] = profile.seats.left.childSeat
            p[booleanPreferencesKey("seat_right_child_seat")] = profile.seats.right.childSeat
        }
    }
}

/** One ordered writer owned by the application, not by a screen's ViewModel. */
class ProfileRepository(private val storage: ProfileStorage, scope: CoroutineScope) {
    private val guard = Any()
    private val writes = Channel<Pair<Long, Profile>>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()
    init {
        scope.launch {
            try {
                val saved = storage.read()
                synchronized(guard) {
                    val s = _state.value
                    _state.value = if (s.revision == 0L) s.copy(profile = saved, persisted = saved, savedRevision = 0, status = ProfileSaveStatus.SAVED)
                    else s.copy(persisted = saved, savedRevision = 0)
                }
            } catch (_: Exception) {
                synchronized(guard) { if (_state.value.revision == 0L) _state.value = _state.value.copy(status = ProfileSaveStatus.ERROR) }
            }
            for ((revision, profile) in writes) {
                try {
                    storage.write(profile)
                    synchronized(guard) {
                        val s = _state.value
                        _state.value = s.copy(persisted = profile, savedRevision = revision, status = if (s.revision == revision) ProfileSaveStatus.SAVED else ProfileSaveStatus.SAVING)
                    }
                } catch (_: Exception) {
                    synchronized(guard) { if (_state.value.revision == revision) _state.value = _state.value.copy(status = ProfileSaveStatus.ERROR) }
                }
            }
        }
    }
    fun update(profile: Profile) = synchronized(guard) {
        if (!profile.heightM.isFinite() || profile.heightM !in 1.0..4.0) {
            _state.value = _state.value.copy(status = ProfileSaveStatus.ERROR)
            return@synchronized
        }
        val revision = _state.value.revision + 1
        _state.value = _state.value.copy(profile = profile, revision = revision, status = ProfileSaveStatus.SAVING)
        if (!writes.trySend(revision to profile).isSuccess) _state.value = _state.value.copy(status = ProfileSaveStatus.ERROR)
    }
    suspend fun readStoredProfile(): Profile = storage.read()
    fun retry() = update(_state.value.profile)
}
