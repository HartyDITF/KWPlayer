package fyi.kiyori.modernmediaplayer.feature.video

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

/** Callbacks for [playerGestures]. Fractions are relative to the size of the gesture area. */
internal interface PlayerGestureListener {
    fun onTap()

    /** Called for the double tap and for every further quick tap, like YouTube. */
    fun onDoubleTap(xFraction: Float)
    fun onLongPressStart()
    fun onLongPressEnd()
    fun onVerticalDragStart(leftHalf: Boolean)

    /** [upFraction] > 0 when the finger moves up; 1.0 equals the full height. */
    fun onVerticalDrag(leftHalf: Boolean, upFraction: Float)
    fun onVerticalDragEnd()
    fun onHorizontalDragStart()
    fun onHorizontalDrag(totalFraction: Float)
    fun onHorizontalDragEnd(commit: Boolean)
    fun onPinchEnd(zoom: Float)
}

private enum class Phase { LongPress, Up, Move, Pinch, Cancel }

/**
 * One gesture detector for the whole video surface. It replaces several stacked detectors,
 * because overlapping siblings do not all receive the same touches in Compose, which
 * leaves the lower detectors dead.
 */
@Composable
internal fun Modifier.playerGestures(listener: PlayerGestureListener): Modifier {
    val current by rememberUpdatedState(listener)
    return pointerInput(Unit) {
        awaitEachGesture { handleGesture { current } }
    }
}

private suspend fun AwaitPointerEventScope.handleGesture(listener: () -> PlayerGestureListener) {
    val down = awaitFirstDown(requireUnconsumed = false)
    val slop = viewConfiguration.touchSlop
    val width = size.width.toFloat().coerceAtLeast(1f)
    val height = size.height.toFloat().coerceAtLeast(1f)

    var phase = Phase.LongPress
    var moved = Offset.Zero
    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } > 1) {
                phase = Phase.Pinch
                break
            }
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null) {
                phase = Phase.Cancel
                break
            }
            if (change.changedToUp()) {
                phase = Phase.Up
                break
            }
            val delta = change.position - down.position
            if (delta.getDistance() > slop) {
                moved = delta
                phase = Phase.Move
                break
            }
        }
    }

    when (phase) {
        Phase.Cancel -> Unit

        Phase.LongPress -> {
            listener().onLongPressStart()
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.all { !it.pressed }) break
                }
            } finally {
                listener().onLongPressEnd()
            }
        }

        Phase.Up -> {
            var next: PointerInputChange? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                awaitFirstDown(requireUnconsumed = false)
            }
            if (next == null) {
                listener().onTap()
                return
            }
            while (true) {
                val tap = next ?: break
                waitForUpOrCancellation()
                listener().onDoubleTap(tap.position.x / width)
                next = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                    awaitFirstDown(requireUnconsumed = false)
                }
            }
        }

        Phase.Move -> {
            if (abs(moved.y) > abs(moved.x)) {
                val left = down.position.x < width / 2f
                listener().onVerticalDragStart(left)
                try {
                    listener().onVerticalDrag(left, -moved.y / height)
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) break
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dy = change.position.y - change.previousPosition.y
                        change.consume()
                        listener().onVerticalDrag(left, -dy / height)
                    }
                } finally {
                    listener().onVerticalDragEnd()
                }
            } else {
                var total = moved.x
                var commit = false
                listener().onHorizontalDragStart()
                try {
                    listener().onHorizontalDrag(total / width)
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) break
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            commit = true
                            break
                        }
                        total += change.position.x - change.previousPosition.x
                        change.consume()
                        listener().onHorizontalDrag(total / width)
                    }
                } finally {
                    listener().onHorizontalDragEnd(commit)
                }
            }
        }

        Phase.Pinch -> {
            var zoom = 1f
            while (true) {
                val event = awaitPointerEvent()
                zoom *= event.calculateZoom()
                event.changes.forEach { it.consume() }
                if (event.changes.all { !it.pressed }) break
            }
            listener().onPinchEnd(zoom)
        }
    }
}
