package com.bhenx.finder.bluetooth

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestionnaire audio de sonnerie d'urgence locale pour le téléphone B.
 * Utilise les APIs officielles Android RingtoneManager et Vibrator.
 * S'arrête automatiquement après 30 secondes pour préserver la batterie.
 */
class BhenxRingManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var currentRingtone: Ringtone? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isRinging = MutableStateFlow(false)
    val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

    private val autoStopRunnable = Runnable {
        stopRinging()
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @Synchronized
    fun startRinging(onTimeout: (() -> Unit)? = null): Boolean {
        if (_isRinging.value) return true

        return try {
            val alertUri = RingtoneManager.getActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE
            ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            if (ringtone != null) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.isLooping = true
                }
                ringtone.play()
                currentRingtone = ringtone
            }

            // Vibration cadencée pour aider à localiser physiquement le téléphone
            startVibration()

            _isRinging.value = true

            // Arrêt automatique configurable après 30 secondes
            mainHandler.removeCallbacks(autoStopRunnable)
            mainHandler.postDelayed({
                stopRinging()
                onTimeout?.invoke()
            }, AUTO_STOP_DURATION_MS)

            true
        } catch (_: Exception) {
            _isRinging.value = false
            false
        }
    }

    @Synchronized
    fun stopRinging() {
        mainHandler.removeCallbacks(autoStopRunnable)
        try {
            currentRingtone?.stop()
            currentRingtone = null
        } catch (_: Exception) {
            // Ignorer
        }

        stopVibration()
        _isRinging.value = false
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 600, 400, 600, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
            // Ignorer si vibration non autorisée
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
            // Ignorer
        }
    }

    fun isSilentOrMuted(): Boolean {
        val mode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        return mode == AudioManager.RINGER_MODE_SILENT || mode == AudioManager.RINGER_MODE_VIBRATE
    }

    companion object {
        const val AUTO_STOP_DURATION_MS = 30_000L // 30 secondes
    }
}
