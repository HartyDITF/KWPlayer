package fyi.kiyori.modernmediaplayer.feature.video

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import java.util.Locale
import kotlin.math.roundToInt

internal data class TrackOption(
    val group: Tracks.Group,
    val index: Int,
    val label: String,
    val selected: Boolean
)

/** Supported tracks of [type], best video quality first. */
internal fun Tracks.options(type: Int): List<TrackOption> {
    val result = ArrayList<TrackOption>()
    for (group in groups) {
        if (group.type != type) continue
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            result += TrackOption(group, i, trackLabel(type, group.getTrackFormat(i), result.size), group.isTrackSelected(i))
        }
    }
    return if (type == C.TRACK_TYPE_VIDEO) {
        result.sortedByDescending { it.group.getTrackFormat(it.index).height }
    } else {
        result
    }
}

internal fun Player.selectTrack(option: TrackOption) {
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(option.group.type, false)
        .setOverrideForType(TrackSelectionOverride(option.group.mediaTrackGroup, option.index))
        .build()
}

internal fun Player.clearTrackOverride(type: Int) {
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(type)
        .setTrackTypeDisabled(type, false)
        .build()
}

internal fun Player.disableTrackType(type: Int) {
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(type, true)
        .build()
}

internal fun Player.isTrackTypeDisabled(type: Int): Boolean =
    trackSelectionParameters.disabledTrackTypes.contains(type)

internal fun Player.hasTrackOverride(type: Int): Boolean =
    trackSelectionParameters.overrides.keys.any { it.type == type }

/** Frame rate of the selected video track, or null when unknown. */
internal fun Tracks.selectedFrameRate(): Float? =
    selectedFormat(C.TRACK_TYPE_VIDEO)?.frameRate?.takeIf { it > 0f }

private fun Tracks.selectedFormat(type: Int): Format? {
    for (group in groups) {
        if (group.type != type || !group.isSelected) continue
        for (i in 0 until group.length) {
            if (group.isTrackSelected(i)) return group.getTrackFormat(i)
        }
    }
    return null
}

/** Human readable facts about the playing media for the "Информация" page. */
internal fun Tracks.describe(): List<Pair<String, String>> {
    val out = ArrayList<Pair<String, String>>()
    selectedFormat(C.TRACK_TYPE_VIDEO)?.let { f ->
        val size = if (f.width > 0 && f.height > 0) "${f.width}×${f.height}" else null
        val fps = if (f.frameRate > 0f) "${"%.2f".format(Locale.US, f.frameRate)} к/с" else null
        out += "Видео" to listOfNotNull(size, codecName(f.sampleMimeType), fps, bitrateText(f.bitrate)).joinToString(" · ")
    }
    selectedFormat(C.TRACK_TYPE_AUDIO)?.let { f ->
        val rate = if (f.sampleRate > 0) "${f.sampleRate} Гц" else null
        out += "Аудио" to listOfNotNull(codecName(f.sampleMimeType), channelName(f.channelCount), rate, bitrateText(f.bitrate)).joinToString(" · ")
    }
    selectedFormat(C.TRACK_TYPE_TEXT)?.let { f ->
        out += "Субтитры" to listOfNotNull(languageName(f.language), codecName(f.sampleMimeType)).joinToString(" · ")
    }
    return out.filter { it.second.isNotBlank() }
}

private fun trackLabel(type: Int, f: Format, ordinal: Int): String = when (type) {
    C.TRACK_TYPE_VIDEO -> {
        val fps = if (f.frameRate >= 49f) f.frameRate.roundToInt().toString() else ""
        val base = if (f.height > 0) "${f.height}p$fps" else "Видео ${ordinal + 1}"
        bitrateText(f.bitrate)?.let { "$base · $it" } ?: base
    }
    C.TRACK_TYPE_AUDIO -> {
        val parts = listOfNotNull(
            f.label?.takeIf { it.isNotBlank() },
            languageName(f.language),
            channelName(f.channelCount),
            codecName(f.sampleMimeType)
        ).distinct()
        if (parts.isEmpty()) "Дорожка ${ordinal + 1}" else parts.joinToString(" · ")
    }
    else -> {
        val forced = if ((f.selectionFlags and C.SELECTION_FLAG_FORCED) != 0) "принудительные" else null
        val parts = listOfNotNull(f.label?.takeIf { it.isNotBlank() }, languageName(f.language), forced).distinct()
        if (parts.isEmpty()) "Субтитры ${ordinal + 1}" else parts.joinToString(" · ")
    }
}

private fun languageName(code: String?): String? {
    if (code.isNullOrBlank() || code == "und") return null
    val name = Locale.forLanguageTag(code).getDisplayLanguage(Locale.forLanguageTag("ru"))
    return name.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: code
}

private fun channelName(count: Int): String? = when {
    count <= 0 -> null
    count == 1 -> "моно"
    count == 2 -> "стерео"
    count == 6 -> "5.1"
    count == 8 -> "7.1"
    else -> "$count кан."
}

private fun bitrateText(bitrate: Int): String? =
    if (bitrate > 0) "${"%.1f".format(Locale.US, bitrate / 1_000_000f)} Мбит/с" else null

private fun codecName(mime: String?): String? = when (mime) {
    null -> null
    MimeTypes.VIDEO_H264 -> "H.264"
    MimeTypes.VIDEO_H265 -> "H.265"
    MimeTypes.VIDEO_AV1 -> "AV1"
    MimeTypes.VIDEO_VP9 -> "VP9"
    MimeTypes.VIDEO_VP8 -> "VP8"
    MimeTypes.AUDIO_AAC -> "AAC"
    MimeTypes.AUDIO_AC3 -> "AC-3"
    MimeTypes.AUDIO_E_AC3 -> "E-AC-3"
    MimeTypes.AUDIO_OPUS -> "Opus"
    MimeTypes.AUDIO_MPEG -> "MP3"
    MimeTypes.AUDIO_FLAC -> "FLAC"
    MimeTypes.AUDIO_DTS -> "DTS"
    MimeTypes.AUDIO_TRUEHD -> "TrueHD"
    MimeTypes.AUDIO_VORBIS -> "Vorbis"
    else -> mime.substringAfter('/').uppercase(Locale.ROOT)
}

internal fun formatPlayerTime(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val h = total / 3600L
    val m = (total / 60L) % 60L
    val s = total % 60L
    return if (h > 0L) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}
