package fyi.kiyori.modernmediaplayer.core.playback
import android.media.audiofx.Equalizer
object PlaybackEqualizer {
 @Volatile private var effect: Equalizer? = null
 fun attach(sessionId: Int) { release(); if (sessionId > 0) effect = runCatching { Equalizer(0, sessionId).apply { enabled = true } }.getOrNull() }
 fun release() { runCatching { effect?.release() }; effect = null }
 fun bandCount() = runCatching { effect?.numberOfBands?.toInt() ?: 0 }.getOrDefault(0)
 fun centerFrequency(band: Int) = runCatching { effect?.getCenterFreq(band.toShort())?.toInt() ?: 0 }.getOrDefault(0)
 fun levelRange(): IntRange { val r = runCatching { effect?.bandLevelRange }.getOrNull(); return if (r != null && r.size >= 2) r[0].toInt()..r[1].toInt() else 0..0 }
 fun level(band: Int) = runCatching { effect?.getBandLevel(band.toShort())?.toInt() ?: 0 }.getOrDefault(0)
 fun setLevel(band: Int, value: Int) { runCatching { val r = levelRange(); effect?.setBandLevel(band.toShort(), value.coerceIn(r.first, r.last).toShort()) } }
 fun nearestBand(freq: Int): Int { val count = bandCount(); if (count == 0) return -1; return (0 until count).minByOrNull { kotlin.math.abs(centerFrequency(it) - freq * 1000) } ?: -1 }
}
