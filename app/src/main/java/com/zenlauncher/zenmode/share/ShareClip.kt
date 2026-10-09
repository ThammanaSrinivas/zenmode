package com.zenlauncher.zenmode.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import com.zenlauncher.zenmode.Sfx
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ── Share clips ───────────────────────────────────────────────────
// Turns any [ShareArt] into an MP4: its whole arrival, then a held settled frame, with the
// card's own cues mixed into an AAC soundtrack over a soft pad, so the clip sounds the way the
// card does in the app. Built only on platform APIs (MediaCodec, MediaMuxer, EGL), nothing to ship.
//
// Frames are drawn with the same Canvas code the preview uses, into a bitmap, and handed to the
// encoder through an EGL surface — the only way to give each frame its exact presentation time,
// so the clip plays at true speed however fast or slow the phone renders it. Everything runs
// inside one blocking call on one thread (EGL contexts belong to a thread).

internal object ShareClip {
    const val FPS = 30
    const val HOLD_MS = 1_500
    private const val SAMPLE_RATE = 44_100
    private const val MASTER_PEAK = 0.7f
    private const val MAX_LIFT = 4f
    private const val MIME_VIDEO = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val MIME_AUDIO = MediaFormat.MIMETYPE_AUDIO_AAC

    /** Length of [art]'s clip, ms. */
    fun durationMs(art: ShareArt) = art.introMs + HOLD_MS

