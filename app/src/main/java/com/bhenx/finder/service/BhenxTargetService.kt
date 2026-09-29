package com.bhenx.finder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bhenx.finder.BhenxFinderApp
import com.bhenx.finder.MainActivity
import com.bhenx.finder.R

/**
 * Service de premier plan pour maintenir le Téléphone B détectable
 * même lorsque l'écran est verrouillé ou que l'application est en arrière-plan.
 */
class BhenxTargetService : Service() {

    private val app by lazy { application as BhenxFinderApp }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_TARGET_MODE) {
            app.setTargetMode(false)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Démarre l'annonce BLE et le serveur GATT
        val identity = app.getDeviceIdentity()
        app.bhenxAdvertiser.startAdvertising(identity)
        app.bhenxGattServer.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        app.bhenxAdvertiser.stopAdvertising()
        app.bhenxGattServer.stop()
        app.bhenxRingManager.stopRinging()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BhenxTargetService::class.java).apply {
            action = ACTION_STOP_TARGET_MODE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BHENX FINDER est prêt à retrouver ce téléphone")
            .setContentText("Ce téléphone est disponible pour être retrouvé à proximité.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .addAction(0, "Désactiver", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Détection BHENX FINDER",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient ce téléphone détectable à proximité par BLE"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "bhenx_target_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_STOP_TARGET_MODE = "com.bhenx.finder.ACTION_STOP_TARGET_MODE"

        fun start(context: Context) {
            val intent = Intent(context, BhenxTargetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BhenxTargetService::class.java)
            context.stopService(intent)
        }
    }
}
