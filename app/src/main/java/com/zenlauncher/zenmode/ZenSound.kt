package com.zenlauncher.zenmode

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import androidx.annotation.RawRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The interface sounds. Synthesised by `scripts/generate_sfx.py` into res/raw — retune there.
 *
 * [volume] is the per-sound mix: taps sit far below confirmations so a busy screen never
 * sounds busy. Everything is then scaled by [ZenSound.MASTER].
 */
enum class Sfx(@RawRes val res: Int, val volume: Float) {
    TAP(R.raw.sfx_tap, 0.35f),
    SELECT(R.raw.sfx_select, 0.45f),
    TOGGLE_ON(R.raw.sfx_toggle_on, 0.5f),
    TOGGLE_OFF(R.raw.sfx_toggle_off, 0.45f),
    SHEET_OPEN(R.raw.sfx_sheet_open, 0.4f),
    SHEET_CLOSE(R.raw.sfx_sheet_close, 0.35f),
    STEP(R.raw.sfx_step, 0.45f),
    SUCCESS(R.raw.sfx_success, 0.6f),
    PRO_UNLOCK(R.raw.sfx_pro_unlock, 0.7f),
    ERROR(R.raw.sfx_error, 0.5f),
    COIN(R.raw.sfx_coin, 0.55f),
    THEME_DARK(R.raw.sfx_theme_dark, 0.55f),
    THEME_LIGHT(R.raw.sfx_theme_light, 0.55f),
    ENTER(R.raw.sfx_enter, 0.65f)
}

/**
 * Plays [Sfx] through one shared [SoundPool]. Rules:
 *  · off entirely unless the ringer is on "sound" — silent and vibrate mean silent here too
 *  · the user can switch sounds off in Settings → Sound & motion (haptics stay)
 *  · the same sound never stacks: a repeat inside [MIN_GAP_MS] is dropped
 *  · sonification stream, so it follows the system's own "touch sounds" volume
 */
object ZenSound {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_ENABLED = "ui_sounds_enabled"
    private const val MIN_GAP_MS = 45L
    private const val MASTER = 0.6f

    private var pool: SoundPool? = null
    private var audio: AudioManager? = null
    private val ids = IntArray(Sfx.entries.size)
    private val loaded = BooleanArray(Sfx.entries.size)
    private val lastPlayed = LongArray(Sfx.entries.size)

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled

    /** Called once from [ZenModeApp]. Loading is async; a sound asked for before it's ready is skipped. */
    fun init(context: Context) {
        if (pool != null) return
        val app = context.applicationContext
        _enabled.value = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)
        audio = app.getSystemService(AudioManager::class.java)
        pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
            .also { p ->
                p.setOnLoadCompleteListener { _, sampleId, status ->
                    val index = ids.indexOf(sampleId)
                    if (index >= 0 && status == 0) loaded[index] = true
                }
                Sfx.entries.forEach { sfx -> ids[sfx.ordinal] = p.load(app, sfx.res, 1) }
            }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    fun play(sfx: Sfx) {
        val p = pool ?: return
        if (!_enabled.value || !loaded[sfx.ordinal]) return
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val now = SystemClock.uptimeMillis()
        if (now - lastPlayed[sfx.ordinal] < MIN_GAP_MS) return
        lastPlayed[sfx.ordinal] = now
        val v = sfx.volume * MASTER
        p.play(ids[sfx.ordinal], v, v, 1, 0, 1f)
    }
}
