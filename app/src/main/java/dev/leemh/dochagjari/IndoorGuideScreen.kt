package dev.leemh.dochagjari

import android.graphics.Paint
import android.graphics.Typeface
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private val RouteBlue = Color(0xFF246EFF)

@Composable
fun IndoorGuideScreen(destination: IndoorDestination, catalogVersion: String, inputLabel: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val owner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val reducedMotion = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
        var progress by rememberSaveable { mutableFloatStateOf(0f) }
        var playing by rememberSaveable { mutableStateOf(!reducedMotion) }
        var tilted by rememberSaveable { mutableStateOf(true) }
        var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
        val supported = EttoiFloorPlan.supports(catalogVersion, destination)
        DisposableEffect(owner) {
            val observer = LifecycleEventObserver { _, event ->
                resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (event == Lifecycle.Event.ON_STOP) playing = false
            }
            owner.lifecycle.addObserver(observer)
            onDispose { owner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(playing, resumed, supported) {
            if (playing && resumed && supported) {
                var previous = withFrameNanos { it }
                while (progress < 1f) {
                    val now = withFrameNanos { it }
                    progress = (progress + (now - previous).coerceAtMost(200_000_000L) / 6_000_000_000f).coerceAtMost(1f)
                    previous = now
                }
                playing = false
            }
        }
        Surface(Modifier.fillMaxSize().testTag("indoor-guide"), color = Color(0xFF101C2D), contentColor = Color(0xFFF3F6FC)) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("5층 에뜨와 동선", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("${destination.facilityName} · $inputLabel", fontSize = 13.sp, color = Color(0xFFB7CCE7))
                    }
                    OutlinedButton(onClick = onClose) { Text("닫기") }
                }
                Text("유아휴게실·유아차 대여 쪽 엘리베이터 → 복도 → 에뜨와", fontSize = 17.sp)
                if (supported) {
                    val density = LocalDensity.current.density
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val wide = maxWidth >= 900.dp
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (wide) Column(Modifier.width(235.dp).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                Text("5F · 걸어갈 동선", color = Color(0xFF8EB7FF), fontWeight = FontWeight.Bold)
                                Text("① 엘리베이터에서 출발\n유아휴게실·유아차 대여 쪽", fontSize = 16.sp)
                                Text("② 옆 복도로 이동\n블루독베이비·밍크뮤 블록을 돌아갑니다", fontSize = 16.sp)
                                Text("③ 에뜨와 입구\n파란 동선을 따라 이동하세요", fontSize = 16.sp)
                                Text("승강기 호기·주차장 연결은 아직 미확인입니다.", fontSize = 13.sp, color = Color(0xFFB7CCE7))
                            }
                    Surface(Modifier.weight(1f).height(470.dp), shape = RoundedCornerShape(22.dp), color = Color(0xFFEAF0F8)) {
                        FloorCanvas(destination, progress, Modifier.fillMaxSize().padding(12.dp).graphicsLayer {
                            rotationX = if (tilted) 26f else 0f
                            rotationZ = if (tilted) -8f else 0f
                            scaleX = .84f; scaleY = .84f
                            cameraDistance = 12 * density
                        }.testTag("indoor-floor-map").semantics {
                            contentDescription = "공식 동선 기반 5층 일부 개략도"
                            stateDescription = if (tilted) "기울인 개략도" else "평면 개략도"
                        })
                    }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { if (progress >= 1f) progress = 0f; playing = !playing }) { Text(if (playing) "일시정지" else "재생") }
                        OutlinedButton(onClick = { progress = 0f; playing = true }) { Text("처음부터") }
                        OutlinedButton(onClick = { tilted = !tilted }) { Text(if (tilted) "평면 보기" else "기울여 보기") }
                        Text("동선 재생 ${(progress * 100).toInt()}%", Modifier.padding(top = 12.dp).testTag("route-progress"), fontSize = 15.sp)
                    }
                } else Text("이 자료 버전의 지도는 준비되지 않았습니다. 저장된 방문은 유지합니다.", color = Color(0xFFFFCD81))
                Text("공식 동선 기반 개략도 · 이동점은 경로 재생이며 현재 위치가 아닙니다.", fontSize = 13.sp, color = Color(0xFFB7CCE7))
                Text("기둥·승강기 호기·거리·전 구간 무단차는 미확인입니다. 주차장 연결·카메라 관측은 다음 단계입니다.", fontSize = 12.sp, color = Color(0xFFB7CCE7))
                Text("자료 ${destination.checkedAt} · $catalogVersion", fontSize = 12.sp, color = Color(0xFFB7CCE7))
            }
        }
    }
}

