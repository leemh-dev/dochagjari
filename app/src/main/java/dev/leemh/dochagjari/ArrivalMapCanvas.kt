package dev.leemh.dochagjari

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp

enum class ArrivalScene(val label: String) { PARKING("주차장"), HOSPITAL("병원 동행"), ACADEMY("학원 픽업"), STATION("정자역 마중"), AIRPORT("공항 배웅") }

/** Presentation coordinates only. No pose, measured location, legal bay or facility connection. */
@Composable fun ArrivalMapCanvas(scene: ArrivalScene, modifier: Modifier = Modifier) {
    val textPx = with(LocalDensity.current) { 14.sp.toPx() }
    Canvas(modifier.background(Color(0xFF172231)).testTag(if (scene == ArrivalScene.PARKING) "arrival-parking-map" else "arrival-road-map")) {
        val sx = size.width / 1000f; val sy = size.height / 620f
        withTransform({ scale(sx, sy, pivot = Offset.Zero) }) {
            val road = Color(0xFF3C4859); val sidewalk = Color(0xFF627184); val line = Color(0xFFD0D9E5)
            val amber = Color(0xFFF7B654); val teal = Color(0xFF5CD5CA)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = textPx / sx }
            fun label(text: String, x: Float, y: Float, color: Color = Color.White) {
                paint.color = android.graphics.Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
            }
            fun block(x: Float, y: Float, w: Float, h: Float, color: Color = Color(0xFF758599)) {
                drawRect(Color(0xFF394758), Offset(x + 8, y + h), Size(w, 26f))
                drawRect(color, Offset(x, y), Size(w, h))
                drawLine(Color(0xFF9FAFC0), Offset(x, y), Offset(x + w, y), 2f)
            }
            if (scene == ArrivalScene.PARKING) {
                drawRoundRect(road, Offset(35f, 70f), Size(930f, 490f), CornerRadius(12f))
                for (row in listOf(120f, 300f, 470f)) for (column in 0..11) {
                    val x = 80f + column * 69f
                    drawRect(line.copy(alpha = .65f), Offset(x, row), Size(60f, 67f), style = Stroke(2f))
                    if ((column + row.toInt()) % 4 == 0) block(x + 12, row + 10, 35f, 46f, Color(0xFF8A9AAA))
                }
                for (x in listOf(110f, 385f, 660f, 865f)) {
                    drawCircle(Color(0xFF92A8BB), 7f, Offset(x, 250f)); drawCircle(Color(0xFF92A8BB), 7f, Offset(x, 425f))
                }
                block(640f, 8f, 180f, 70f, Color(0xFF4273B9)); label("승강시설 · 배치 예시", 530f, 50f)
                label("주차층·기둥·현재 위치 미확인", 90f, 590f, Color(0xFFD1DCEB))
            } else {
                drawRect(road, Offset(0f, 280f), Size(1000f, 285f))
                drawRect(sidewalk, Offset(0f, 205f), Size(1000f, 75f))
                drawRect(Color(0xFF556575), Offset(0f, 565f), Size(1000f, 40f))
                for (y in listOf(375f, 470f)) for (x in 0..12) drawLine(line, Offset(x * 85f, y), Offset(x * 85f + 42, y), 3f)
                drawLine(line, Offset(0f, 282f), Offset(1000f, 282f), 4f)
                for (i in 0..6) drawRect(line, Offset(790f, 290f + i * 39), Size(65f, 22f))
                for (i in 0..3) block(35f + i * 210, 40f, 165f, 128f)
                for (x in listOf(150f, 450f, 690f)) block(x, 316f, 82f, 42f, Color(0xFF9EACBA))
                when (scene) {
                    ArrivalScene.ACADEMY -> {
                        drawLine(amber, Offset(75f, 298f), Offset(700f, 298f), 12f)
                        label("정차 허용·대안 장소 확인 필요", 90f, 585f, amber)
                        label("학원가", 80f, 100f); label("보행로", 90f, 253f, teal)
                    }
                    ArrivalScene.HOSPITAL -> {
                        block(245f, 15f, 470f, 158f, Color(0xFF7895A9))
                        label("사람 출입구", 355f, 147f)
                        drawRoundRect(amber, Offset(390f, 285f), Size(220f, 65f), CornerRadius(12f), style = Stroke(5f))
                        label("승하차 후보 · 현장 확인 전", 160f, 585f, amber)
                    }
                    ArrivalScene.STATION -> {
                        block(200f, 190f, 150f, 70f, Color(0xFF3F8187))
                        label("역 출구 · 배치 예시", 365f, 248f)
                        drawLine(teal, Offset(90f, 270f), Offset(720f, 270f), 5f)
                        label("사람 출구와 차량 승차점은 달라요", 80f, 585f, teal)
                    }
                    ArrivalScene.AIRPORT -> {
                        block(0f, 20f, 1000f, 145f, Color(0xFF68829C))
                        label("터미널 · 배웅 예시", 200f, 110f)
                        drawLine(amber, Offset(50f, 288f), Offset(720f, 288f), 8f)
                        label("긴 대기는 주차장 이용을 확인해요", 100f, 585f, amber)
                    }
                    else -> Unit
                }
            }
        }
    }
}
