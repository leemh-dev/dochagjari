package dev.leemh.dochagjari

import kotlin.math.min

/** Drawing coordinates only: no distance unit or user positioning is implied. */
data class DrawingRect(val left: Double, val top: Double, val right: Double, val bottom: Double)
data class FloorBlock(val label: String, val rect: DrawingRect, val kind: String = "store")
data class ProjectedPoint(val x: Float, val y: Float)
class FloorProjection(private val bounds: DrawingRect, width: Float, height: Float, padding: Float) {
    init {
        require(width.isFinite() && height.isFinite() && padding.isFinite() && width > padding * 2 && height > padding * 2 && padding >= 0)
        require(bounds.right > bounds.left && bounds.bottom > bounds.top)
    }
    private val scale = min((width - 2 * padding) / (bounds.right - bounds.left), (height - 2 * padding) / (bounds.bottom - bounds.top))
    private val dx = (width - (bounds.right - bounds.left) * scale) / 2
    private val dy = (height - (bounds.bottom - bounds.top) * scale) / 2
    fun project(point: FloorPoint) = ProjectedPoint(((point.x - bounds.left) * scale + dx).toFloat(), ((point.y - bounds.top) * scale + dy).toFloat())
}

/** Self-drawn, partial floor schematic. Source geometry is not a licensed map image/SDK. */
object EttoiFloorPlan {
    const val catalogVersion = "pangyo-ettoi-20261113-v1"
    val bounds = DrawingRect(400.0, 420.0, 1100.0, 1035.0)
    private fun block(label: String, x: Double, y: Double, width: Double, height: Double, kind: String = "store"): FloorBlock {
        // Reference screenshot pixel positions converted to the same drawing coordinates as the catalog route.
        fun projectX(v: Double) = (v + 64) / .96
        fun projectY(v: Double) = (v + 88) / .96
        return FloorBlock(label, DrawingRect(projectX(x), projectY(y), projectX(x + width), projectY(y + height)), kind)
    }
    val blocks = listOf(
        block("엘리베이터", 668.0, 328.0, 36.0, 108.0, "lift"),
        block("유아휴게실", 354.0, 519.0, 119.0, 152.0, "service"),
        block("유아차 대여", 509.0, 519.0, 102.0, 33.0, "service"),
        block("아디다스\n키즈", 509.0, 552.0, 52.0, 70.0),
        block("플레이\n키즈프로", 561.0, 552.0, 50.0, 70.0),
        block("뉴발란스\n키즈", 611.0, 519.0, 101.0, 103.0),
        block("파타고니아\n키즈", 712.0, 519.0, 101.0, 103.0),
        block("노스페이스\n키즈", 813.0, 519.0, 101.0, 103.0),
        block("아람북스", 500.0, 653.0, 62.0, 65.0),
        block("메론\n스위치", 562.0, 653.0, 62.0, 65.0),
        block("더바디샵", 624.0, 653.0, 62.0, 65.0),
        block("리틀\n아카이브", 686.0, 653.0, 62.0, 65.0),
        block("블루독\n베이비", 500.0, 718.0, 62.0, 66.0),
        block("밍크뮤", 562.0, 718.0, 62.0, 66.0),
        block("에뜨와", 624.0, 718.0, 62.0, 66.0, "destination"),
        block("압소바", 686.0, 718.0, 62.0, 66.0),
        block("에스컬레이터", 690.0, 818.0, 33.0, 80.0, "escalator")
    )
    fun supports(version: String, destination: IndoorDestination): Boolean =
        version == catalogVersion && destination.id == "hyundai-pangyo-ettoi" && destination.facilityId == "hyundai-pangyo" &&
            destination.floor == "5F" && destination.routeStartId == "PO-hpuc3mdcR3562" && destination.routeEndId == "PO-AE7oOvIjQ9013" &&
            destination.route.size >= 2 && destination.route.all { it.x in bounds.left..bounds.right && it.y in bounds.top..bounds.bottom }
}
