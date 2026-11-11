package dev.leemh.dochagjari

import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Application.profileStore by preferencesDataStore("profile")
data class AppState(
    val facilities: List<Facility> = emptyList(), val facilityIndex: Int = 0,
    val mode: VisitMode = VisitMode.PICKUP, val profile: Profile = Profile(),
    val vehicle: VehicleSnapshot = VehicleSnapshot(), val testVehicle: Boolean = true, val testNavi: Boolean = true,
    val blocked: Set<String> = emptySet(), val observations: Map<String, Observation> = emptyMap(),
    val recommendation: Recommendation = Recommendation(null, "준비 중", emptyList()),
    val naviStatus: String = "모의 내비 선택됨", val session: PickupSession = PickupSession(),
    val clue: String = "", val latitude: String = "", val longitude: String = "",
    val analysis: String = "영상을 선택하거나 동봉 샘플을 분석하세요", val analyzing: Boolean = false,
    val useServer: Boolean = false, val serverUrl: String = "http://10.0.2.2:8000", val token: String = "", val recipientId: String = "demo",
    val consent: Boolean = false, val error: String = "", val log: List<String> = emptyList()
) { val facility: Facility get() = facilities[facilityIndex] }

class AppModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val journal = app.getSharedPreferences("session-journal", 0)
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()
    private var navi: NavigationGateway? = null
    private var vehicleSource: VehicleStateSource? = null
    private var testSource: TestVehicleStateSource? = null
    private var sourceGeneration = 0
    private val recommender = Recommender()
    private val analyzer by lazy { VideoSceneAnalyzer(app) }
    init {
        val facilities = FacilityParser.parse(app.assets.open("facilities.json").bufferedReader().use { it.readText() })
        val restored = journal.getString("session", null)?.let { raw -> runCatching {
            val j = JSONObject(raw)
            PickupSession(id = j.getString("id"), notification = j.getString("notification"), clue = j.optString("clue"))
        }.getOrNull() } ?: PickupSession()
        _state.update { it.copy(facilities = facilities, session = restored) }
        recompute(); selectVehicle(true); selectNavi(true)
        viewModelScope.launch {
            app.profileStore.data.collect { p ->
                _state.update { it.copy(profile = it.profile.copy(leftDoor = p[booleanPreferencesKey("left")] ?: false, rightDoor = p[booleanPreferencesKey("right")] ?: false, stroller = p[booleanPreferencesKey("stroller")] ?: false, wheelchair = p[booleanPreferencesKey("wheelchair")] ?: false, companion = p[booleanPreferencesKey("companion")] ?: false, heightM = p[doublePreferencesKey("height")] ?: 1.8)) }
                recompute()
            }
        }
        viewModelScope.launch { while (true) { delay(5_000); recompute() } }
    }
    private fun log(message: String) { _state.update { it.copy(log = (listOf(message) + it.log).take(24)) }; android.util.Log.i("Dochagjari", message) }
    private fun recompute() { _state.update { it.copy(recommendation = recommender.recommend(it.facility, it.mode, it.profile, it.blocked, it.observations)) } }
    fun chooseFacility(index: Int) { _state.update { it.copy(facilityIndex = index, mode = if (index == 0) VisitMode.PICKUP else VisitMode.DROPOFF, blocked = emptySet(), clue = "", latitude = "", longitude = "") }; newSession(); recompute() }
    fun mode(mode: VisitMode) { _state.update { it.copy(mode = mode) }; recompute() }
    fun profile(profile: Profile) {
        if (_state.value.vehicle.stopped != true) return
        _state.update { it.copy(profile = profile) }; recompute()
        viewModelScope.launch { app.profileStore.edit { p ->
            mapOf("left" to profile.leftDoor, "right" to profile.rightDoor, "stroller" to profile.stroller, "wheelchair" to profile.wheelchair, "companion" to profile.companion).forEach { (k,v) -> p[booleanPreferencesKey(k)] = v }
            p[doublePreferencesKey("height")] = profile.heightM
        } }
    }
    fun blocked() { _state.value.recommendation.point?.let { p -> _state.update { it.copy(blocked = it.blocked + p.id) }; recompute() } }
    fun resetBlocked() { _state.update { it.copy(blocked = emptySet()) }; recompute() }
    fun selectNavi(test: Boolean) {
        navi?.close(); _state.update { it.copy(testNavi = test) }
        val report: (String) -> Unit = { message -> _state.update { it.copy(naviStatus = message) }; log(message) }
        navi = if (test) TestNavigationGateway(report) else PleosNavigationGateway(app, report)
    }
    fun navigate() { _state.value.recommendation.point?.let { navi?.route(it) } }
    fun selectVehicle(test: Boolean) {
        sourceGeneration++; val generation = sourceGeneration
        vehicleSource?.close(); _state.update { it.copy(testVehicle = test, vehicle = VehicleSnapshot()) }
        val emit: (VehicleSnapshot) -> Unit = { snapshot ->
            viewModelScope.launch {
                if (generation == sourceGeneration) {
                    val s = _state.value.session
                    _state.update { it.copy(vehicle = snapshot, session = s.copy(stopped = snapshot.stopped == true, confirmed = if (snapshot.stopped == true) s.confirmed else false, source = snapshot.source)) }
                    maybeNotify()
                }
            }
        }
        if (test) { testSource = TestVehicleStateSource(emit); vehicleSource = testSource }
        else { testSource = null; vehicleSource = PleosVehicleStateSource(app, emit) }
        vehicleSource?.start()
    }
    fun testSeat(left: Boolean, occupancy: Occupancy) { if (left) testSource?.set(left = occupancy) else testSource?.set(right = occupancy) }
    fun testStopped(stopped: Boolean?) { testSource?.set(stopped = stopped) }
    fun testUnavailable(status: String) { testSource?.set(left = Occupancy.UNKNOWN, right = Occupancy.UNKNOWN, stopped = null, status = "테스트 입력 · $status") }
    fun editLocation(clue: String = _state.value.clue, latitude: String = _state.value.latitude, longitude: String = _state.value.longitude) {
        if (_state.value.vehicle.stopped != true) return
        _state.update { it.copy(clue = clue, latitude = latitude, longitude = longitude, session = it.session.copy(confirmed = false)) }
    }
    fun fillDemoPosition() {
        val p = _state.value.recommendation.point ?: return
        editLocation(clue = "${_state.value.facility.name}, ${p.name}, B1 C12 기둥 옆 (시연 입력)", latitude = p.position.latitude.toString(), longitude = p.position.longitude.toString())
    }
    fun confirmLocation() {
        val s = _state.value
        if (s.vehicle.stopped != true) return
        val lat = s.latitude.toDoubleOrNull(); val lon = s.longitude.toDoubleOrNull()
        if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0 || s.clue.isBlank()) { _state.update { it.copy(error = "유효한 좌표와 만남 단서를 확인하세요") }; return }
        _state.update { it.copy(session = it.session.copy(stopped = true, confirmed = true, position = Position(lat, lon), clue = s.clue, source = s.vehicle.source), error = "") }
        maybeNotify()
    }
    fun configureNotify(useServer: Boolean = _state.value.useServer, url: String = _state.value.serverUrl, token: String = _state.value.token, recipient: String = _state.value.recipientId, consent: Boolean = _state.value.consent) {
        if (_state.value.vehicle.stopped != true) return
        _state.update { it.copy(useServer = useServer, serverUrl = url, token = token, recipientId = recipient, consent = consent) }
        maybeNotify()
    }
    private fun persist(session: PickupSession): Boolean = journal.edit().putString("session", JSONObject().put("id", session.id).put("notification", session.notification).put("clue", session.clue).toString()).commit()
    private fun maybeNotify() {
        val s = _state.value
        if (!s.session.eligible || !s.consent) return
        val pending = s.session.copy(notification = "발송 확인 필요 · 요청 중")
        if (!persist(pending)) { _state.update { it.copy(error = "발송 기록 저장 실패 · 미발송") }; return }
        _state.update { it.copy(session = pending) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                (if (s.useServer) HttpArrivalNotifier(s.serverUrl, s.token) else MockArrivalNotifier()).notify(pending, s.recipientId)
            }.getOrElse { "발송 확인 필요 · ${it.javaClass.simpleName}" } }
            val done = pending.copy(notification = result)
            // Do not overwrite a newer visit when an old HTTP request finishes.
            if (_state.value.session.id == done.id) { persist(done); _state.update { it.copy(session = it.session.copy(notification = result)) } }
            log("알림 ${done.id}: $result")
        }
    }
    fun newSession() {
        val session = PickupSession(stopped = _state.value.vehicle.stopped == true, source = _state.value.vehicle.source)
        if (persist(session)) _state.update { it.copy(session = session, clue = "", latitude = "", longitude = "") }
    }
    fun analyze(uri: Uri?, asset: String? = null, source: InputSource = InputSource.VIDEO) {
        val s = _state.value
        if (s.vehicle.stopped != true || s.analyzing) return
        val point = s.recommendation.point ?: return
        _state.update { it.copy(analyzing = true, analysis = "${source.label} · 실제 모델 분석 중") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { analyzer.analyze(uri, asset, source) } }
            result.onSuccess { analysis ->
                _state.update { it.copy(observations = it.observations + (point.id to analysis.observation), analysis = "${source.label} · ${point.name}\n원본 관측 ${analysis.observation.occupancy} (${analysis.observation.confidence})\n${analysis.detail}", analyzing = false, clue = if (it.vehicle.stopped == true && it.session.id == s.session.id && analysis.clue.isNotBlank()) analysis.clue + " · 사용자 확인 필요" else it.clue, session = it.session.copy(confirmed = false)) }
                recompute(); log("영상 분석: ${analysis.observation}")
            }.onFailure { e -> _state.update { it.copy(analyzing = false, analysis = "분석 실패 · 미확인: ${e.message}") } }
        }
    }
    fun testObservation(occupancy: Occupancy, stale: Boolean = false, low: Boolean = false) {
        val point = _state.value.recommendation.point ?: return
        val o = Observation(occupancy, if (low) .3f else .95f, System.currentTimeMillis() - if (stale) 60_000 else 0, InputSource.TEST)
        _state.update { it.copy(observations = it.observations + (point.id to o), analysis = "테스트 상태 입력 · 영상 분석 결과 아님: ${o.effective(System.currentTimeMillis())}") }; recompute()
    }
    override fun onCleared() { sourceGeneration++; navi?.close(); vehicleSource?.close(); super.onCleared() }
}
