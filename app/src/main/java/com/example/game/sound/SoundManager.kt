package com.example.game.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural audio synthesizer and haptics manager for Puffy Wings.
 * Generates custom 8-bit/chime arcade sounds in real-time with zero external assets.
 */
class SoundManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("puffy_wings_prefs", Context.MODE_PRIVATE)
    var isMuted: Boolean = prefs.getBoolean("is_muted", false)
        private set

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Pre-rendered audio sound buffers
    private val flapBuffer: ShortArray by lazy { generateSweep(startFreq = 380f, endFreq = 720f, durationMs = 85, peak = 0.55f) }
    private val scoreBuffer: ShortArray by lazy { generateScoreChime() }
    private val hitBuffer: ShortArray by lazy { generateHitThud() }
    private val gameOverBuffer: ShortArray by lazy { generateGameOverJingle() }
    private val clickBuffer: ShortArray by lazy { generateSweep(startFreq = 700f, endFreq = 950f, durationMs = 35, peak = 0.4f) }
    private val medalBuffer: ShortArray by lazy { generateFanfare() }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        prefs.edit().putBoolean("is_muted", isMuted).apply()
        if (!isMuted) {
            playClick()
        }
        return isMuted
    }

    fun playFlap() {
        if (isMuted) return
        scope.launch {
            playSound(flapBuffer)
        }
        vibrate(12)
    }

    fun playScore() {
        if (isMuted) return
        scope.launch {
            playSound(scoreBuffer)
        }
        vibrate(25)
    }

    fun playHit() {
        if (isMuted) return
        scope.launch {
            playSound(hitBuffer)
        }
        vibrate(80)
    }

    fun playGameOver() {
        if (isMuted) return
        scope.launch {
            playSound(gameOverBuffer)
        }
    }

    fun playClick() {
        if (isMuted) return
        scope.launch {
            playSound(clickBuffer)
        }
        vibrate(15)
    }

    fun playMedal() {
        if (isMuted) return
        scope.launch {
            playSound(medalBuffer)
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore vibration errors on devices without vibrator hardware
        }
    }

    private fun playSound(samples: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            // Track will naturally stop and can be released after duration
            Thread.sleep((samples.size * 1000L / sampleRate) + 20)
            audioTrack.release()
        } catch (_: Exception) {
            // Ignore audio track initializations failures
        }
    }

    private fun generateSweep(startFreq: Float, endFreq: Float, durationMs: Int, peak: Float): ShortArray {
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val currentFreq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * currentFreq / sampleRate

            // Envelope: fast linear attack, exponential decay
            val envelope = when {
                t < 0.15 -> t / 0.15
                else -> (1.0 - (t - 0.15) / 0.85).coerceAtLeast(0.0)
            }
            val sample = sin(phase) * envelope * peak
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateScoreChime(): ShortArray {
        val durationMs = 170
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        val note1Freq = 880.0   // A5
        val note2Freq = 1318.5  // E6
        val split = totalSamples / 2

        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until totalSamples) {
            val sampleVal: Double
            if (i < split) {
                val t = i.toDouble() / split
                phase1 += 2.0 * PI * note1Freq / sampleRate
                val env = (1.0 - t * 0.7) * 0.5
                sampleVal = (sin(phase1) + 0.3 * sin(phase1 * 2)) * env
            } else {
                val t = (i - split).toDouble() / (totalSamples - split)
                phase2 += 2.0 * PI * note2Freq / sampleRate
                val env = (1.0 - t) * 0.6
                sampleVal = (sin(phase2) + 0.35 * sin(phase2 * 2)) * env
            }
            buffer[i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateHitThud(): ShortArray {
        val durationMs = 190
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = (280.0 - 200.0 * t).coerceAtLeast(50.0)
            phase += 2.0 * PI * freq / sampleRate
            val env = Math.exp(-6.0 * t) * 0.7
            val sampleVal = (sin(phase) + 0.25 * sin(phase * 1.5)) * env
            buffer[i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateGameOverJingle(): ShortArray {
        val notes = doubleArrayOf(659.25, 587.33, 523.25, 392.00) // E5, D5, C5, G4
        val noteDuration = 100
        val noteSamples = (sampleRate * (noteDuration / 1000.0)).toInt()
        val totalSamples = noteSamples * notes.size
        val buffer = ShortArray(totalSamples)

        for (n in notes.indices) {
            val freq = notes[n]
            var phase = 0.0
            val offset = n * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / noteSamples
                phase += 2.0 * PI * freq / sampleRate
                val env = (1.0 - t * 0.8) * 0.45
                val sampleVal = (sin(phase) + 0.2 * sin(phase * 2)) * env
                buffer[offset + i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return buffer
    }

    private fun generateFanfare(): ShortArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        val noteDuration = 80
        val noteSamples = (sampleRate * (noteDuration / 1000.0)).toInt()
        val totalSamples = noteSamples * notes.size
        val buffer = ShortArray(totalSamples)

        for (n in notes.indices) {
            val freq = notes[n]
            var phase = 0.0
            val offset = n * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / noteSamples
                phase += 2.0 * PI * freq / sampleRate
                val env = (1.0 - t * 0.6) * 0.5
                val sampleVal = (sin(phase) + 0.25 * sin(phase * 2)) * env
                buffer[offset + i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return buffer
    }
}