    /**
     * Encodes [art] into [out]. Blocking: call it off the main thread (runInterruptible), and
     * interrupt the thread to cancel. [onProgress] gets 0..1 as frames are written.
     */
    fun encode(context: Context, art: ShareArt, out: File, onProgress: (Float) -> Unit) {
        val duration = durationMs(art)
        val frames = duration * FPS / 1_000
        val soundtrack = runCatching { encodeAudio(mix(context, art, duration)) }.getOrNull()

        val (width, height) = encoderSize(art.format)
        val format = MediaFormat.createVideoFormat(MIME_VIDEO, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, if (width >= 1080) 8_000_000 else 4_500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MIME_VIDEO)
        var muxer: MediaMuxer? = null
        var gl: GlFrames? = null
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val input = codec.createInputSurface()
            codec.start()
            gl = GlFrames(input, width, height)
            muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val sink = VideoSink(codec, muxer, soundtrack)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val scale = width / art.format.width.toFloat()
            for (i in 0 until frames) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException("clip cancelled")
                sink.drain(endOfStream = false)
                val t = i * 1_000f / FPS
                canvas.save()
                canvas.scale(scale, scale)
                art.draw(canvas, t)
                canvas.restore()
                gl.draw(bitmap, presentationNanos = i * 1_000_000_000L / FPS)
                onProgress((i + 1f) / frames)
            }
            sink.drain(endOfStream = true)
            bitmap.recycle()
        } finally {
            runCatching { codec.stop() }
            codec.release()
            gl?.release()
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    /** The art's own size when the phone's encoder takes it, else 720 wide at the same shape. */
    internal fun encoderSize(format: ShareFormat, supported: (Int, Int) -> Boolean = ::encoderTakes): Pair<Int, Int> {
        val small = 720 to ((720f * format.height / format.width).roundToInt() and 1.inv())
        return if (supported(format.width, format.height)) format.width to format.height else small
    }

    private fun encoderTakes(width: Int, height: Int): Boolean =
        MediaCodecList(MediaCodecList.REGULAR_CODECS)
            .findEncoderForFormat(MediaFormat.createVideoFormat(MIME_VIDEO, width, height)) != null

    // ── Video ─────────────────────────────────────────────────────

    /** Drains the video encoder into the muxer, starting the muxer (and writing the
     * pre-encoded soundtrack) once the encoder has announced its format. */
    private class VideoSink(private val codec: MediaCodec, private val muxer: MediaMuxer, private val audio: EncodedAudio?) {
        private val info = MediaCodec.BufferInfo()
        private var track = -1
        private var started = false

        fun drain(endOfStream: Boolean) {
            if (endOfStream) codec.signalEndOfInputStream()
            var idle = 0
            while (true) {
                val index = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream || ++idle > 300) return
                    }
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        track = muxer.addTrack(codec.outputFormat)
                        val audioTrack = audio?.let { muxer.addTrack(it.format) }
                        muxer.start()
                        started = true
                        if (audio != null && audioTrack != null) audio.writeTo(muxer, audioTrack)
                    }
                    index >= 0 -> {
                        idle = 0
                        val buffer = codec.getOutputBuffer(index)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (buffer != null && info.size > 0 && started) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(track, buffer, info)
                        }
                        codec.releaseOutputBuffer(index, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }
    }

    /** An EGL window onto the encoder's input surface that draws one bitmap per frame. */
    private class GlFrames(surface: Surface, private val width: Int, private val height: Int) {
        private val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        private val context: EGLContext
        private val window: EGLSurface
        private val program: Int
        private val texture: Int
        private var uploaded = false
        private val quad: FloatBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            // x, y, u, v — the bitmap's top row at the top of the frame.
            put(floatArrayOf(-1f, -1f, 0f, 1f, 1f, -1f, 1f, 1f, -1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f))
            position(0)
        }

        init {
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
            val attributes = intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) && count[0] > 0) { "no recordable EGL config" }
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
            window = EGL14.eglCreateWindowSurface(display, configs[0], surface, intArrayOf(EGL14.EGL_NONE), 0)
            check(EGL14.eglMakeCurrent(display, window, window, context)) { "eglMakeCurrent failed" }

            program = link(VERTEX, FRAGMENT)
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            texture = ids[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }

        fun draw(bitmap: Bitmap, presentationNanos: Long) {
            GLES20.glViewport(0, 0, width, height)
            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            if (!uploaded) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                uploaded = true
            } else {
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap)
            }
            val position = GLES20.glGetAttribLocation(program, "aPosition")
            val coord = GLES20.glGetAttribLocation(program, "aCoord")
            quad.position(0)
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 16, quad)
            GLES20.glEnableVertexAttribArray(position)
            quad.position(2)
            GLES20.glVertexAttribPointer(coord, 2, GLES20.GL_FLOAT, false, 16, quad)
            GLES20.glEnableVertexAttribArray(coord)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            EGLExt.eglPresentationTimeANDROID(display, window, presentationNanos)
            EGL14.eglSwapBuffers(display, window)
        }

        fun release() {
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            GLES20.glDeleteProgram(program)
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, window)
            EGL14.eglDestroyContext(display, context)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(display)
        }

        private fun link(vertex: String, fragment: String): Int {
            fun compile(type: Int, source: String) = GLES20.glCreateShader(type).also {
                GLES20.glShaderSource(it, source)
                GLES20.glCompileShader(it)
            }
            return GLES20.glCreateProgram().also {
                GLES20.glAttachShader(it, compile(GLES20.GL_VERTEX_SHADER, vertex))
                GLES20.glAttachShader(it, compile(GLES20.GL_FRAGMENT_SHADER, fragment))
                GLES20.glLinkProgram(it)
            }
        }

        private companion object {
            const val EGL_RECORDABLE_ANDROID = 0x3142
            const val VERTEX = "attribute vec4 aPosition; attribute vec2 aCoord; varying vec2 vCoord;" +
                "void main() { gl_Position = aPosition; vCoord = aCoord; }"
            const val FRAGMENT = "precision mediump float; varying vec2 vCoord; uniform sampler2D uTexture;" +
                "void main() { gl_FragColor = texture2D(uTexture, vCoord); }"
        }
    }

    // ── Audio ─────────────────────────────────────────────────────

    private class EncodedAudio(val format: MediaFormat, val frames: List<Pair<ByteArray, MediaCodec.BufferInfo>>) {
        fun writeTo(muxer: MediaMuxer, track: Int) {
            frames.forEach { (bytes, info) -> muxer.writeSampleData(track, ByteBuffer.wrap(bytes), info) }
        }
    }

    /**
     * The clip's soundtrack as 16-bit mono PCM: a quiet open-fifth pad under the whole clip, and
     * each of the card's cues at its moment and pitch — the same sounds the preview plays.
     */
    internal fun mix(context: Context, art: ShareArt, durationMs: Int): ShortArray {
        val length = durationMs * SAMPLE_RATE / 1_000
        val bus = FloatArray(length)
        pad(bus)
        val decoded = mutableMapOf<Sfx, ShortArray>()
        art.cues.forEach { cue ->
            val samples = decoded.getOrPut(cue.sfx) { readWav(context, cue.sfx) }
            val start = cue.atMs * SAMPLE_RATE / 1_000
            val gain = cue.sfx.volume * 0.95f / Short.MAX_VALUE
            var i = 0
            while (true) {
                val source = i * cue.rate
                val k = source.toInt()
                if (k + 1 >= samples.size || start + i >= length) break
                val frac = source - k
                bus[start + i] += (samples[k] * (1f - frac) + samples[k + 1] * frac) * gain
                i++
            }
        }
        // Master it: the app plays these well under full scale (they sit under a finger), but a
        // clip is listened to on its own, so lift the whole mix to a -3 dB peak.
        var peak = 0f
        for (v in bus) peak = max(peak, abs(v))
        val gain = if (peak > 0f) min(MASTER_PEAK / peak, MAX_LIFT) else 1f
        return ShortArray(length) { (bus[it] * gain).coerceIn(-1f, 1f).let { v -> (v * Short.MAX_VALUE).toInt().toShort() } }
    }

    /** C3 + G3 + a faint E4, swelling in and fading out across the clip. */
    private fun pad(bus: FloatArray) {
        val notes = floatArrayOf(130.81f, 196f, 329.63f)
        val gains = floatArrayOf(0.04f, 0.028f, 0.012f)
        val attack = SAMPLE_RATE * 1.2f
        val release = SAMPLE_RATE * 1.6f
        for (i in bus.indices) {
            val envelope = min(1f, i / attack) * min(1f, (bus.size - i) / release)
            var v = 0f
            for (n in notes.indices) v += gains[n] * sin(2.0 * PI * notes[n] * i / SAMPLE_RATE).toFloat()
            bus[i] += v * envelope
        }
    }

    /** The app's own WAVs: 16-bit mono PCM (scripts/generate_sfx.py), read chunk by chunk. */
    private fun readWav(context: Context, sfx: Sfx): ShortArray {
        val bytes = context.resources.openRawResource(sfx.res).use { it.readBytes() }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var at = 12
        while (at + 8 <= bytes.size) {
            val id = String(bytes, at, 4, Charsets.US_ASCII)
            val size = buffer.getInt(at + 4)
            if (id == "data") {
                val count = min(size, bytes.size - at - 8) / 2
                return ShortArray(count) { buffer.getShort(at + 8 + it * 2) }
            }
            at += 8 + size + (size and 1)
        }
        return ShortArray(0)
    }

    private fun encodeAudio(pcm: ShortArray): EncodedAudio {
        val format = MediaFormat.createAudioFormat(MIME_AUDIO, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val codec = MediaCodec.createEncoderByType(MIME_AUDIO)
        val frames = mutableListOf<Pair<ByteArray, MediaCodec.BufferInfo>>()
        var outputFormat: MediaFormat? = null
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var fed = 0
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException("clip cancelled")
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index)!!
                        buffer.clear()
                        val count = min(buffer.remaining() / 2, pcm.size - fed)
                        val time = fed * 1_000_000L / SAMPLE_RATE
                        if (count <= 0) {
                            codec.queueInputBuffer(index, 0, 0, time, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm, fed, count)
                            codec.queueInputBuffer(index, 0, count * 2, time, 0)
                            fed += count
                        }
                    }
                }
                val out = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = codec.outputFormat
                    out >= 0 -> {
                        val buffer = codec.getOutputBuffer(out)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                            val bytes = ByteArray(info.size)
                            buffer.position(info.offset)
                            buffer.get(bytes)
                            frames += bytes to MediaCodec.BufferInfo().apply {
                                set(0, info.size, info.presentationTimeUs, info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv())
                            }
                        }
                        codec.releaseOutputBuffer(out, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }
        return EncodedAudio(checkNotNull(outputFormat) { "AAC encoder never announced a format" }, frames)
    }
}
