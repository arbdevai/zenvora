package com.zenvora.gesture.service

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
import com.zenvora.gesture.MainActivity
import com.zenvora.gesture.R
import com.zenvora.gesture.ZenvoraApp

class OverlayForegroundService : Service() {

    private var overlayController: OverlayController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val app = application as ZenvoraApp
        overlayController = OverlayController(
            context = this,
            repository = app.repository,
            onCloseRequested = { stopSelf() }
        ).also { it.show() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayController?.hide()
        overlayController = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kontrol Overlay Zenvora",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga tombol kontrol melayang tetap aktif"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val stopIntent = Intent(this, OverlayForegroundService::class.java).apply {
            action = ACTION_STOP
        }.let {
            PendingIntent.getService(
                this,
                1,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Zenvora Overlay Aktif")
            .setContentText("Panel kontrol gestur siap digunakan")
            .setContentIntent(openIntent)
            .addAction(0, "Tutup Overlay", stopIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "zenvora_overlay_channel"
        private const val NOTIFICATION_ID = 1042
        const val ACTION_STOP = "com.zenvora.gesture.ACTION_STOP_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, OverlayForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayForegroundService::class.java))
        }
    }
}
