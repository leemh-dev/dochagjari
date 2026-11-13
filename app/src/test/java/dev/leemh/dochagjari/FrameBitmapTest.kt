package dev.leemh.dochagjari

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FrameBitmapTest {
    @Test fun decoderRgb565FrameKeepsPixelsInTensorCompatibleBitmap() {
        val input = Bitmap.createBitmap(64, 48, Bitmap.Config.RGB_565)
        input.eraseColor(Color.RED)
        val ready = modelBitmap(input)
        assertEquals(Bitmap.Config.ARGB_8888, ready.config)
        assertEquals(64, ready.width); assertEquals(48, ready.height)
        assertEquals(Color.RED, ready.getPixel(0, 0))
        assertTrue(input.isRecycled)
        ready.recycle()
    }
    @Test fun compatibleFrameIsNotCopiedOrRecycledPrematurely() {
        val input = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        assertSame(input, modelBitmap(input)); assertFalse(input.isRecycled)
        input.recycle()
    }
}
