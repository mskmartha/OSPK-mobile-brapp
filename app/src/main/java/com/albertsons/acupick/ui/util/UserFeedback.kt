package com.albertsons.acupick.ui.util

import android.content.Context
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import com.albertsons.acupick.R
import timber.log.Timber
import java.io.IOException

class UserFeedback(val context: Context) {

    private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var soundPool: SoundPool? = null
    private var successBeep: Int = 0
    private var failureBeep: Int = 0
    private var notificationInterjectionBeep: Int = 0
    private var pickingInterjectionBeep: Int = 0

    fun setSuccessScannedSoundAndHaptic() {
        soundPool?.play(successBeep, 1f, 1f, 1, 0, 1f)
    }

    fun setFailureScannedSoundAndHaptic() {
        vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_MS_ARRAY, NO_REPEAT))
        soundPool?.play(failureBeep, 1f, 1f, 1, 0, 1f)
    }

    fun setInterjectionsSoundAndHaptic() {
        vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_MS_ARRAY, NO_REPEAT))
        soundPool?.play(notificationInterjectionBeep, 1f, 1f, 1, 0, 1f)
    }

    fun setPickingInterjectionsSoundAndHaptic() {
        vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_MS_ARRAY, NO_REPEAT))
        soundPool?.play(pickingInterjectionBeep, 1f, 1f, 1, 0, 1f)
    }

    /** Set up the SoundPool and load the two sounds from assets */
    fun initializeSoundPool() {
        if (soundPool != null) {
            Timber.d("SoundManager: SoundPool is already initialized")
            return
        }

        Timber.d("SoundManager: Initializing SoundPool")
        soundPool = SoundPool.Builder()
            .setMaxStreams(2) // Allow up to 2 sounds to play simultaneously
            .build()

        try {
            successBeep = soundPool?.load(context, R.raw.success_beep, 1) ?: 0
        } catch (e: IOException) {
            Timber.e("SoundManager: Error loading success Beep from assets: ${e.message}")
        }

        try {
            failureBeep = soundPool?.load(context, R.raw.failure_beep, 1) ?: 0
        } catch (e: IOException) {
            Timber.e("SoundManager: Error loading failure beep from assets: ${e.message}")
        }

        try {
            notificationInterjectionBeep = soundPool?.load(context, R.raw.notification_hybrid, 1) ?: 0
        } catch (e: IOException) {
            Timber.e("SoundManager: Error loading notification_hybrid from assets: ${e.message}")
        }

        try {
            pickingInterjectionBeep = soundPool?.load(context, R.raw.notification_picking_hybrid, 1) ?: 0
        } catch (e: IOException) {
            Timber.e("SoundManager: Error loading notification_picking_hybrid from assets: ${e.message}")
        }
    }

    fun releaseSoundPool() {
        Timber.d("SoundManager: releasing SoundPool")
        soundPool?.release()
        soundPool = null
    }

    companion object {
        private val VIBRATION_MS_ARRAY = longArrayOf(0, 200, 0, 200)
        private const val NO_REPEAT = -1
    }

    enum class SoundAndHaptic(val shortName: String) {
        ArrivalInterjection("ArrivalInterjection"),
        PickingInterjection("PickingInterjection"),
    }
}