@Composable private fun FloorCanvas(destination: IndoorDestination, progress: Float, modifier: Modifier) {
    val labelSize = with(LocalDensity.current) { 12.sp.toPx() }
    Canvas(modifier.background(Color.White, RoundedCornerShape(14.dp))) {
        val projection = FloorProjection(EttoiFloorPlan.bounds, size.width, size.height, 16.dp.toPx())
        fun point(p: FloorPoint): Offset = projection.project(p).let { Offset(it.x, it.y) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = labelSize; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        EttoiFloorPlan.blocks.forEach { block ->
            val a = point(FloorPoint(block.rect.left, block.rect.top)); val b = point(FloorPoint(block.rect.right, block.rect.bottom))
            val color = when (block.kind) {
                "lift", "escalator" -> Color(0xFFB4DFCC)
                "service" -> Color(0xFFE8DBEF)
                "destination" -> Color(0xFFD4E5FF)
                else -> Color(0xFFE4E7EC)
            }
            drawRoundRect(color, a, Size(b.x - a.x, b.y - a.y), CornerRadius(3.dp.toPx()))
            if (block.kind == "destination") drawRoundRect(RouteBlue, a, Size(b.x - a.x, b.y - a.y), CornerRadius(3.dp.toPx()), style = Stroke(2.dp.toPx()))
            paint.color = android.graphics.Color.rgb(43, 59, 78)
            val lines = block.label.split('\n')
            lines.forEachIndexed { index, line ->
                val maxWidth = (b.x - a.x - 3.dp.toPx()).coerceAtLeast(1f)
                paint.textSize = labelSize
                if (paint.measureText(line) > maxWidth) paint.textSize *= maxWidth / paint.measureText(line)
                drawContext.canvas.nativeCanvas.drawText(line, (a.x + b.x) / 2, (a.y + b.y) / 2 + (index - (lines.size - 1) / 2f) * labelSize * 1.1f + paint.textSize / 3, paint)
            }
        }
        val path = Path().apply { destination.route.forEachIndexed { index, p -> val q = point(p); if (index == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), partial, true)
        val stroke = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(partial, Color.White, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(partial, RouteBlue, style = stroke)
        val start = point(destination.route.first()); val end = point(destination.route.last())
        listOf(start, end).forEach { drawCircle(Color.White, 7.dp.toPx(), it); drawCircle(RouteBlue, 4.dp.toPx(), it) }
        if (progress > 0 && progress < 1) {
            val traveling = measure.getPosition(measure.length * progress)
            drawCircle(Color.White, 7.dp.toPx(), traveling); drawCircle(RouteBlue, 4.dp.toPx(), traveling)
        }
        paint.textSize = labelSize; paint.color = android.graphics.Color.rgb(28, 88, 182); paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawContext.canvas.nativeCanvas.drawText("엘리베이터 · 호기 미확인", start.x + 70.dp.toPx(), start.y - 8.dp.toPx(), paint)
        drawContext.canvas.nativeCanvas.drawText("에뜨와 입구", end.x + 40.dp.toPx(), end.y + 20.dp.toPx(), paint)
    }
}
