package com.zenlauncher.zenmode.island

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.week.DayMark
import com.zenlauncher.zenmode.coreapi.week.MomentKind
import com.zenlauncher.zenmode.quiet.QuietSession
import com.zenlauncher.zenmode.widget.glance.WidgetDemo

/**
 * Debug builds only: drives the island's real paths from adb, for recordings and QA.
 *
 *   adb shell am broadcast -n com.zenlauncher.zenmode/.island.IslandDemoReceiver --es do <what>
 *
 * quiet / quiet_end   start or end a real Quiet session
 * music / music_stop  a real media session ("Slow Mornings"), seen through the notification listener
 * won                 the week-won celebration (a sample week), as it arrives
 * preview             Settings' "Show me"
 */
class IslandDemoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.getStringExtra("do")) {
            "quiet" -> QuietSession.start(app)
            "quiet_end" -> QuietSession.end(app)
            "music" -> Music.start(app)
            "music_stop" -> Music.stop(app)
            "won" -> {
                val d = WidgetDemo.data()
                val m = d.moment.copy(
                    kind = MomentKind.JUST_WON, goodDays = 4, needed = 0, streakWeeks = d.moment.streakWeeks + 1,
                    marks = listOf(DayMark.GOOD, DayMark.GOOD, DayMark.QUIET, DayMark.GOOD, DayMark.GOOD, DayMark.TO_COME, DayMark.TO_COME)
                )
                IslandStore.putLive(IslandSamples.moment(d.copy(week = d.week.copy(moment = m)), MomentPlan.Reason.CELEBRATION))
                IslandStore.open(MomentItem.ID, IslandSources.ARRIVAL_MILLIS)
            }
            "preview" -> IslandHost.preview()
        }
    }

    /** A real media session with artwork, playing, so the listener's session lookup is what shows it. */
    private object Music {
        private var session: MediaSession? = null
        private const val CHANNEL = "island_demo"

        fun start(ctx: Context) {
            stop(ctx)
            val s = MediaSession(ctx, "IslandDemo")
            s.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, "Slow Mornings")
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "Quiet Rooms")
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, 214_000L)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art())
                    .build()
            )
            s.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                    .setState(PlaybackState.STATE_PLAYING, 38_000L, 1f, SystemClock.elapsedRealtime())
                    .build()
            )
            s.setCallback(object : MediaSession.Callback() {
                override fun onPause() = state(s, PlaybackState.STATE_PAUSED)
                override fun onPlay() = state(s, PlaybackState.STATE_PLAYING)
            })
            s.isActive = true
            session = s
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Island demo", NotificationManager.IMPORTANCE_LOW))
            nm.notify(
                7001,
                Notification.Builder(ctx, CHANNEL)
                    .setSmallIcon(R.drawable.ic_zen_mark_gradient)
                    .setContentTitle("Slow Mornings")
                    .setContentText("Quiet Rooms")
                    .setStyle(Notification.MediaStyle().setMediaSession(s.sessionToken))
                    .build()
            )
        }

        private fun state(s: MediaSession, state: Int) {
            val now = s.controller.playbackState
            s.setPlaybackState(
                PlaybackState.Builder(now).setState(state, s.controller.playbackState?.position ?: 0L, 1f, SystemClock.elapsedRealtime()).build()
            )
        }

        fun stop(ctx: Context) {
            session?.release()
            session = null
            ctx.getSystemService(NotificationManager::class.java).cancel(7001)
        }

        /** Album art: a soft dusk gradient, so the island has a colour to take. */
        private fun art(): Bitmap {
            val b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val p = Paint().apply {
                shader = LinearGradient(0f, 0f, 256f, 256f, intArrayOf(0xFFFFB86B.toInt(), 0xFFE25C7A.toInt(), 0xFF5B3A9E.toInt()), null, Shader.TileMode.CLAMP)
            }
            Canvas(b).drawRect(0f, 0f, 256f, 256f, p)
            p.shader = null
            p.color = 0x55FFFFFF
            Canvas(b).drawCircle(178f, 84f, 38f, p)
            return b
        }
    }
}
