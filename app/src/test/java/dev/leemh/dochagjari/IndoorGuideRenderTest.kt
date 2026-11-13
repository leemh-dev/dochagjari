package dev.leemh.dochagjari

import android.graphics.Bitmap
import android.graphics.Canvas
import org.robolectric.shadows.ShadowDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Host Android framework/Compose render, explicitly not an emulator/Pleos screenshot. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IndoorGuideRenderTest {
    @get:Rule val ui = createComposeRule()
    @Test fun nativeRendererUsesSavedRouteAndControlsWithoutNetworkOrUserPositioning() {
        val app = RuntimeEnvironment.getApplication()
        val catalog = VisitGuidanceCodec.catalog(app.assets.open("visit-catalog.json").bufferedReader().use { it.readText() })
        val destination = catalog.destinations.single()
        assertTrue(EttoiFloorPlan.supports(catalog.version, destination))
        assertFalse(EttoiFloorPlan.supports("unverified-new-version", destination))
        assertFalse(EttoiFloorPlan.supports(catalog.version, destination.copy(floor = "B3")))
        var closed = false
        ui.setContent { MaterialTheme(colorScheme = darkColorScheme()) { IndoorGuideScreen(destination, catalog.version, "호스트 Compose 렌더 검사", onClose = { closed = true }) } }
        ui.onNodeWithTag("indoor-guide").assertIsDisplayed()
        ui.onNodeWithText("평면 보기").performClick()
        ui.onNodeWithText("기울여 보기").assertIsDisplayed()
        val out = File("build/reports/native-guide-preview").apply { mkdirs() }
        fun capture(name: String) {
            lateinit var bitmap: Bitmap
            ui.runOnIdle {
                val view = ShadowDialog.getLatestDialog().window!!.decorView
                bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
            }
            File(out, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        capture("flat.png")
        ui.onNodeWithText("기울여 보기").performClick()
        capture("tilted.png")
        ui.mainClock.autoAdvance = false
        ui.onNodeWithText("처음부터").performClick(); ui.mainClock.advanceTimeByFrame()
        val observed = mutableListOf<String>()
        for (frame in 0..24) {
            if (frame > 0) ui.mainClock.advanceTimeBy(300)
            observed += ui.onNodeWithTag("route-progress").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
            capture("frame-%02d.png".format(frame))
        }
        assertTrue(observed.distinct().size > 10)
        assertTrue(observed.last().contains("100%"))
        ui.mainClock.autoAdvance = true
        ui.onNodeWithText("닫기").performClick(); assertTrue(closed)
    }
}
