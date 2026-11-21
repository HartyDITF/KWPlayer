package fyi.kiyori.modernmediaplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object MediaArtworkCache {
    val bitmaps = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
}

@Composable
fun MediaArtwork(item: LocalMediaItem, modifier: Modifier = Modifier, accent: Color, surface: Color) {
    val context = LocalContext.current
    val cacheKey = item.uri.toString()
    val bitmapState = produceState<Bitmap?>(MediaArtworkCache.bitmaps.get(cacheKey), cacheKey) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                MediaArtworkCache.bitmaps.get(cacheKey) ?: loadArtwork(context, item.uri, item.isVideo)?.also {
                    MediaArtworkCache.bitmaps.put(cacheKey, it)
                }
            }
        }
    }
    Box(modifier.clip(RoundedCornerShape(11.dp)).background(surface), contentAlignment = Alignment.Center) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = if (item.isVideo) "Превью видео" else "Обложка аудио",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(if (item.isVideo) "▶" else "♫", color = accent, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun loadArtwork(context: Context, uri: Uri, isVideo: Boolean): Bitmap? {
    if (isVideo && Build.VERSION.SDK_INT >= 29) {
        runCatching {
            context.contentResolver.loadThumbnail(uri, Size(480, 270), CancellationSignal())
        }.getOrNull()?.let { return it }
    }
    val retriever = MediaMetadataRetriever()
    val bitmap = try {
        retriever.setDataSource(context, uri)
        if (isVideo) {
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } else {
            retriever.embeddedPicture?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        }
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
    return bitmap?.let { downscale(it, if (isVideo) 480 else 360) }
}

private fun downscale(source: Bitmap, maxSide: Int): Bitmap {
    val largest = maxOf(source.width, source.height)
    if (largest <= maxSide) return source
    val scale = maxSide.toFloat() / largest.toFloat()
    val width = (source.width * scale).toInt().coerceAtLeast(1)
    val height = (source.height * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(source, width, height, true)
    if (scaled !== source) source.recycle()
    return scaled
}
