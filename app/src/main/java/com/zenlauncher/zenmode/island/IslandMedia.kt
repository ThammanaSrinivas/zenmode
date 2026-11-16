package com.zenlauncher.zenmode.island

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import com.zenlauncher.zenmode.ZenNotificationListenerService
import com.zenlauncher.zenmode.ui.theme.IslandPalette

/**
 * Music and podcasts in the island, from whatever app is playing. Reads the media sessions the
 * system lets a notification listener see: title, artist, artwork and position, and drives
 * play, pause and skip through the session's own controls.
 *
 * Paused music stays a minute, then leaves; nothing playing means nothing shown.
 */
class IslandMedia(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, ZenNotificationListenerService::class.java)
    private var controllers: List<MediaController> = emptyList()
    private var shown: MediaController? = null
    private val callbacks = HashMap<MediaController, MediaController.Callback>()
    private var lastArt: Bitmap? = null
    private var lastTint: Int = IslandPalette.zenInt

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { list -> track(list.orEmpty()) }
    private val leaveAfterPause = Runnable { publish() }

    fun start() {
        val m = manager ?: return
        runCatching {
            m.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler)
            track(m.getActiveSessions(listener))
        }
    }

    fun stop() {
        runCatching { manager?.removeOnActiveSessionsChangedListener(sessionsChanged) }
        callbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }
        callbacks.clear()
        controllers = emptyList()
        handler.removeCallbacksAndMessages(null)
        IslandStore.removeLive(MediaItem.ID)
    }

    fun toggle() {
        val c = shown ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() = shown?.transportControls?.skipToNext()
    fun previous() = shown?.transportControls?.skipToPrevious()

    fun open() = runCatching { shown?.sessionActivity?.send() }

    private fun track(list: List<MediaController>) {
        val gone = callbacks.keys.filter { old -> list.none { it.sessionToken == old.sessionToken } }
        gone.forEach { c -> callbacks.remove(c)?.let { cb -> runCatching { c.unregisterCallback(cb) } } }
        list.forEach { c ->
            if (callbacks.keys.none { it.sessionToken == c.sessionToken }) {
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                    override fun onSessionDestroyed() = track(controllers.filter { it.sessionToken != c.sessionToken })
                }
                runCatching { c.registerCallback(cb, handler) }
                callbacks[c] = cb
            }
        }
        controllers = list
        publish()
    }

    /** The session to show: one that plays, else the one that paused most recently. */
    private fun publish() {
        handler.removeCallbacks(leaveAfterPause)
        val playing = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val paused = controllers
            .filter { it.playbackState?.state == PlaybackState.STATE_PAUSED }
            .maxByOrNull { it.playbackState?.lastPositionUpdateTime ?: 0L }
        val c = playing ?: paused?.takeIf { pausedRecently(it) }
        shown = c
        val meta = c?.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (c == null || meta == null || title.isNullOrBlank() || !IslandPrefs.liveOn(context)) {
            IslandStore.removeLive(MediaItem.ID)
            return
        }
        val state = c.playbackState
        val art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val small = art?.let { scaled(it) }
        IslandStore.putLive(
            MediaItem(
                pkg = c.packageName,
                appLabel = IslandNotes.appLabel(context, c.packageName),
                title = title,
                artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty(),
                art = small,
                tint = small?.let { tintOf(it) } ?: IslandPalette.zenInt,
                playing = state?.state == PlaybackState.STATE_PLAYING,
                positionMs = state?.position ?: 0L,
                durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
                positionAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
                speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f
            )
        )
        if (playing == null) handler.postDelayed(leaveAfterPause, PAUSED_STAYS_MILLIS)
    }

    private fun pausedRecently(c: MediaController): Boolean {
        val at = c.playbackState?.lastPositionUpdateTime ?: return false
        return SystemClock.elapsedRealtime() - at < PAUSED_STAYS_MILLIS
    }

    /** Artwork small enough to hold (the island draws it at 56dp at most). */
    private fun scaled(src: Bitmap): Bitmap {
        if (src === lastArt) return src
        val px = (ART_DP * context.resources.displayMetrics.density).toInt()
        val out = if (src.width <= px && src.height <= px) src else src.scale(px, px)
        lastTint = averageTint(out)
        lastArt = out
        return out
    }

    private fun tintOf(art: Bitmap): Int = if (art === lastArt) lastTint else averageTint(art)

    /** The artwork's average colour, lifted so it reads on the island's black. */
    private fun averageTint(art: Bitmap): Int {
        val avg = runCatching { art.scale(1, 1).getPixel(0, 0) }.getOrDefault(IslandPalette.zenInt)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(avg, hsl)
        hsl[1] = hsl[1].coerceIn(0.35f, 0.85f)
        hsl[2] = hsl[2].coerceIn(0.62f, 0.74f)
        return ColorUtils.HSLToColor(hsl).let { Color.rgb(Color.red(it), Color.green(it), Color.blue(it)) }
    }

    companion object {
        const val PAUSED_STAYS_MILLIS = 60_000L
        private const val ART_DP = 56
    }
}
