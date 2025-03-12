package statusbar.finder.hook.observe

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.BigTextStyle
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import cn.lyric.getter.api.data.ExtraData
import cn.zhaiyifan.lyric.LyricUtils
import cn.zhaiyifan.lyric.model.Lyric
import cn.zhaiyifan.lyric.model.Lyric.Sentence
import com.google.gson.Gson
import statusbar.finder.BuildConfig
import statusbar.finder.CSLyricHelper
import statusbar.finder.CSLyricHelper.PlayInfo
import statusbar.finder.app.LyricsActivity
import statusbar.finder.app.MusicListenerService.LrcUpdateThread
import statusbar.finder.app.event.LyricSentenceUpdate
import statusbar.finder.app.event.LyricsChange
import statusbar.finder.config.Config
import statusbar.finder.data.db.DatabaseHelper
import statusbar.finder.data.model.MediaInfo
import statusbar.finder.hook.tool.EventTool
import statusbar.finder.misc.Constants.*
import java.util.*


/**
 * LyricGetterExt - statusbar.finder.hook.observe
 * @description 参考自 https://github.com/xiaowine/Lyric-Getter/blob/master/app/src/main/kotlin/cn/lyric/getter/observe/MediaSessionObserve.kt
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/17 02:41
 */
@SuppressLint("StaticFieldLeak", "MissingPermission")
object MediaSessionManagerHelper {
    private lateinit var context: Context
    private lateinit var config: Config
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeControllers: MutableMap<MediaController, MediaController.Callback> = mutableMapOf()
    private var lastSentenceMap: MutableMap<String, Sentence> = mutableMapOf()
    private var requiredLrcTitle: MutableMap<String, String> = mutableMapOf()
    private var curLrcUpdateThread: MutableMap<String, LrcUpdateThread> = mutableMapOf()
    private var currentLyric: MutableMap<String, Lyric?> = mutableMapOf()
    private var lastMetadata: MutableMap<String, MediaMetadata?> = mutableMapOf()
    private var lastState: MutableMap<String, PlaybackState> = mutableMapOf()
    private var lastLyricLine: MutableMap<String, String> = mutableMapOf()
    private var playInfo: PlayInfo = PlayInfo("", BuildConfig.APPLICATION_ID)
    private val user: UserHandle = UserHandle.getUserHandleForUid(android.os.Process.myUid())
    private lateinit var notificationManager: NotificationManager
    private val noticeChannelId = "${BuildConfig.APPLICATION_ID.replace(".", "_")}_info"
    private lateinit var pendingIntent: PendingIntent
    private val gson = Gson()
    private var lastBroadcastIntent: Intent? = null


