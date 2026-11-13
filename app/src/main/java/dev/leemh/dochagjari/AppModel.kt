package dev.leemh.dochagjari

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
enum class ParkingView { SUMMARY, RETURN }
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
    val observingVisit: Boolean = false, val visitObservationMessage: String = "관측 중지 · 자동 재개 없음", val observationTick: Long = 0,
    val parkingView: ParkingView? = null, val parkingSaving: Boolean = false, val parkingError: String = "", val parkingSaveMessage: String = "주차 완료 기록 없음",
    val parkingDraft: ParkingDetails = ParkingDetails(), val parkingDraftSource: GuidanceInputSource = GuidanceInputSource.USER,
    val parkingDraftRevision: Long = 0, val parkingDraftDirty: Boolean = false, val parkingSummarySeconds: Int = 8,
    val roadTarget: RoadTarget? = null, val roadStarting: Boolean = false,
    val profileSave: ProfileState = ProfileState(), val guidance: GuidanceState = GuidanceState(), val showIndoorGuide: Boolean = false
) { val facility: Facility get() = facilities[facilityIndex] }

class AppModel @JvmOverloads constructor(application: Application, private val services: AppServices = AppServices.get(application), private val visitFrameAnalyzer: VisitFrameAnalyzer? = null) : AndroidViewModel(application) {
    private val app = application
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()
    private var navi: NavigationGateway? = null
    private var visitRoad: VisitRoadNavigator? = null
    private var roadGeneration = 0
    private val scanGeneration = AtomicLong()
    private var scanJob: Job? = null
    private var scanRunId: String? = null
    private var scanVisitId: String? = null
    private var parkingViewGeneration = 0L
    private var parkingSequence = 0L
    private var parkingWrites = 0
    private var parkingRetry: (() -> Unit)? = null
    private var parkingSummaryJob: Job? = null
    private var movementEpoch = 0L
    private var lastMovement: Pair<InputSource, Long>? = null
    private var vehicleSource: VehicleStateSource? = null
    private var testSource: TestVehicleStateSource? = null
    private var sourceGeneration = 0
    private var consentedAt = 0L
    private val recommender = Recommender()
    private val analyzer by lazy { VideoSceneAnalyzer(app) }
    init {
        val facilities = FacilityParser.parse(app.assets.open("facilities.json").bufferedReader().use { it.readText() })
        _state.update { it.copy(facilities = facilities, roadTarget = RoadTarget.parse(org.json.JSONObject(app.assets.open("road-target.json").bufferedReader().use { r -> r.readText() }))) }
        applyOutbox(services.outbox.state.value)
        recompute(); selectVehicle(true); selectNavi(true)
        viewModelScope.launch {
            services.profiles.state.collect { saved ->
                _state.update { it.copy(profile = saved.profile, profileSave = saved) }
                recompute()
            }
        }
        viewModelScope.launch { services.outbox.state.collect(::applyOutbox) }
        services.guidance?.let { repository -> viewModelScope.launch { repository.state.collect { saved -> _state.update { it.copy(guidance = saved, parkingSaveMessage = if (parkingSequence == 0L && saved.visit?.parked?.latest != null) "저장 주차 기록 확인 · 자동 재개 없음" else it.parkingSaveMessage) } } } }
        viewModelScope.launch { while (true) { delay(5_000); recompute(); if (_state.value.guidance.visit?.observation?.latest != null) _state.update { it.copy(observationTick = System.currentTimeMillis()) } } }
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
    fun openIndoorGuide() {
        val s = _state.value
        if (s.vehicle.stopped != true || s.guidance.visit == null || s.guidance.visitDestination == null || s.parkingSaving || s.guidance.visit.phase == VisitPhase.CLOSED) return
        services.guidance?.setVisibility(GuideVisibility.VISIBLE)
        _state.update { it.copy(showIndoorGuide = true) }
    }
    fun closeIndoorGuide() {
        dismissParkingView()
        stopVisitObservation("지도/입력 소스 닫기")
        if (!_state.value.showIndoorGuide) return
        _state.update { it.copy(showIndoorGuide = false) }
        services.guidance?.setVisibility(GuideVisibility.HIDDEN)
    }
    fun requestVisitRoad() {
        val s = _state.value; val repo = services.guidance ?: return
        val visit = s.guidance.visit ?: return; val target = s.roadTarget ?: return
        if (s.vehicle.stopped != true || s.roadStarting) return
        val generation = roadGeneration
        _state.update { it.copy(roadStarting = true, error = "") }
        viewModelScope.launch {
            try {
                val prepared = repo.prepareRoad(visit.id, target, if (s.testNavi) RoadChannel.MOCK else RoadChannel.PLEOS)
                val claimed = repo.claimRoad(visit.id, prepared.id) ?: return@launch
                if (generation != roadGeneration || _state.value.vehicle.stopped != true) {
                    repo.roadEvent(visit.id, claimed.id, RoadStatus.INTERRUPTED, "NOT_DISPATCHED", "저장 후 상태 변경 · 외부 요청하지 않음")
                    return@launch
                }
                visitRoad?.close()
                if (claimed.channel == RoadChannel.PLEOS) { navi?.close(); navi = null }
                visitRoad = VisitRoadNavigator(app, claimed) { status, kind, detail ->
                    if (generation == roadGeneration) repo.roadEvent(visit.id, claimed.id, status, kind, detail)
                }.also { it.dispatch() }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = "방문 내비 연결 실패 · 저장 상태를 확인하세요") } }
            finally { _state.update { it.copy(roadStarting = false) } }
        }
    }
    fun cancelVisitRoad() {
        if (_state.value.vehicle.stopped != true) return
        roadGeneration++; visitRoad?.close(); visitRoad = null
        _state.value.guidance.visit?.let { services.guidance?.cancelRoad(it.id) }
    }
    fun confirmVisitEntry(test: Boolean, eventId: String = "entry-" + java.util.UUID.randomUUID()) {
        val s = _state.value; val repo = services.guidance ?: return; val visit = s.guidance.visit ?: return
        if (s.vehicle.stopped != true || test && !s.testVehicle) return
        viewModelScope.launch {
            try {
                val created = repo.enter(visit.id, eventId, if (test) GuidanceInputSource.TEST else GuidanceInputSource.USER)
                _state.update { it.copy(guidance = repo.state.value) }
                if (created) {
                    if (_state.value.vehicle.stopped == true) openIndoorGuide() else repo.setVisibility(GuideVisibility.HIDDEN)
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(error = "입차 확인 저장 실패 · 방문 상태를 확인하세요") } }
        }
    }
    fun stopVisitObservation(reason: String = "사용자 관측 중지") {
        if (!_state.value.observingVisit && scanJob == null && scanRunId == null) return
        scanGeneration.incrementAndGet(); scanJob?.cancel(); scanJob = null
        val run = scanRunId; val visit = scanVisitId
        scanRunId = null; scanVisitId = null
        _state.update { it.copy(observingVisit = false, visitObservationMessage = reason + " · 저장 기록만 유지") }
        if (run != null && visit != null) services.guidance?.stopObservation(visit, run, reason)
    }
    private fun startVisitObservation(source: VisitFrameSource, uri: Uri?, asset: String?, input: InputSource?) {
        val s = _state.value; val repo = services.guidance ?: return; val visit = s.guidance.visit ?: return
        if (s.vehicle.stopped != true || s.analyzing || visit.journey.entry == null || visit.phase != VisitPhase.PARKING_GUIDANCE || source == VisitFrameSource.TEST && !s.testVehicle) return
        stopVisitObservation("입력 소스 변경")
        val generation = scanGeneration.incrementAndGet()
        _state.update { it.copy(observingVisit = true, visitObservationMessage = "${source.label} · 시작 저장 중") }
        scanJob = viewModelScope.launch {
            var run: VisitScan? = null
            try {
                val started = repo.beginObservation(visit.id, source); run = started
                if (generation != scanGeneration.get()) { repo.stopObservation(visit.id, started.id, "시작 중 취소"); return@launch }
                scanRunId = started.id; scanVisitId = visit.id
                _state.update { it.copy(visitObservationMessage = "${source.label} · 관측 중 · 실제 측위 아님") }
                if (source == VisitFrameSource.TEST) return@launch
                withContext(Dispatchers.IO) {
                    services.visitAnalysisLock.withLock {
                        (visitFrameAnalyzer ?: analyzer).analyzeVisit(uri, asset, requireNotNull(input), { generation == scanGeneration.get() }) { frame ->
                            if (generation == scanGeneration.get()) repo.appendObservation(visit.id, started.id,
                                VisitSample(java.util.UUID.randomUUID().toString(), frame.rawText.take(1000), null, frame.analyzedAt, frame.frameTimeMs,
                                    frame.occupancy, frame.occupancyConfidence.toDouble(), frame.rawText.length > 1000))
                        }
                    }
                }
                repo.stopObservation(visit.id, started.id, "영상 분석 완료")
                if (generation == scanGeneration.get()) _state.update { it.copy(observingVisit = false, visitObservationMessage = "영상 분석 완료 · 저장 기록/문자 점수 미제공 · 현재 위치 아님") }
            } catch (e: CancellationException) {
                run?.let { repo.stopObservation(visit.id, it.id, "작업 취소/화면 종료") }; throw e
            } catch (e: Exception) {
                val reason = if (e is SecurityException) "영상 권한 없음/철회" else "관측 입력/저장 실패"
                run?.let { repo.stopObservation(visit.id, it.id, reason) }
                if (generation == scanGeneration.get()) _state.update { it.copy(observingVisit = false, visitObservationMessage = reason + " · 미확인") }
            }
        }
    }
    fun startVisitTestObservation() = startVisitObservation(VisitFrameSource.TEST, null, null, null)
    fun startVisitVideo(uri: Uri? = null, asset: String? = null) {
        val source = when (asset) {
            null -> VisitFrameSource.SELECTED_VIDEO to InputSource.VIDEO
            "generated-empty.mp4", "generated-blocked.mp4" -> VisitFrameSource.GENERATED_VIDEO to InputSource.GENERATED
            "public-obstacles.mp4" -> VisitFrameSource.PUBLIC_VIDEO to InputSource.PUBLIC_VIDEO
            else -> return
        }
        startVisitObservation(source.first, uri, asset, source.second)
    }
    fun feedVisitTestObservation(case: String) {
        val s = _state.value; val repo = services.guidance ?: return; val run = s.guidance.visit?.observation?.latest ?: return
        val visit = scanVisitId ?: return
        if (!s.observingVisit || s.vehicle.stopped != true || !s.testVehicle || run.source != VisitFrameSource.TEST || run.id != scanRunId) return
        val raw = when (case) { "B3" -> "FLOOR B3 C12"; "B4" -> "FLOOR B4 C12"; "NONE" -> ""; else -> "C12" }
        val now = System.currentTimeMillis()
        repeat(2) { i -> repo.appendObservation(visit, run.id, VisitSample(java.util.UUID.randomUUID().toString(), raw,
            if (case == "LOW") .2 else .95, now - if (case == "STALE") 60_000 else 0, i * 1000L,
            if (case == "BLOCKED") Occupancy.OCCUPIED else Occupancy.UNKNOWN, if (case == "BLOCKED") .95 else .0)) }
    }
    fun dismissParkingView() {
        parkingViewGeneration++; parkingSummaryJob?.cancel(); parkingSummaryJob = null
        _state.update { it.copy(parkingView = null) }
    }
    fun setParkingSummarySeconds(seconds: Int) {
        if (_state.value.vehicle.stopped == true && seconds in setOf(0, 8, 15)) _state.update { it.copy(parkingSummarySeconds = seconds) }
    }
    private fun <T> saveParking(label: String, queued: Boolean = false, retrying: Boolean = false,
        after: suspend (T, Boolean, Long) -> Unit = { _, _, _ -> }, operation: suspend () -> T) {
        if (_state.value.vehicle.stopped != true || !queued && parkingWrites > 0) return
        closeIndoorGuide()
        val generation = parkingViewGeneration; val sequence = ++parkingSequence
        parkingWrites++; parkingRetry = null
        _state.update { it.copy(parkingSaving = true, parkingError = "", parkingSaveMessage = "$label · 저장 중") }
        viewModelScope.launch {
            try {
                val result = operation(); services.guidance?.let { repo -> _state.update { it.copy(guidance = repo.state.value) } }; after(result, retrying, generation)
                if (sequence == parkingSequence) {
                    parkingRetry = null
                    _state.update { it.copy(parkingError = "", parkingSaveMessage = "$label · 저장값 재조회 완료" + if (it.parkingDraftDirty) " · 새 입력 미저장" else "") }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (sequence == parkingSequence) {
                    parkingRetry = { saveParking(label, queued, true, after, operation) }
                    _state.update { it.copy(parkingError = "주차 저장 확인 실패 · 저장 결과 미확인 · 같은 요청 다시 저장 가능", parkingSaveMessage = "$label · 저장 확인 실패") }
                }
            } finally { parkingWrites--; _state.update { it.copy(parkingSaving = parkingWrites > 0) } }
        }
    }
    fun retryParkingSave() { if (!_state.value.parkingSaving) parkingRetry?.invoke() }
    fun confirmParking(test: Boolean, id: String = java.util.UUID.randomUUID().toString()) {
        val s = _state.value; val repo = services.guidance ?: return; val v = s.guidance.visit ?: return
        if (v.journey.entry == null || test && !s.testVehicle) return
        val at = System.currentTimeMillis(); val epoch = movementEpoch; val sourceVersion = sourceGeneration
        saveParking("주차 완료", after = { result: ParkingCommit, retried, generation ->
            val active = result.visit.parked.active
            if (movementEpoch != epoch && active != null) {
                lastMovement?.let { repo.resumeParking(v.id, active.id, it.first, false, it.second) }
            } else if ((result.created || retried && active?.id == id) && generation == parkingViewGeneration && _state.value.vehicle.stopped == true) {
                _state.update { it.copy(parkingView = ParkingView.SUMMARY) }
                val seconds = _state.value.parkingSummarySeconds
                if (seconds > 0) parkingSummaryJob = viewModelScope.launch { delay(seconds * 1000L); if (_state.value.parkingView == ParkingView.SUMMARY) dismissParkingView() }
            }
        }) {
            check(sourceGeneration == sourceVersion) { "Vehicle state changed; explicit parking confirmation required" }
            repo.completeParking(v.id, id, if (test) GuidanceInputSource.TEST else GuidanceInputSource.USER, s.vehicle.source, at)
        }
    }
    fun editParkingDetails(details: ParkingDetails, source: GuidanceInputSource = _state.value.parkingDraftSource) {
        if (_state.value.vehicle.stopped != true) return
        require(source in setOf(GuidanceInputSource.USER, GuidanceInputSource.TEST))
        _state.update { it.copy(parkingDraft = details, parkingDraftSource = source, parkingDraftRevision = it.parkingDraftRevision + 1, parkingDraftDirty = true) }
    }
    fun clearParkingDraft() = editParkingDetails(ParkingDetails(), GuidanceInputSource.USER)
    fun fillParkingTestDraft() {
        if (_state.value.testVehicle) editParkingDetails(ParkingDetails("B3", pillar = "C12", memo = "테스트 주차 단서 · 실제 판교점 아님"), GuidanceInputSource.TEST)
    }
    fun saveParkingDetails() {
        val s = _state.value; val repo = services.guidance ?: return; val v = s.guidance.visit ?: return
        val parked = v.parked.active ?: return
        if (!s.parkingDraftDirty) return
        fun clean(value: String?) = value?.trim()?.takeIf { it.isNotEmpty() }
        val d = s.parkingDraft
        val revision = ParkingRevision(java.util.UUID.randomUUID().toString(), s.parkingDraftSource, System.currentTimeMillis(), ParkingDetails(clean(d.floor), clean(d.zone), clean(d.pillar), clean(d.elevator), d.memo.trim()))
        saveParking("수동 단서", queued = true, after = { _: VisitRecord, _, generation ->
            if (generation == parkingViewGeneration && _state.value.parkingDraftRevision == s.parkingDraftRevision) _state.update { it.copy(parkingDraftDirty = false) }
        }) { repo.amendParking(v.id, parked.id, revision) }
    }
    fun openParkingReturn() {
        val s = _state.value
        if (s.vehicle.stopped != true || s.parkingSaving || s.guidance.visit?.parked?.latest == null) return
        closeIndoorGuide(); _state.update { it.copy(parkingView = ParkingView.RETURN) }
    }
    fun resumeParkingSearch() {
        val s = _state.value; val repo = services.guidance ?: return; val v = s.guidance.visit ?: return; val parked = v.parked.active ?: return
        val at = System.currentTimeMillis()
        saveParking("다시 찾기") { repo.resumeParking(v.id, parked.id, s.vehicle.source, true, at) }
    }
    fun searchStore(query: String) { if (_state.value.vehicle.stopped == true) services.guidance?.search(query) }
    fun prepareVisit(destinationId: String) { if (_state.value.vehicle.stopped == true) services.guidance?.prepare(destinationId) }
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
        roadGeneration++
        if (visitRoad != null || _state.value.roadStarting) {
            visitRoad?.close(); visitRoad = null
            _state.value.guidance.visit?.let { v -> v.journey.latest?.let { a -> services.guidance?.roadEvent(v.id, a.id, RoadStatus.INTERRUPTED, "MODE_CHANGED", "앱 내비 방식 변경 · 순정 내비 종료/핀 상태 미확인") } }
        }
        stopVisitObservation("내비 입력 변경")
        navi?.close(); _state.update { it.copy(testNavi = test) }
        val report: (String) -> Unit = { message -> _state.update { it.copy(naviStatus = message) }; log(message) }
        navi = if (test) TestNavigationGateway(report) else PleosNavigationGateway(app, report)
    }
    fun navigate() { if (_state.value.vehicle.stopped == true) { if (navi == null) selectNavi(_state.value.testNavi); _state.value.recommendation.point?.let { navi?.route(it) } } }
    fun selectVehicle(test: Boolean) {
        closeIndoorGuide()
        sourceGeneration++; val generation = sourceGeneration
        vehicleSource?.close(); _state.update { it.copy(testVehicle = test, vehicle = VehicleSnapshot()) }
        val emit: (VehicleSnapshot) -> Unit = { snapshot ->
            viewModelScope.launch {
                if (generation == sourceGeneration) {
                    if (snapshot.stopped != true) closeIndoorGuide()
                    if (snapshot.stopped == false) { movementEpoch++; lastMovement = snapshot.source to System.currentTimeMillis() }
                    val s = _state.value.session
                    _state.update { it.copy(vehicle = snapshot, session = s.copy(stopped = snapshot.stopped == true, confirmed = if (snapshot.stopped == true) s.confirmed else false, source = services.outbox.state.value.record.request?.source ?: snapshot.source)) }
                    maybeNotify()
                    if (snapshot.stopped == false) {
                        val repo = services.guidance; val visit = repo?.state?.value?.visit; val parked = visit?.parked?.active
                        if (visit != null && parked != null) try { repo.resumeParking(visit.id, parked.id, snapshot.source, false, System.currentTimeMillis()) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { _state.update { it.copy(parkingError = "이동 상태 저장 확인 실패 · 결과 재확인 필요 · 정차 후 다시 찾기로 확인") } }
                    }
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
        stopVisitObservation("기존 후보 영상 분석으로 전환")
        _state.update { it.copy(analyzing = true, analysis = "${source.label} · 실제 모델 분석 중") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { services.visitAnalysisLock.withLock { analyzer.analyze(uri, asset, source) } } }
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
    override fun onCleared() { closeIndoorGuide(); roadGeneration++; visitRoad?.close(); sourceGeneration++; navi?.close(); vehicleSource?.close(); super.onCleared() }
}
