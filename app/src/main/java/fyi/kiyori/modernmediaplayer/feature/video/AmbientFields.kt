package fyi.kiyori.modernmediaplayer.feature.video

import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Average colour of each edge of the current video frame. */
internal class AmbientEdges(val left: Color, val right: Color, val top: Color, val bottom: Color) {
    companion object {
        val Black = AmbientEdges(Color.Black, Color.Black, Color.Black, Color.Black)
    }
}

private const val SAMPLE_W = 32
private const val SAMPLE_H = 18
private const val SAMPLE_PERIOD_MS = 800L

/**
 * Samples a tiny (32x18) copy of the video frame about once a second and returns its edge colours.
 * Needs the TextureView surface of [playerView]; returns black until a frame is available.
 * Sampling stops when [active] is false so nothing runs when no side fields are visible.
 */
@Composable
internal fun rememberAmbientEdges(playerView: () -> PlayerView?, active: Boolean): State<AmbientEdges> {
    val edges = remember { mutableStateOf(AmbientEdges.Black) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        val bitmap = Bitmap.createBitmap(SAMPLE_W, SAMPLE_H, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(SAMPLE_W * SAMPLE_H)
        try {
            while (isActive) {
                val texture = playerView()?.videoSurfaceView as? TextureView
                if (texture != null && texture.isAvailable) {
                    runCatching {
                        texture.getBitmap(bitmap)
                        bitmap.getPixels(pixels, 0, SAMPLE_W, 0, 0, SAMPLE_W, SAMPLE_H)
                        edges.value = computeEdges(pixels)
                    }
                }
                delay(SAMPLE_PERIOD_MS)
            }
        } finally {
            bitmap.recycle()
        }
    }
    return edges
}

private fun computeEdges(px: IntArray): AmbientEdges {
    fun average(xs: IntRange, ys: IntRange): Color {
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0
        for (y in ys) for (x in xs) {
            val p = px[y * SAMPLE_W + x]
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
            n++
        }
        if (n == 0) return Color.Black
        return soften(Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt()))
    }
    return AmbientEdges(
        left = average(0..2, 0 until SAMPLE_H),
        right = average(SAMPLE_W - 3 until SAMPLE_W, 0 until SAMPLE_H),
        top = average(0 until SAMPLE_W, 0..1),
        bottom = average(0 until SAMPLE_W, SAMPLE_H - 2 until SAMPLE_H)
    )
}

/** Slightly more saturated, darker colour so the fields never compete with the picture. */
private fun soften(c: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((c.red * 255f).toInt(), (c.green * 255f).toInt(), (c.blue * 255f).toInt(), hsv)
    hsv[1] = (hsv[1] * 1.15f).coerceAtMost(1f)
    hsv[2] = hsv[2] * 0.8f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Full-screen background behind the video. Whatever the video does not cover (side fields in
 * landscape, top and bottom bars in portrait) shows black, an accent tint, a custom colour,
 * or the dynamic colours sampled from the frame edges.
 */
@Composable
internal fun FieldsBackground(
    mode: Int,
    customColor: Int,
    accent: Color,
    edges: AmbientEdges,
    displayW: Dp,
    displayH: Dp,
    modifier: Modifier = Modifier
) {
    val left by animateColorAsState(edges.left, tween(900), label = "fieldLeft")
    val right by animateColorAsState(edges.right, tween(900), label = "fieldRight")
    val top by animateColorAsState(edges.top, tween(900), label = "fieldTop")
    val bottom by animateColorAsState(edges.bottom, tween(900), label = "fieldBottom")

    Box(
        modifier.drawBehind {
            drawRect(Color.Black)
            when (mode) {
                FIELD_ACCENT -> drawRect(accent.copy(alpha = 0.24f))
                FIELD_CUSTOM -> drawRect(Color(customColor))
                FIELD_DYNAMIC -> {
                    val w = size.width
                    val h = size.height
                    val dw = displayW.toPx()
                    val dh = displayH.toPx()
                    if (dw < w - 1f) {
                        val fx = ((w - dw) / 2f / w).coerceIn(0f, 0.5f)
                        drawRect(
                            Brush.horizontalGradient(
                                0f to left, fx to left, (1f - fx) to right, 1f to right
                            )
                        )
                    } else if (dh < h - 1f) {
                        val fy = ((h - dh) / 2f / h).coerceIn(0f, 0.5f)
                        drawRect(
                            Brush.verticalGradient(
                                0f to top, fy to top, (1f - fy) to bottom, 1f to bottom
                            )
                        )
                    }
                }
            }
        }
    )
}
