package fyi.kiyori.modernmediaplayer.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import fyi.kiyori.modernmediaplayer.PlaybackService

class PlayerGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF101318)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("KW PLAYER", style = TextStyle(color = ColorProvider(Color(0xFFFF8F1F))))
                Text("Локальное воспроизведение", style = TextStyle(color = ColorProvider(Color(0xFFE9ECF2))))
                Row {
                    Text("PREV", modifier = GlanceModifier.padding(8.dp).clickable(actionSendBroadcast(Intent(context, PlaybackWidgetActionReceiver::class.java).setAction(PlaybackWidgetActionReceiver.ACTION_PREVIOUS))), style = TextStyle(color = ColorProvider(Color.White)))
                    Text("PLAY / PAUSE", modifier = GlanceModifier.padding(8.dp).clickable(actionSendBroadcast(Intent(context, PlaybackWidgetActionReceiver::class.java).setAction(PlaybackWidgetActionReceiver.ACTION_TOGGLE))), style = TextStyle(color = ColorProvider(Color(0xFFFF8F1F))))
                    Text("NEXT", modifier = GlanceModifier.padding(8.dp).clickable(actionSendBroadcast(Intent(context, PlaybackWidgetActionReceiver::class.java).setAction(PlaybackWidgetActionReceiver.ACTION_NEXT))), style = TextStyle(color = ColorProvider(Color.White)))
                }
            }
        }
    }
}
class PlayerGlanceWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = PlayerGlanceWidget() }
class PlaybackWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                when (intent.action) {
                    ACTION_TOGGLE -> if (c.isPlaying) c.pause() else c.play()
                    ACTION_PREVIOUS -> c.seekToPreviousMediaItem()
                    ACTION_NEXT -> c.seekToNextMediaItem()
                }
                MediaController.releaseFuture(future)
            } catch (_: Exception) { runCatching { MediaController.releaseFuture(future) } }
            finally { pending.finish() }
        }, MoreExecutors.directExecutor())
    }
    companion object {
        const val ACTION_TOGGLE = "fyi.kiyori.modernmediaplayer.widget.TOGGLE"
        const val ACTION_PREVIOUS = "fyi.kiyori.modernmediaplayer.widget.PREVIOUS"
        const val ACTION_NEXT = "fyi.kiyori.modernmediaplayer.widget.NEXT"
    }
}