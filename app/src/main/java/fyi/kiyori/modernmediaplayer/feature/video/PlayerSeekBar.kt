package fyi.kiyori.modernmediaplayer.feature.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val BarPadding = 10.dp
private val MarkerColor = Color(0xFFFFD54F)

/**
 * YouTube-style seek bar: thin track, buffered segment, round thumb that grows while
 * scrubbing. Touching anywhere on the bar starts scrubbing at once, as in YouTube.
 * Bookmarks are drawn as small ticks and the A/B loop points as amber markers.
 */
@Composable
internal fun YtSeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    scrubbing: Boolean,
    color: Color,
    bookmarks: List<Long>,
    markA: Long?,
    markB: Long?,
    onScrubStart: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val start by rememberUpdatedState(onScrubStart)
    val move by rememberUpdatedState(onScrub)
    val end by rememberUpdatedState(onScrubEnd)
    val duration by rememberUpdatedState(durationMs)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val total = duration
                    if (total <= 0L) return@awaitEachGesture
                    val pad = BarPadding.toPx()
                    val usable = (size.width - 2f * pad).coerceAtLeast(1f)
                    fun toMs(x: Float): Long = (((x - pad) / usable).coerceIn(0f, 1f) * total).toLong()
                    down.consume()
                    start()
                    var target = toMs(down.position.x)
                    move(target)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        target = toMs(change.position.x)
                        move(target)
                    }
                    end(target)
                }
            }
    ) {
        val pad = BarPadding.toPx()
        val usable = (size.width - 2f * pad).coerceAtLeast(1f)
        val trackH = (if (scrubbing) 5.dp else 3.dp).toPx()
        val radius = trackH / 2f
        val cy = size.height / 2f
        val total = durationMs.coerceAtLeast(1L).toFloat()
        fun xOf(ms: Long) = pad + usable * (ms / total).coerceIn(0f, 1f)

        fun segment(toX: Float, c: Color) {
            drawRoundRect(
                color = c,
                topLeft = Offset(pad, cy - radius),
                size = Size((toX - pad).coerceAtLeast(0f), trackH),
                cornerRadius = CornerRadius(radius)
            )
        }
        segment(pad + usable, Color.White.copy(alpha = 0.3f))
        segment(xOf(bufferedMs), Color.White.copy(alpha = 0.5f))
        val playedX = xOf(positionMs)
        segment(playedX, color)

        val tickW = 2.dp.toPx()
        bookmarks.forEach { ms ->
            drawRect(Color.White, Offset(xOf(ms) - tickW / 2f, cy - 6.dp.toPx()), Size(tickW, 12.dp.toPx()))
        }
        listOfNotNull(markA, markB).forEach { ms ->
            drawRect(MarkerColor, Offset(xOf(ms) - 1.5.dp.toPx(), cy - 8.dp.toPx()), Size(3.dp.toPx(), 16.dp.toPx()))
        }
        drawCircle(color, radius = (if (scrubbing) 10.dp else 6.dp).toPx(), center = Offset(playedX, cy))
    }
}
