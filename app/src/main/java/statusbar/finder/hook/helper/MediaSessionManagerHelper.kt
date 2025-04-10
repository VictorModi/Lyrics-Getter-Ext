package statusbar.finder.hook.helper

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.BigTextStyle
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.XModuleResources
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import androidx.core.content.res.ResourcesCompat
import cn.zhaiyifan.lyric.LyricUtils
import cn.zhaiyifan.lyric.model.Lyric
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.google.gson.Gson
import com.hchen.superlyricapi.SuperLyricData
import com.hchen.superlyricapi.SuperLyricPush
import com.hchen.superlyricapi.SuperLyricTool.drawableToBase64
import org.apache.commons.lang3.tuple.MutablePair
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
import statusbar.finder.data.repository.ResRepository
import statusbar.finder.misc.Constants.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt


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
    private val controllerInfo: MutableMap<String, ControllerInfo> = mutableMapOf()
    private var activeControllers: ConcurrentHashMap<MediaController, MediaController.Callback> = ConcurrentHashMap()
    private var playInfo: PlayInfo = PlayInfo("", BuildConfig.APPLICATION_ID)
    private val user: UserHandle = UserHandle.getUserHandleForUid(android.os.Process.myUid())
    private lateinit var notificationManager: NotificationManager
    private val noticeChannelId = "${BuildConfig.APPLICATION_ID.replace(".", "_")}_info"
    private lateinit var pendingIntent: PendingIntent
    private val gson = Gson()
    private var icon: String? = null
    private var lastBroadcastIntent: MutablePair<Intent, Intent?>? = null


    private var activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        controllers?.let {
            if (controllers.isEmpty()) return@OnActiveSessionsChangedListener
            activeControllers.forEach { it.key.unregisterCallback(it.value) }
            activeControllers.clear()
            controllerInfo.clear()
            val targetPackages = config.targetPackages.split(";")
            for (controller in controllers) {
                if (targetPackages.contains(controller.packageName)) {
                    controllerInfo[controller.packageName] = ControllerInfo()
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
            if (msg.what == MSG_LYRIC_UPDATE_DONE) {
                val lyric = msg.obj as? Lyric
                val lyricsChangeData: LyricsChange.Data = lyric?.let {
                    val packageName = it.packageName
                    controllerInfo[packageName]?.let { controllerInfo ->
                        controllerInfo.currentLyric = it
                        controllerInfo.lastSentenceIndex = null
                    }
                    LyricsChange.Data(it, ResRepository.getProvidersMapByOriginId(it.lyricResult.originId))
                } ?: LyricsChange.Data(null, null)
                val intent = Intent(BROADCAST_LYRICS_CHANGED).apply {
                    setPackage(BuildConfig.APPLICATION_ID)
                    putExtra("data", gson.toJson(lyricsChangeData))
                }
                context.sendBroadcastAsUser(intent, user)
                LyricsChange.getInstance().notifyResult(lyricsChangeData)
                lastBroadcastIntent = MutablePair(intent, null)
            }
        }
    }

    private class MediaControllerCallback(private val controller: MediaController) :
        MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                val info = it.toMediaInfo()
                if (it == controllerInfo[controller.packageName]?.lastMetadata) return
                controllerInfo[controller.packageName]?.lastMetadata = it
                notificationManager = context.getSystemService(NotificationManager::class.java)
                val notification = Notification.Builder(context, noticeChannelId)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(info.title)
                    .setContentText(
                        info.artist + (if (info.album.isBlank()) "" else " - ${info.album}")
                    )
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
                notificationManager.notify(NOTIFICATION_ID_LRC, notification)
                controllerInfo[controller.packageName]?.requiredLrcTitle = info.title
                updateLyrics(controller.packageName)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            state?.let { playState ->
                controllerInfo[controller.packageName]?.let {
                    if (it.state == playState) return
                    it.state = playState
                    it.lastSentenceIndex = null
                    if (state.state == PlaybackState.STATE_PLAYING) {
                        it.lastMetadata?.let { metadata ->
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
                        SuperLyricPush.onStop(
                            SuperLyricData()
                                .setPackageName(BuildConfig.APPLICATION_ID)
                                .setPlaybackState(it.state)
                        )
                        CSLyricHelper.pauseAsUser(context, playInfo, user)
                        notificationManager.cancel(NOTIFICATION_ID_LRC)
                    }
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
                    SuperLyricPush.onStop(
                        SuperLyricData()
                            .setPackageName(BuildConfig.APPLICATION_ID)
                            .setPlaybackState(playbackState)
                    )
                    return
                }
                updateLyric(controller, playbackState.position, context)
            }
            handler.postDelayed(this, 100)
        }
    }

    fun updateLyric(controller: MediaController, position: Long, context: Context) {
        controllerInfo[controller.packageName]?.let {
            it.currentLyric?.let { lyric ->
                val sentenceIndex = LyricUtils.getSentenceIndex(lyric.sentenceList, position, 0, lyric.offset)
                if (sentenceIndex < 0) return
                val sentence = lyric.sentenceList[sentenceIndex]
                val translatedSentence = LyricUtils.getSentence(lyric.translatedSentenceList, position, 0, lyric.offset)
                if (sentence.content.isBlank()) return
                if (sentenceIndex == it.lastSentenceIndex) return
                val delay: Int = lyric.calcDelay(position)
                val sentenceContent = sentence.content.trim()
                val translatedContent = translatedSentence?.content?.trim() ?: ""

                var curLyric = when (config.translateDisplayType) {
                    "translated" -> translatedContent.ifBlank { sentenceContent }
                    "both" -> if (translatedContent.isBlank()) sentenceContent else "$sentenceContent\n\r$translatedContent"
                    else -> sentenceContent
                }
                if (config.forceRepeat &&
                    it.lastLyricLineContent == curLyric) {
                    curLyric = insertZeroWidthSpace(curLyric)
                }
                playInfo.isPlaying = true
                val data = LyricSentenceUpdate.Data(
                    sentence.content,
                    translatedSentence?.content,
                    sentenceIndex,
                    delay
                )
                val intent = Intent(BROADCAST_LYRIC_SENTENCE_UPDATE).apply {
                    setPackage(BuildConfig.APPLICATION_ID)
                    putExtra("data", gson.toJson(data))
                }
                context.sendBroadcastAsUser(intent, user)
                lastBroadcastIntent?.right = intent
                LyricSentenceUpdate.getInstance().notifyLyrics(data)
                val notification = Notification.Builder(MediaSessionManagerHelper.context, noticeChannelId)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(
                        "${lyric.originalMediaInfo.title} - ${lyric.originalMediaInfo.artist}" +
                                (if (lyric.originalMediaInfo.album.isBlank()) "" else " - ${lyric.originalMediaInfo.album}")
                    )
                    .setContentText(
                        "${lyric.title} - ${lyric.artist}" +
                                (if (lyric.album.isBlank()) "" else " - ${lyric.album}")
                    )
                    .setStyle(BigTextStyle().bigText(
                        "${lyric.lyricResult.source} (${lyric.lyricResult.dataOrigin.getCapitalizedName()})" +
                                "\n\rcurrentLyric:\n\r$curLyric\n\r\n\rdelay: $delay sec"
                    ))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setTicker(curLyric)
                    .build()
                notificationManager.notify(NOTIFICATION_ID_LRC, notification)
                SuperLyricPush.onSuperLyric(
                    SuperLyricData()
                        .setLyric(curLyric)
                        .setDelay(delay)
                        .setMediaMetadata(it.lastMetadata)
                        .setPlaybackState(it.state)
                        .setBase64Icon(icon ?: "")
                        .setPackageName(BuildConfig.APPLICATION_ID)
                )
                CSLyricHelper.updateLyricAsUser(
                    context,
                    playInfo,
                    CSLyricHelper.LyricData(curLyric),
                    user
                )
                it.lastLyricLineContent = curLyric
                it.lastSentenceIndex = sentenceIndex
            }
        }
    }

    private fun Lyric.calcDelay(position: Long): Int {
        val nextSentenceIndex = LyricUtils.getSentenceIndex(this.sentenceList, position, 0, this.offset) + 1
        if (nextSentenceIndex >= this.sentenceList.size) return 1
        var delay: Int = ((this.sentenceList[nextSentenceIndex].fromTime - position) / 1000.0).roundToInt()
        this.translatedSentenceList?.let {
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

    @SuppressLint("DiscouragedApi")
    fun init(initContext: Context) {
        context = initContext
        config = Config()
        if (config.targetPackages.isBlank()) return
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
        XModuleResources.createInstance(EzXHelper.modulePath, null)?.let { xmr ->
            val resId = xmr.getIdentifier("ic_statusbar_icon", "drawable", BuildConfig.APPLICATION_ID)
            val drawable =  ResourcesCompat.getDrawable(xmr, resId, null)
            if (drawable != null) {
                icon = drawableToBase64(drawable)
            }
        }
    }

    fun getLastBroadcastIntent(): MutablePair<Intent, Intent?>? {
        return lastBroadcastIntent
    }

    fun updateLyrics(packageName: String) {
        controllerInfo[packageName]?.let {
            synchronized(it) {
                if (it.curLrcUpdateThread?.isAlive == true) return
                it.currentLyric = null
                SuperLyricPush.onStop(
                    SuperLyricData()
                        .setPackageName(BuildConfig.APPLICATION_ID)
                        .setPlaybackState(it.state)
                )
                context.sendBroadcastAsUser(
                    Intent(BROADCAST_LYRICS_CHANGED).apply {
                        putExtra("data", gson.toJson(LyricsChange.Data(null, null)))
                        setPackage(BuildConfig.APPLICATION_ID)
                    },
                    user
                )
                it.lastMetadata?.let { metadata ->
                    it.curLrcUpdateThread = LrcUpdateThread(
                        context,
                        handler,
                        metadata,
                        packageName
                    ).apply { start() }
                }
            }
        }
    }


    data class ControllerInfo(
        var lastSentenceIndex: Int? = null,
        var requiredLrcTitle: String? = null,
        var curLrcUpdateThread: LrcUpdateThread? = null,
        var currentLyric: Lyric? = null,
        var lastMetadata: MediaMetadata? = null,
        var lastLyricLineContent: String? = null,
        var state: PlaybackState? = null,
    )
}

