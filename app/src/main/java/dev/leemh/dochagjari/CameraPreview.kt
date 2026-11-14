package dev.leemh.dochagjari

import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.graphics.SurfaceTexture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private class PreviewPlayer {
    var player: MediaPlayer? = null
    var surface: Surface? = null
    fun close() { player?.release(); player = null; surface?.release(); surface = null }
}

/** Muted bundled clip only, disposed when the guide closes/stops. Never an OEM camera claim. */
@Composable fun SampleCameraPreview(asset: String?, badge: String, modifier: Modifier = Modifier) {
    var failed by remember(asset) { mutableStateOf(false) }
    val owner = remember(asset) { PreviewPlayer() }
    DisposableEffect(owner) { onDispose { owner.close() } }
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B111C)).testTag("arrival-camera")) {
        if (asset != null && !failed) key(asset) {
            AndroidView(factory = { context -> TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                        runCatching {
                            owner.close(); owner.surface = Surface(texture)
                            owner.player = MediaPlayer().apply {
                                context.assets.openFd(asset).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                                setSurface(owner.surface); isLooping = true; setVolume(0f, 0f)
                                setOnPreparedListener { it.start() }
                                setOnErrorListener { _, _, _ -> failed = true; owner.close(); true }
                                prepareAsync()
                            }
                        }.onFailure { failed = true; owner.close() }
                    }
                    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit
                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean { owner.close(); return true }
                }
            } }, modifier = Modifier.fillMaxSize())
        } else Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("▣", color = CabinMuted, fontSize = 34.sp)
            Text(if (failed) "영상 미리보기를 열 수 없어요" else "차량 카메라 연결 대기", color = CabinMuted)
            Text("실제 외부 카메라 미연동", color = CabinMuted, fontSize = 13.sp)
        }
        Text(if (asset != null) "생성 사전 영상 · 실카메라 아님" else "카메라 입력 미확인",
            Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0xCC101722), RoundedCornerShape(10.dp)).padding(8.dp),
            color = Color.White, fontSize = 13.sp)
        Text(badge, Modifier.align(Alignment.BottomEnd).padding(10.dp).background(Color(0xDD101722), RoundedCornerShape(10.dp)).padding(8.dp),
            color = Color.White, fontSize = 15.sp)
    }
}
