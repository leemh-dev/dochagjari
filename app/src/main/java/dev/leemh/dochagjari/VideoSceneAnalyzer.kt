package dev.leemh.dochagjari

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class SceneResult(val observation: Observation, val clue: String, val detail: String)
interface SceneAnalyzer { fun analyze(uri: Uri?, asset: String?, source: InputSource): SceneResult }

/** MediaMetadataRetriever may return RGB_565 or hardware bitmaps; TensorImage requires ARGB_8888. */
internal fun modelBitmap(decoded: Bitmap): Bitmap {
    if (decoded.config == Bitmap.Config.ARGB_8888) return decoded
    return try { requireNotNull(decoded.copy(Bitmap.Config.ARGB_8888, false)) { "프레임 형식 변환 실패" } }
    finally { decoded.recycle() }
}

class VideoSceneAnalyzer(private val context: Context) : SceneAnalyzer {
    override fun analyze(uri: Uri?, asset: String?, source: InputSource): SceneResult {
        val retriever = MediaMetadataRetriever()
        val detector = ObjectDetector.createFromFileAndOptions(context, "efficientdet-lite0.tflite", ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(BaseOptions.builder().setNumThreads(2).build()).setMaxResults(12).setScoreThreshold(.35f).build())
        val ocr = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var temp: File? = null
        try {
            if (asset != null) {
                temp = File.createTempFile("scene", ".mp4", context.cacheDir)
                context.assets.open(asset).use { input -> temp.outputStream().use { input.copyTo(it) } }
                retriever.setDataSource(temp.absolutePath)
            } else retriever.setDataSource(context, requireNotNull(uri))
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            require(duration > 0) { "재생 가능한 영상이 아닙니다" }
            val baseline = if (source == InputSource.GENERATED) context.assets.open("generated-empty.png").use { BitmapFactory.decodeStream(it) } else null
            var occupied = 0; var empty = 0; var peak = 0f; var frames = 0
            val labels = linkedSetOf<String>(); val clues = linkedSetOf<String>()
            for (second in 0 until minOf(8, (duration / 1000).toInt().coerceAtLeast(1))) {
                val decoded = retriever.getScaledFrameAtTime(second * 1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST, 640, 480) ?: continue
                val frame = modelBitmap(decoded)
                try {
                    frames++
                    // Registered demo candidate ROI, central 80%; never infers permission to stop.
                    val roi = RectF(frame.width * .1f, frame.height * .1f, frame.width * .9f, frame.height * .9f)
                    var frameScore = 0f
                    val detections = detector.detect(TensorImage.fromBitmap(frame))
                    detections.forEach { detection ->
                        if (RectF.intersects(roi, detection.boundingBox)) {
                            detection.categories.forEach { category ->
                                labels += "${category.label}:${"%.2f".format(category.score)}"
                                // People, animals, vehicles and bulky objects are obstacles, not just cars.
                                if (category.label in setOf("person", "bicycle", "car", "motorcycle", "bus", "truck", "dog", "cat", "bench", "chair", "suitcase")) frameScore = maxOf(frameScore, category.score)
                            }
                        }
                    }
                    if (baseline != null) {
                        val difference = referenceDifference(frame, baseline)
                        if (difference > .08) frameScore = maxOf(frameScore, .9f)
                        else if (difference < .025 && frameScore < .65f) empty++
                    }
                    if (frameScore >= .65f) { occupied++; peak = maxOf(peak, frameScore) }
                    if (second % 2 == 0) {
                        val text = Tasks.await(ocr.process(InputImage.fromBitmap(frame, 0)), 15, TimeUnit.SECONDS).text
                        Regex("\\b[A-Z][ -]?[0-9]{1,3}\\b").findAll(text.uppercase()).forEach { clues += it.value }
                    }
                } finally { frame.recycle() }
            }
            baseline?.recycle()
            val status = when { occupied >= 2 -> Occupancy.OCCUPIED; frames >= 2 && empty == frames -> Occupancy.EMPTY; else -> Occupancy.UNKNOWN }
            val confidence = when (status) { Occupancy.OCCUPIED -> peak; Occupancy.EMPTY -> .9f; Occupancy.UNKNOWN -> .3f }
            val detail = "$frames 프레임 · 점유 $occupied · 기준영상 일치 $empty\n탐지: ${labels.joinToString()}\n표지 제안: ${clues.joinToString().ifBlank { "미확인" }}\n일반 영상은 기준영상이 없으면 빈 공간으로 확정하지 않습니다."
            return SceneResult(Observation(status, confidence, System.currentTimeMillis(), source, detail), clues.joinToString(), detail)
        } finally { retriever.release(); detector.close(); ocr.close(); temp?.delete() }
    }
    private fun referenceDifference(frame: Bitmap, reference: Bitmap): Double {
        val a = Bitmap.createScaledBitmap(frame, 80, 60, false)
        val b = Bitmap.createScaledBitmap(reference, 80, 60, false)
        var changed = 0; var total = 0
        for (y in 12 until 48) for (x in 8 until 72) {
            val p = a.getPixel(x,y); val q = b.getPixel(x,y)
            val delta = abs((p shr 16 and 255) - (q shr 16 and 255)) + abs((p shr 8 and 255) - (q shr 8 and 255)) + abs((p and 255) - (q and 255))
            if (delta > 100) changed++
            total++
        }
        a.recycle(); b.recycle(); return changed.toDouble() / total
    }
}
