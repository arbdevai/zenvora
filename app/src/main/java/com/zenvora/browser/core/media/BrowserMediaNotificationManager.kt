package com.zenvora.browser.core.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import com.zenvora.browser.MainActivity
import com.zenvora.browser.R
import org.mozilla.geckoview.GeckoSession

class BrowserMediaNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "zenvora_media_playback_channel"
        private const val NOTIFICATION_ID = 7701
        private const val ACTION_PLAY = "com.zenvora.browser.action.MEDIA_PLAY"
        private const val ACTION_PAUSE = "com.zenvora.browser.action.MEDIA_PAUSE"
        private const val ACTION_NEXT = "com.zenvora.browser.action.MEDIA_NEXT"
        private const val ACTION_PREV = "com.zenvora.browser.action.MEDIA_PREV"
        private const val ACTION_STOP = "com.zenvora.browser.action.MEDIA_STOP"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var androidMediaSession: MediaSession? = null
    private var activeGeckoMediaSession: org.mozilla.geckoview.MediaSession? = null

    private var currentTitle: String = "Zenvora Media"
    private var currentArtist: String = "Zenvora"
    private var isPlaying: Boolean = false
    private var isReceiverRegistered = false

    private val mediaActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val geckoMedia = activeGeckoMediaSession ?: return
            when (action) {
                ACTION_PLAY -> {
                    geckoMedia.play()
                    updatePlaybackState(playing = true)
                }
                ACTION_PAUSE -> {
                    geckoMedia.pause()
                    updatePlaybackState(playing = false)
                }
                ACTION_NEXT -> geckoMedia.nextTrack()
                ACTION_PREV -> geckoMedia.previousTrack()
                ACTION_STOP -> {
                    geckoMedia.stop()
                    dismissNotification()
                }
            }
        }
    }

    init {
        createNotificationChannel()
        initAndroidMediaSession()
        registerReceiverIfNeeded()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pemutaran Media & Video",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kontrol pemutar video dan audio di statusbar & panel cepat Android"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initAndroidMediaSession() {
        try {
            val session = MediaSession(context, "ZenvoraMediaSession")
            session.setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    activeGeckoMediaSession?.play()
                    updatePlaybackState(playing = true)
                }

                override fun onPause() {
                    activeGeckoMediaSession?.pause()
                    updatePlaybackState(playing = false)
                }

                override fun onSkipToNext() {
                    activeGeckoMediaSession?.nextTrack()
                }

                override fun onSkipToPrevious() {
                    activeGeckoMediaSession?.previousTrack()
                }

                override fun onStop() {
                    activeGeckoMediaSession?.stop()
                    dismissNotification()
                }

                override fun onSeekTo(pos: Long) {
                    activeGeckoMediaSession?.seekTo(pos / 1000.0, true)
                }
            })
            androidMediaSession = session
        } catch (_: Exception) {
            // Abaikan jika layanan media sistem dibatasi
        }
    }

    private fun registerReceiverIfNeeded() {
        if (isReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(ACTION_PLAY)
                addAction(ACTION_PAUSE)
                addAction(ACTION_NEXT)
                addAction(ACTION_PREV)
                addAction(ACTION_STOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(mediaActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(mediaActionReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (_: Exception) {
            // Abaikan jika gagal mendaftarkan receiver
        }
    }

    fun createGeckoMediaDelegate(fallbackPageTitleProvider: () -> String): org.mozilla.geckoview.MediaSession.Delegate {
        return object : org.mozilla.geckoview.MediaSession.Delegate {
            override fun onActivated(
                session: GeckoSession,
                mediaSession: org.mozilla.geckoview.MediaSession
            ) {
                activeGeckoMediaSession = mediaSession
                if (currentTitle == "Zenvora Media") {
                    val pageTitle = fallbackPageTitleProvider().trim()
                    if (pageTitle.isNotEmpty()) {
                        currentTitle = pageTitle
                    }
                }
                androidMediaSession?.isActive = true
                updatePlaybackState(playing = true)
            }

            override fun onDeactivated(
                session: GeckoSession,
                mediaSession: org.mozilla.geckoview.MediaSession
            ) {
                if (activeGeckoMediaSession == mediaSession) {
                    activeGeckoMediaSession = null
                    dismissNotification()
                }
            }

            override fun onMetadata(
                session: GeckoSession,
                mediaSession: org.mozilla.geckoview.MediaSession,
                meta: org.mozilla.geckoview.MediaSession.Metadata
            ) {
                activeGeckoMediaSession = mediaSession
                val metaTitle = meta.title?.trim().orEmpty()
                val metaArtist = meta.artist?.trim().orEmpty()
                val fallback = fallbackPageTitleProvider().trim()

                currentTitle = when {
                    metaTitle.isNotEmpty() -> metaTitle
                    fallback.isNotEmpty() -> fallback
                    else -> "Video Web Zenvora"
                }
                currentArtist = if (metaArtist.isNotEmpty()) metaArtist else "Zenvora"

                updateAndroidMediaMetadata()
                showOrUpdateNotification()
            }

            override fun onPlay(
                session: GeckoSession,
                mediaSession: org.mozilla.geckoview.MediaSession
            ) {
                activeGeckoMediaSession = mediaSession
                val fallback = fallbackPageTitleProvider().trim()
                if (currentTitle == "Zenvora Media" && fallback.isNotEmpty()) {
                    currentTitle = fallback
                }
                updatePlaybackState(playing = true)
            }

            override fun onPause(
                session: GeckoSession,
                mediaSession: org.mozilla.geckoview.MediaSession
            ) {
                activeGeckoMediaSession = mediaSession
                updatePlaybackState(playing = false)
            }

            override fun onStop(
                session: GeckoSession,
                mediaSession: org.mozilla.geckoview.MediaSession
            ) {
                if (activeGeckoMediaSession == mediaSession) {
                    dismissNotification()
                }
            }
        }
    }

    private fun updateAndroidMediaMetadata() {
        try {
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
                .build()
            androidMediaSession?.setMetadata(metadata)
        } catch (_: Exception) {
            // Abaikan error metadata
        }
    }

    private fun updatePlaybackState(playing: Boolean) {
        isPlaying = playing
        try {
            val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val playbackState = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_SEEK_TO
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
            androidMediaSession?.setPlaybackState(playbackState)
            androidMediaSession?.isActive = true
            updateAndroidMediaMetadata()
            showOrUpdateNotification()
        } catch (_: Exception) {
            // Abaikan jika gagal memperbarui playback state
        }
    }

    private fun showOrUpdateNotification() {
        try {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val contentPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, pendingFlags)

            val prevIntent = PendingIntent.getBroadcast(
                context,
                101,
                Intent(ACTION_PREV).setPackage(context.packageName),
                pendingFlags
            )
            val playPauseIntent = PendingIntent.getBroadcast(
                context,
                102,
                Intent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY).setPackage(context.packageName),
                pendingFlags
            )
            val nextIntent = PendingIntent.getBroadcast(
                context,
                103,
                Intent(ACTION_NEXT).setPackage(context.packageName),
                pendingFlags
            )

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val prevAction = Notification.Action.Builder(
                android.R.drawable.ic_media_previous,
                "Sebelumnya",
                prevIntent
            ).build()

            val playPauseAction = Notification.Action.Builder(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Jeda" else "Putar",
                playPauseIntent
            ).build()

            val nextAction = Notification.Action.Builder(
                android.R.drawable.ic_media_next,
                " Berikutnya",
                nextIntent
            ).build()

            val mediaStyle = Notification.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
            androidMediaSession?.sessionToken?.let { token ->
                mediaStyle.setMediaSession(token)
            }

            val notification = builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setContentIntent(contentPendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .addAction(prevAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .setStyle(mediaStyle)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Abaikan jika izin notifikasi ditolak pengguna
        }
    }

    fun dismissNotification() {
        isPlaying = false
        try {
            androidMediaSession?.isActive = false
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
            // Abaikan
        }
    }

    fun release() {
        dismissNotification()
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(mediaActionReceiver)
                isReceiverRegistered = false
            }
            androidMediaSession?.release()
            androidMediaSession = null
        } catch (_: Exception) {
            // Abaikan
        }
    }
}
