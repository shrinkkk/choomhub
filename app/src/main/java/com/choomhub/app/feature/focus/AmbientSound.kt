package com.choomhub.app.feature.focus

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fire and rain for the focus room, made on the phone (no sound files): soft filtered noise
 * for the rain with the odd drip, and a low rumble with crackles and pops for the fire. It
 * plays only while the room is on screen; the on/off choice is remembered.
 */
@Singleton
class AmbientSound @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs get() = context.getSharedPreferences("focus_room", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY, true))
    val enabled: StateFlow<Boolean> = _enabled

    @Volatile private var thread: Thread? = null

    @Volatile private var running = false

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        prefs.edit { putBoolean(KEY, on) }
        if (!on) stop()
    }

    /** Starts playing if it's switched on and not already playing. */
    fun start() {
        if (!_enabled.value || running) return
        running = true
        thread = Thread({ play() }, "focus-ambient").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        thread = null
    }

    private fun play() {
        val size = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            .coerceAtLeast(CHUNK * 2)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(size)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: run {
            running = false
            return
        }
        val synth = FireAndRain(Random(System.nanoTime()))
        val buffer = ShortArray(CHUNK)
        try {
            track.play()
            var fade = 0f
            while (running) {
                for (i in buffer.indices) {
                    fade = (fade + 1f / (RATE * 2)).coerceAtMost(1f)
                    buffer[i] = (synth.next() * fade * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                }
                track.write(buffer, 0, buffer.size)
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private companion object {
        const val KEY = "ambient_on"
        const val RATE = 22_050
        const val CHUNK = 2_048
    }
}

/** The sound itself, one sample at a time (−1..1). Kept separate so it can be tested. */
internal class FireAndRain(private val random: Random, private val rate: Int = 22_050) {
    // Rain: white noise, low-passed into a hiss, with a slow swell.
    private var rainLow = 0f
    private var swell = 0.0

    // Fire: brown-noise rumble plus decaying crackles.
    private var brown = 0f
    private var crackle = 0f
    private var crackleTone = 0f
    private var crackleFilter = 0f

    // A drip now and then.
    private var drip = 0f
    private var dripPhase = 0.0
    private var dripFreq = 0.0

    fun next(): Float {
        val white = random.nextFloat() * 2f - 1f

        rainLow += 0.18f * (white - rainLow)
        swell += 2 * PI * 0.07 / rate
        val rain = rainLow * (0.55f + 0.12f * sin(swell).toFloat())

        brown = (brown + white * 0.02f) * 0.995f
        if (random.nextFloat() < 4f / rate) {
            crackle = 0.4f + random.nextFloat() * 0.6f
            crackleTone = 0.2f + random.nextFloat() * 0.6f
        }
        crackleFilter += crackleTone * (white - crackleFilter)
        val pop = crackleFilter * crackle
        crackle *= exp(-1f / (rate * 0.012f))

        if (random.nextFloat() < 0.6f / rate) {
            drip = 0.25f
            dripPhase = 0.0
            dripFreq = 900.0 + random.nextDouble() * 900.0
        }
        dripPhase += 2 * PI * dripFreq / rate
        dripFreq *= 0.9995
        val dripSample = (sin(dripPhase) * drip).toFloat()
        drip *= exp(-1f / (rate * 0.03f))

        return (rain * 0.32f + brown * 1.6f + pop * 0.5f + dripSample * 0.4f).coerceIn(-1f, 1f)
    }
}
