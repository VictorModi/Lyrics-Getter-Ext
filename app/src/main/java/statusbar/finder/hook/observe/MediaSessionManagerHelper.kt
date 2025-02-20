package statusbar.finder.hook.observe

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.BigTextStyle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
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
import com.github.kyuubiran.ezxhelper.Log
import statusbar.finder.BuildConfig
import statusbar.finder.CSLyricHelper
import statusbar.finder.CSLyricHelper.PlayInfo
import statusbar.finder.app.MusicListenerService.LrcUpdateThread
import statusbar.finder.config.Config
import statusbar.finder.data.db.DatabaseHelper
import statusbar.finder.hook.tool.EventTool
import statusbar.finder.misc.Constants.MSG_LYRIC_UPDATE_DONE
import statusbar.finder.misc.Constants.NOTIFICATION_ID_LRC
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
    private var playInfo: PlayInfo = PlayInfo("", BuildConfig.APPLICATION_ID)
    private val user: UserHandle = UserHandle.getUserHandleForUid(android.os.Process.myUid())
    private var notificationManager: NotificationManager? = null;
    private val noticeChannelId = "${BuildConfig.APPLICATION_ID.replace(".", "_")}_info"


    private var activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.i("${BuildConfig.APPLICATION_ID} Active sessions changed: ${controllers?.size ?: 0}");
        controllers?.let {
            if (controllers.isEmpty()) return@OnActiveSessionsChangedListener
            activeControllers.forEach { it.key.unregisterCallback(it.value) }
            activeControllers.clear()
            currentLyric.clear()
            lastMetadata.clear()
            lastSentenceMap.clear()
            val targetPackages = config.targetPackages.split(";")
            for (controller in controllers) {
                Log.i("${BuildConfig.APPLICATION_ID} Checking controller: ${controller.packageName}");
                if (targetPackages.contains(controller.packageName)) {
                    lastMetadata.remove(controller.packageName)
                    val callback = MediaControllerCallback(controller)
                    activeControllers[controller] = callback
                    controller.registerCallback(callback)
                    controller.metadata?.let {
                        callback.onMetadataChanged(controller.metadata)
                        callback.onPlaybackStateChanged(controller.playbackState)
                    }
                    Log.i("${BuildConfig.APPLICATION_ID} Registered callback for: ${controller.packageName}");
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
                lyric?.let {
                    notificationManager = context.getSystemService(NotificationManager::class.java)
                    val notification = Notification.Builder(context, noticeChannelId)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentTitle(lyric.originalMediaInfo.title)
                        .setContentText(
                            "${lyric.originalMediaInfo.artist} - ${lyric.originalMediaInfo.album}"
                        )
                        .setStyle(BigTextStyle().bigText(
                                "${lyric.title}\n\r" +
                                "${lyric.artist} - ${lyric.album}\n\r" +
                                "From ${lyric.lyricResult.source} (${lyric.lyricResult.dataOrigin.getCapitalizedName()})"
                        ))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .build()
                    notificationManager!!.notify(NOTIFICATION_ID_LRC, notification)
                } ?: Log.i("${BuildConfig.APPLICATION_ID} Failed find Lyric for controller: ${packageName}")
            }
        }
    }

    private class MediaControllerCallback(private val controller: MediaController) :
        MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                if (it == lastMetadata[controller.packageName]) {
                    Log.i("${BuildConfig.APPLICATION_ID} same metadata, IGNORE")
                    return
                }
                notificationManager = context.getSystemService(NotificationManager::class.java)
                val notification = Notification.Builder(context, noticeChannelId)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
                    .setContentText(
                        "${metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)} - ${metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)}"
                    )
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
                notificationManager!!.notify(NOTIFICATION_ID_LRC, notification)
                requiredLrcTitle[controller.packageName] = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                Log.i("${BuildConfig.APPLICATION_ID} Metadata changed: ${metadata.getString(MediaMetadata.METADATA_KEY_TITLE)}");
                if (curLrcUpdateThread[controller.packageName] == null || !curLrcUpdateThread[controller.packageName]!!.isAlive) {
                    currentLyric.remove(controller.packageName)
                    EventTool.cleanLyric()
                    curLrcUpdateThread[controller.packageName] = LrcUpdateThread(
                        context,
                        handler,
                        metadata,
                        controller.packageName
                    )
                    curLrcUpdateThread[controller.packageName]!!.start()
                    Log.i("${BuildConfig.APPLICATION_ID} Lrc Update thread started")
                    lastMetadata[controller.packageName] = metadata
                }
            } ?: Log.i("${BuildConfig.APPLICATION_ID} metadata is Null, IGNORE")
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            state?.let {
                if (lastState[controller.packageName] == it) return
                lastState[controller.packageName] = it
                Log.i("${BuildConfig.APPLICATION_ID} Playback state changed: ${state.state}")
                lastSentenceMap.remove(controller.packageName)
                if (state.state == PlaybackState.STATE_PLAYING) {
                    handler.post(lyricUpdateRunnable)
                } else {
                    handler.removeCallbacks(lyricUpdateRunnable)
                    EventTool.cleanLyric()
                    CSLyricHelper.pauseAsUser(context, playInfo, user)
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
//                Log.i("${BuildConfig.APPLICATION_ID} Updating lyrics for: ${controller.packageName}");
                updateLyric(controller, playbackState.position, context)
            }
            handler.postDelayed(this, 250)
        }
    }

    fun updateLyric(controller: MediaController, position: Long, context: Context) {
        currentLyric[controller.packageName]?.let {
            val sentence = LyricUtils.getSentence(it.sentenceList, position, 0, it.offset) ?: return
            val translatedSentence = LyricUtils.getSentence(it.translatedSentenceList, position, 0, it.offset)
            if (sentence.content.isBlank()) return
            if (sentence == lastSentenceMap[controller.packageName]) return
            val delay: Int = it.calcDelay(controller, position)
            val sentenceContent = sentence.content.trim()
            val translatedContent = translatedSentence?.content?.trim() ?: ""
            Log.i("${BuildConfig.APPLICATION_ID} current TranslateDisplayType: ${config.translateDisplayType}")
            var curLyric = when (config.translateDisplayType) {
                "translated" -> translatedContent.ifBlank { sentenceContent }
                "both" -> if (translatedContent.isBlank()) sentenceContent else "$sentenceContent\n\r$translatedContent"
                else -> sentenceContent
            }
            if (config.forceRepeat && lastSentenceMap[controller.packageName]?.content == sentence.content) {
                curLyric = insertZeroWidthSpace(curLyric)
            }
            playInfo.isPlaying = true
            Log.i("${BuildConfig.APPLICATION_ID} Updating Lyric: $curLyric, delay: $delay")
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
            lastSentenceMap[controller.packageName] = sentence
        } ?: Log.i("${BuildConfig.APPLICATION_ID} currentLyric[controller] is null, IGNORE")
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

    private fun insertZeroWidthSpace(input: String): String {
        if (input.isEmpty() || input.length < 2) {
            return input
        }
        val random = Random()
        val position = 1 + random.nextInt(input.length - 1)
        val modifiedString = StringBuilder(input)
        modifiedString.insert(position, '\u200B')
        return modifiedString.toString()
    }

    fun init(initContext: Context) {
        context = initContext
        config = Config()
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
        val channel = NotificationChannel(
            noticeChannelId,
            "Lyrics Getter Ext",
            NotificationManager.IMPORTANCE_MIN
        )
        notificationManager!!.createNotificationChannel(channel)
        val notification = Notification.Builder(context, noticeChannelId)
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentTitle("Lyrics Getter Ext")
            .setContentText("已成功加载")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
        notificationManager!!.notify(NOTIFICATION_ID_LRC, notification)

        Log.i("${BuildConfig.APPLICATION_ID} config.translateDisplayType: '${config.translateDisplayType}'")
        Log.i("${BuildConfig.APPLICATION_ID} config.forceRepeat: '${config.forceRepeat}'")
        Log.i("${BuildConfig.APPLICATION_ID} config.targetPackages: '${config.targetPackages}'")
    }
}

