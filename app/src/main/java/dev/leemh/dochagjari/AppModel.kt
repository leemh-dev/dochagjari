package dev.leemh.dochagjari

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val consent: Boolean = false, val error: String = "", val log: List<String> = emptyList(),
    val sensServer: Boolean = false, val delivery: OutboxState = OutboxState(ArrivalRecord()),
    val profileSave: ProfileState = ProfileState()
) { val facility: Facility get() = facilities[facilityIndex] }

class AppModel @JvmOverloads constructor(application: Application, private val services: AppServices = AppServices.get(application)) : AndroidViewModel(application) {
    private val app = application
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()
    private var navi: NavigationGateway? = null
    private var vehicleSource: VehicleStateSource? = null
    private var testSource: TestVehicleStateSource? = null
    private var sourceGeneration = 0
    private var consentedAt = 0L
    private val recommender = Recommender()
    private val analyzer by lazy { VideoSceneAnalyzer(app) }
    init {
        val facilities = FacilityParser.parse(app.assets.open("facilities.json").bufferedReader().use { it.readText() })
        _state.update { it.copy(facilities = facilities) }
        applyOutbox(services.outbox.state.value)
        recompute(); selectVehicle(true); selectNavi(true)
        viewModelScope.launch {
            services.profiles.state.collect { saved ->
                _state.update { it.copy(profile = saved.profile, profileSave = saved) }
                recompute()
            }
        }
        viewModelScope.launch { services.outbox.state.collect(::applyOutbox) }
        viewModelScope.launch { while (true) { delay(5_000); recompute() } }
    }
    private fun log(message: String) { _state.update { it.copy(log = (listOf(message) + it.log).take(24)) }; android.util.Log.i("Dochagjari", message) }
    private fun recompute() { _state.update { it.copy(recommendation = recommender.recommend(it.facility, it.mode, it.profile, it.blocked, it.observations)) } }
    private fun applyOutbox(delivery: OutboxState) {
        val record = delivery.record; val request = record.request
        _state.update { s ->
            val changedSession = s.session.id != record.sessionId
            val session = if (request != null) PickupSession(record.sessionId, s.vehicle.stopped == true, true, request.position, request.clue, record.status, request.source)
                else if (changedSession) PickupSession(id = record.sessionId, stopped = s.vehicle.stopped == true, clue = record.legacyClue, notificationStatus = record.status, source = s.vehicle.source)
                else s.session.copy(notificationStatus = record.status)
            s.copy(delivery = delivery, session = session,
                clue = request?.clue ?: if (changedSession) record.legacyClue else s.clue,
                latitude = request?.position?.latitude?.toString() ?: if (changedSession) "" else s.latitude,
                longitude = request?.position?.longitude?.toString() ?: if (changedSession) "" else s.longitude,
                consent = if (request != null) record.consentActive else if (changedSession) false else s.consent,
                useServer = request?.let { it.channel != DeliveryChannel.LOCAL_MOCK } ?: s.useServer,
                sensServer = request?.let { it.channel == DeliveryChannel.SENS } ?: s.sensServer,
                serverUrl = request?.takeIf { it.channel != DeliveryChannel.LOCAL_MOCK }?.endpoint ?: s.serverUrl,
                recipientId = request?.recipientId ?: s.recipientId)
        }
    }
    fun chooseFacility(index: Int) {
        if (index !in _state.value.facilities.indices || index == _state.value.facilityIndex) return
        if (!newSession()) return
        _state.update { it.copy(facilityIndex = index, mode = if (index == 0) VisitMode.PICKUP else VisitMode.DROPOFF, blocked = emptySet()) }; recompute()
    }
    fun mode(mode: VisitMode) { _state.update { it.copy(mode = mode) }; recompute() }
    fun profile(profile: Profile) {
        if (_state.value.vehicle.stopped != true) return
        services.profiles.update(profile)
        _state.update { it.copy(profile = services.profiles.state.value.profile, profileSave = services.profiles.state.value) }; recompute()
    }
    fun retryProfile() { if (_state.value.vehicle.stopped == true) services.profiles.retry() }
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
                    _state.update { it.copy(vehicle = snapshot, session = s.copy(stopped = snapshot.stopped == true, confirmed = if (snapshot.stopped == true) s.confirmed else false, source = services.outbox.state.value.record.request?.source ?: snapshot.source)) }
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
        if (_state.value.vehicle.stopped != true || services.outbox.state.value.record.request != null) return
        _state.update { it.copy(clue = clue, latitude = latitude, longitude = longitude, session = it.session.copy(confirmed = false)) }
    }
    fun fillDemoPosition() {
        val p = _state.value.recommendation.point ?: return
        editLocation(clue = "${_state.value.facility.name}, ${p.name}, B1 C12 기둥 옆 (시연 입력)", latitude = p.position.latitude.toString(), longitude = p.position.longitude.toString())
    }
    fun confirmLocation() {
        val s = _state.value
        if (s.vehicle.stopped != true || services.outbox.state.value.record.status != DeliveryStatus.DRAFT) return
        val lat = s.latitude.toDoubleOrNull(); val lon = s.longitude.toDoubleOrNull()
        if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0 || s.clue.isBlank() || s.clue.length > 180) { _state.update { it.copy(error = "유효한 좌표와 180자 이내 만남 단서를 확인하세요") }; return }
        _state.update { it.copy(session = it.session.copy(stopped = true, confirmed = true, position = Position(lat, lon), clue = s.clue, source = s.vehicle.source), error = "") }
        maybeNotify()
    }
    fun configureNotify(useServer: Boolean = _state.value.useServer, url: String = _state.value.serverUrl, token: String = _state.value.token, recipient: String = _state.value.recipientId, consent: Boolean = _state.value.consent, sensServer: Boolean = _state.value.sensServer) {
        if (_state.value.vehicle.stopped != true) return
        val s = _state.value
        if (services.outbox.state.value.record.request != null) {
            _state.update { it.copy(token = token) }
            if (!consent && s.consent) { services.outbox.revokeConsent(); applyOutbox(services.outbox.state.value) }
            return
        }
        val changedTarget = useServer != s.useServer || url != s.serverUrl || recipient != s.recipientId || sensServer != s.sensServer
        val acceptedConsent = consent && !changedTarget
        if (acceptedConsent && !s.consent) consentedAt = System.currentTimeMillis()
        if (!acceptedConsent) consentedAt = 0
        _state.update { it.copy(useServer = useServer, serverUrl = url, token = token, recipientId = recipient, consent = acceptedConsent, sensServer = sensServer, session = if (changedTarget) it.session.copy(confirmed = false) else it.session) }
        if (acceptedConsent && !s.consent) maybeNotify()
    }
    private fun maybeNotify() {
        val s = _state.value
        if (!s.session.eligible || !s.consent || consentedAt == 0L || services.outbox.state.value.record.status != DeliveryStatus.DRAFT) return
        val channel = if (!s.useServer) DeliveryChannel.LOCAL_MOCK else if (s.sensServer) DeliveryChannel.SENS else DeliveryChannel.SERVER_MOCK
        val request = ConfirmedArrival(s.session.id, s.session.position!!, s.session.clue, s.recipientId, channel,
            if (s.useServer) s.serverUrl.trim().trimEnd('/') else "", s.session.source, System.currentTimeMillis().coerceAtLeast(consentedAt), consentedAt)
        if (services.outbox.prepare(request)) {
            applyOutbox(services.outbox.state.value)
            services.scope.launch { services.outbox.recover(s.token) }
        }
    }
    fun recoverNotification() {
        if (_state.value.vehicle.stopped != true) return
        val token = _state.value.token
        services.scope.launch { services.outbox.recover(token) }
    }
    fun newSession(): Boolean {
        if (_state.value.vehicle.stopped != true) return false
        val result = services.outbox.newSession()
        applyOutbox(services.outbox.state.value)
        if (result) { consentedAt = 0; _state.update { it.copy(consent = false, error = "") } }
        return result
    }
    fun analyze(uri: Uri?, asset: String? = null, source: InputSource = InputSource.VIDEO) {
        val s = _state.value
        if (s.vehicle.stopped != true || s.analyzing) return
        val point = s.recommendation.point ?: return
        _state.update { it.copy(analyzing = true, analysis = "${source.label} · 실제 모델 분석 중") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { analyzer.analyze(uri, asset, source) } }
            result.onSuccess { analysis ->
                _state.update { it.copy(observations = it.observations + (point.id to analysis.observation), analysis = "${source.label} · ${point.name}\n원본 관측 ${analysis.observation.occupancy} (${analysis.observation.confidence})\n${analysis.detail}", analyzing = false, clue = if (it.delivery.record.request == null && it.vehicle.stopped == true && it.session.id == s.session.id && analysis.clue.isNotBlank()) analysis.clue + " · 사용자 확인 필요" else it.clue, session = if (it.delivery.record.request == null) it.session.copy(confirmed = false) else it.session) }
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
