package dev.leemh.dochagjari

import android.app.Application
import android.net.Uri
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class VideoReadAccessTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    @Test fun readableAppCacheFileNeedsNoDocumentGrant() {
        val f = File.createTempFile("owned", ".mp4", app.cacheDir)
        try { requireVideoReadAccess(app, Uri.fromFile(f)) } finally { f.delete() }
    }
    @Test fun readableAppFilesFileNeedsNoDocumentGrant() {
        val f = File.createTempFile("owned", ".mp4", app.filesDir)
        try { requireVideoReadAccess(app, Uri.fromFile(f)) } finally { f.delete() }
    }
    @Test fun similarPrefixOutsidePrivateRootIsDenied() {
        val dir = File(app.cacheDir.path + "-outside").apply { mkdirs() }; val f = File(dir, "video.mp4").apply { writeText("test") }
        try { assertThrows(SecurityException::class.java) { requireVideoReadAccess(app, Uri.fromFile(f)) } } finally { f.delete(); dir.delete() }
    }
    private fun documentContext(permission: Int) = object : android.content.ContextWrapper(app) {
        override fun checkUriPermission(uri: Uri, pid: Int, uid: Int, modeFlags: Int) = permission
    }
    @Test fun ungrantedContentDocumentCannotBeRead() {
        assertThrows(SecurityException::class.java) { requireVideoReadAccess(documentContext(android.content.pm.PackageManager.PERMISSION_DENIED), Uri.parse("content://ungranted.example/video/1")) }
    }
    @Test fun grantedContentDocumentUsesNormalGrant() {
        requireVideoReadAccess(documentContext(android.content.pm.PackageManager.PERMISSION_GRANTED), Uri.parse("content://granted.example/video/1"))
    }
}
