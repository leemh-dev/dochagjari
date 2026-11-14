package dev.leemh.dochagjari

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/** Runs inside the Compose activity rule, before its failed-test teardown. Test data only. */
class FailureScreenshot : TestWatcher() {
    override fun failed(error: Throwable, description: Description) {
        runCatching {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "test-failures").apply { mkdirs() }
            val name = description.className.substringAfterLast('.') + "-" + description.methodName
            instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
            File(directory, "$name.txt").writeText(error.javaClass.name + ": " + error.message)
        }
    }
}