    private var activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        controllers?.let {
            if (controllers.isEmpty()) return@OnActiveSessionsChangedListener
            activeControllers.forEach { it.key.unregisterCallback(it.value) }
            activeControllers.clear()
            currentLyric.clear()
            lastMetadata.clear()
            lastSentenceMap.clear()
            val targetPackages = config.targetPackages.split(";")
            for (controller in controllers) {
                if (targetPackages.contains(controller.packageName)) {
                    lastMetadata.remove(controller.packageName)
                    val callback = MediaControllerCallback(controller)
                    activeControllers[controller] = callback
                    controller.registerCallback(callback)
                    controller.metadata?.let {
                        callback.onMetadataChanged(controller.metadata)
                        callback.onPlaybackStateChanged(controller.playbackState)
                    }
                }
            }
        }
    }

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == MSG_LYRIC_UPDATE_DONE && requiredLrcTitle.containsValue(msg.data.getString("title", ""))) {
                val packageName = requiredLrcTitle.filterValues { it == msg.data.getString("title", "") }.keys.first()
                val lyric = msg.obj as Lyric?
                currentLyric[packageName] = lyric
                lastLyricLine.remove(packageName)
                val data = LyricsChange.Data(lyric)
                val intent = Intent(
                    BROADCAST_LYRICS_CHANGED
                )
                intent.setPackage(BuildConfig.APPLICATION_ID)
                LyricsChange.getInstance().notifyResult(data)
                intent.putExtra("data", gson.toJson(data))
                context.sendBroadcastAsUser(intent, user)
                lastBroadcastIntent = intent
            }
        }
    }

    private class MediaControllerCallback(private val controller: MediaController) :
        MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                val info = it.toMediaInfo()
                if (it == lastMetadata[controller.packageName]) return
                notificationManager = context.getSystemService(NotificationManager::class.java)
                val notification = Notification.Builder(context, noticeChannelId)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(info.title)
                    .setContentText(
                        "${info.artist} - ${info.album}"
                    )
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
                notificationManager.notify(NOTIFICATION_ID_LRC, notification)
                requiredLrcTitle[controller.packageName] = info.title
                updateLyrics(controller.packageName)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            state?.let {
                if (lastState[controller.packageName] == it) return
                lastState[controller.packageName] = it

                lastSentenceMap.remove(controller.packageName)
                if (state.state == PlaybackState.STATE_PLAYING) {
                    lastMetadata[controller.packageName]?.let { metadata ->
                        val info = metadata.toMediaInfo()
                        val notification = Notification.Builder(context, noticeChannelId)
                            .setSmallIcon(android.R.drawable.ic_media_play)
                            .setContentTitle(info.title)
                            .setContentText(
                                info.artist +
                                        (if (info.album.isBlank()) "" else
                                            " - ${info.album}")
                            )
                            .setOngoing(true)
                            .setOnlyAlertOnce(true)
                            .build()
                        notificationManager.notify(NOTIFICATION_ID_LRC, notification)
                    }
                    handler.post(lyricUpdateRunnable)
                } else {
                    handler.removeCallbacks(lyricUpdateRunnable)
                    EventTool.cleanLyric()
                    CSLyricHelper.pauseAsUser(context, playInfo, user)
                    notificationManager.cancel(NOTIFICATION_ID_LRC)
                }
            }
        }
    }


    private val lyricUpdateRunnable: Runnable = object : Runnable {
        override fun run() {
            for ((controller, _) in activeControllers) {
                val playbackState = controller.playbackState
                if (playbackState?.state != PlaybackState.STATE_PLAYING) {
                    playInfo.isPlaying = false
                    CSLyricHelper.pauseAsUser(context, playInfo, user)
                    EventTool.cleanLyric()
                    return
                }
                updateLyric(controller, playbackState.position, context)
            }
            handler.postDelayed(this, 100)
        }
    }

    fun updateLyric(controller: MediaController, position: Long, context: Context) {
        currentLyric[controller.packageName]?.let {
            val sentenceIndex = LyricUtils.getSentenceIndex(it.sentenceList, position, 0, it.offset)
            if (sentenceIndex < 0) return
            val sentence =  it.sentenceList[sentenceIndex]
            val translatedSentence = LyricUtils.getSentence(it.translatedSentenceList, position, 0, it.offset)
            if (sentence.content.isBlank()) return
            if (sentence == lastSentenceMap[controller.packageName]) return
            val delay: Int = it.calcDelay(controller, position)
            val sentenceContent = sentence.content.trim()
            val translatedContent = translatedSentence?.content?.trim() ?: ""

            var curLyric = when (config.translateDisplayType) {
                "translated" -> translatedContent.ifBlank { sentenceContent }
                "both" -> if (translatedContent.isBlank()) sentenceContent else "$sentenceContent\n\r$translatedContent"
                else -> sentenceContent
            }
            if (config.forceRepeat &&
                lastLyricLine[controller.packageName] == curLyric) {
                curLyric = insertZeroWidthSpace(curLyric)
            }
            playInfo.isPlaying = true
            val data = LyricSentenceUpdate.Data(
                sentence.content,
                translatedSentence?.content,
                sentenceIndex,
                delay
            )
            val intent = Intent(
                BROADCAST_LYRIC_SENTENCE_UPDATE
            )
            intent.setPackage(BuildConfig.APPLICATION_ID)
            LyricSentenceUpdate.getInstance().notifyLyrics(data)
            intent.putExtra("data", gson.toJson(data))
            context.sendBroadcastAsUser(intent, user)
            val notification = Notification.Builder(MediaSessionManagerHelper.context, noticeChannelId)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(
                    "${it.originalMediaInfo.title} - ${it.originalMediaInfo.artist}" +
                            (if (it.originalMediaInfo.album.isBlank()) "" else " - ${it.originalMediaInfo.album}")
                )
                .setContentText(
                    "${it.title} - ${it.artist}" +
                            (if (it.album.isBlank()) "" else " - ${it.album}")
                )
                .setStyle(BigTextStyle().bigText(
                    "${it.lyricResult.source} (${it.lyricResult.dataOrigin.getCapitalizedName()})" +
                    "\n\rcurrentLyric:\n\r$curLyric\n\r\n\rdelay: $delay sec"
                ))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setTicker(curLyric)
                .build()
            notificationManager.notify(NOTIFICATION_ID_LRC, notification)
            EventTool.sendLyric(curLyric,
                ExtraData(
                    customIcon = false,
                    "",
                    false,
                    BuildConfig.APPLICATION_ID,
                    delay = delay,
                )
            )
            CSLyricHelper.updateLyricAsUser(
                context,
                playInfo,
                CSLyricHelper.LyricData(curLyric),
                user
            )
            lastLyricLine[controller.packageName] = curLyric
            lastSentenceMap[controller.packageName] = sentence
        }
    }

    private fun Lyric.calcDelay(controller: MediaController, position: Long): Int {
        val nextFoundIndex = LyricUtils.getSentenceIndex(this.sentenceList, position, 0, this.offset) + 1
        if (nextFoundIndex >= this.sentenceList.size) return 1
        var delay: Int = ((this.sentenceList[nextFoundIndex].fromTime - position) / 1000).toInt()
        currentLyric[controller.packageName]!!.translatedSentenceList?.let {
            delay /= 2
        }
        delay -= 3
        return delay.coerceAtLeast(1)
    }

    private fun MediaMetadata.toMediaInfo(): MediaInfo {
        return MediaInfo(this)
    }

    private fun insertZeroWidthSpace(input: String): String {
        if (input.isEmpty() || input.length < 2) {
            return input
        }
        val position = 1 + Random().nextInt(input.length - 1)
        val modifiedString = StringBuilder(input)
        modifiedString.insert(position, '\u200B')
        return modifiedString.toString()
    }

    fun init(initContext: Context) {
        context = initContext
        config = Config()
        if (config.targetPackages.isBlank()) return
        EventTool.setContext(context, user)
        DatabaseHelper.init(context)
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        mediaSessionManager?.addOnActiveSessionsChangedListener(
            activeSessionsListener,
            ComponentName(context, NotificationListenerService::class.java)
        )
        val initialControllers = mediaSessionManager?.getActiveSessions(
            ComponentName(context, NotificationListenerService::class.java)
        )
        activeSessionsListener.onActiveSessionsChanged(initialControllers)
        notificationManager = context.getSystemService(NotificationManager::class.java)
        val intent = Intent()
        intent.setClassName(BuildConfig.APPLICATION_ID, LyricsActivity::class.java.name)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun getLastBroadcastIntent(): Intent? {
        return lastBroadcastIntent
    }

    fun updateLyrics(packageName: String) {
        if (curLrcUpdateThread[packageName] == null || !curLrcUpdateThread[packageName]!!.isAlive) {
            currentLyric.remove(packageName)
            EventTool.cleanLyric()
            curLrcUpdateThread[packageName] = LrcUpdateThread(
                context,
                handler,
                lastMetadata[packageName],
                packageName
            )
            curLrcUpdateThread[packageName]!!.start()
        }
    }
}

