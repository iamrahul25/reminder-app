package com.example.first_app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.first_app.R
import com.example.first_app.reminder.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Ringing(val reminderId: Int, val title: String, val startedAt: Long)

/** Foreground service that loops the alarm sound and vibration until stopped or snoozed. */
class AlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopSelf() }
    private var player: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    private val beep = object : Runnable {
        override fun run() {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1_000)
            handler.postDelayed(this, 1_500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ringing = Ringing(
            reminderId = intent?.getIntExtra(EXTRA_REMINDER_ID, -1) ?: -1,
            title = intent?.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_title),
            startedAt = System.currentTimeMillis(),
        )
        _ringing.value = ringing
        startInForeground(buildNotification(ringing))

        stopSound()
        startSound(intent?.getStringExtra(EXTRA_SOUND_URI)?.let(Uri::parse))
        startVibration()
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AUTO_STOP_MS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopSound()
        vibrator?.cancel()
        _ringing.value = null
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startSound(customUri: Uri?) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidates = listOfNotNull(
            customUri,
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        for (uri in candidates) {
            val mediaPlayer = MediaPlayer()
            try {
                mediaPlayer.setAudioAttributes(attributes)
                mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
                mediaPlayer.setDataSource(this, uri)
                mediaPlayer.isLooping = true
                mediaPlayer.prepare()
                mediaPlayer.start()
                player = mediaPlayer
                return
            } catch (e: Exception) {
                Log.w(TAG, "Could not play $uri", e)
                mediaPlayer.release()
            }
        }
        // No playable ringtone on the device (common on emulators): fall back to a beep.
        toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }.getOrNull()
        handler.post(beep)
    }

    private fun stopSound() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        handler.removeCallbacks(beep)
        toneGenerator?.release()
        toneGenerator = null
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        this.vibrator = vibrator
        val pattern = longArrayOf(0, 800, 800)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, 0),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, 0),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
            else -> vibrator.vibrate(pattern, 0)
        }
    }

    private fun buildNotification(ringing: Ringing): Notification {
        val alarmScreen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(ringing.title)
            .setContentText(getString(R.string.ringing_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(alarmScreen)
            .setFullScreenIntent(alarmScreen, true)
            .addAction(0, getString(R.string.stop), actionIntent(ringing, AlarmActionReceiver.ACTION_STOP, 0))
            .addAction(0, getString(R.string.snooze_5m), actionIntent(ringing, AlarmActionReceiver.ACTION_SNOOZE, 5))
            .addAction(0, getString(R.string.snooze_1h), actionIntent(ringing, AlarmActionReceiver.ACTION_SNOOZE, 60))
            .build()
    }

    private fun actionIntent(ringing: Ringing, action: String, minutes: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            minutes,
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(action)
                .putExtra(AlarmActionReceiver.EXTRA_REMINDER_ID, ringing.reminderId)
                .putExtra(AlarmActionReceiver.EXTRA_MINUTES, minutes),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val CHANNEL_ID = "alarms"
        private const val NOTIFICATION_ID = 1001
        private const val AUTO_STOP_MS = 10 * 60 * 1000L
        private const val EXTRA_REMINDER_ID = "reminder_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_SOUND_URI = "sound_uri"
        private const val TAG = "AlarmService"

        private val _ringing = MutableStateFlow<Ringing?>(null)
        /** The reminder currently ringing, or null when silent. */
        val ringing: StateFlow<Ringing?> = _ringing

        fun start(context: Context, reminder: Reminder) {
            val intent = Intent(context, AlarmService::class.java)
                .putExtra(EXTRA_REMINDER_ID, reminder.id)
                .putExtra(EXTRA_TITLE, reminder.title)
                .putExtra(EXTRA_SOUND_URI, reminder.soundUri)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }

        /** The service plays the sound itself, so the channel is silent to avoid a double sound. */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_description)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
