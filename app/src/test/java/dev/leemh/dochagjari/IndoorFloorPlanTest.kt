package dev.leemh.dochagjari

import org.junit.Assert.*
import org.junit.Test

class IndoorFloorPlanTest {
    @Test fun projectionKeepsGeometryInsideLandscapeAndPortraitViewportsWithoutDistortingAspect() {
        listOf(1200f to 390f, 350f to 390f).forEach { (width, height) ->
            val projection = FloorProjection(EttoiFloorPlan.bounds, width, height, 16f)
            EttoiFloorPlan.blocks.forEach { block ->
                listOf(FloorPoint(block.rect.left, block.rect.top), FloorPoint(block.rect.right, block.rect.bottom)).forEach { p ->
                    val q = projection.project(p)
                    assertTrue(q.x >= 16f && q.x <= width - 16f); assertTrue(q.y >= 16f && q.y <= height - 16f)
                }
            }
            val origin = projection.project(FloorPoint(600.0, 600.0))
            val x = projection.project(FloorPoint(700.0, 600.0)); val y = projection.project(FloorPoint(600.0, 700.0))
            assertEquals(x.x - origin.x, y.y - origin.y, .001f)
        }
    }
    @Test fun invalidViewportCannotCreateInfiniteCoordinates() {
        assertThrows(IllegalArgumentException::class.java) { FloorProjection(EttoiFloorPlan.bounds, 0f, 390f, 16f) }
        assertThrows(IllegalArgumentException::class.java) { FloorProjection(EttoiFloorPlan.bounds, Float.NaN, 390f, 16f) }
    }
}
