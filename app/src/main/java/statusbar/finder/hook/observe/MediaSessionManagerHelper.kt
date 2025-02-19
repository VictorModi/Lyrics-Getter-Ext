package statusbar.finder.hook.observe

import android.annotation.SuppressLint
import android.app.Notification
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
    private lateinit var activeControllers: MutableMap<MediaController, MediaController.Callback>
    private var requiredLrcTitle: String = ""
    private var curLrcUpdateThread: LrcUpdateThread? = null
    private var currentLyric: Lyric? = null
    private var lastSentenceFromTime: Long = -1
    private var playInfo: PlayInfo = PlayInfo("", BuildConfig.APPLICATION_ID)
    private val user: UserHandle = UserHandle.getUserHandleForUid(android.os.Process.myUid())
    private var notificationManager: NotificationManager? = null;
    private val noticeChannelId = "${BuildConfig.APPLICATION_ID.replace(".", "_")}_info"


    private var activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        controllers?.let {
            currentLyric = null
            EventTool.cleanLyric()
            if (controllers.isEmpty()) return@OnActiveSessionsChangedListener
            activeControllers.forEach { it.key.unregisterCallback(it.value) }
            val targetPackages = config.targetPackages.split(";")
            for (controller in controllers) {
                if (targetPackages.contains(controller.packageName)) {
                    val callback = MediaControllerCallback(controller)
                    activeControllers[controller] = callback
                    controller.registerCallback(callback)
                }
            }
        }
    }

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == MSG_LYRIC_UPDATE_DONE && msg.data.getString("title", "") == requiredLrcTitle) {
                currentLyric = msg.obj as Lyric?
            }
        }
    }

    private class MediaControllerCallback(private val controller: MediaController) :
        MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                currentLyric = null
                EventTool.cleanLyric()
                requiredLrcTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                Log.i("${BuildConfig.APPLICATION_ID} MediaMetadata Change, New Title: $requiredLrcTitle")
                if (curLrcUpdateThread == null || !curLrcUpdateThread!!.isAlive) {
                    curLrcUpdateThread = LrcUpdateThread(
                        context,
                        handler,
                        metadata,
                        controller.packageName
                    )
                    curLrcUpdateThread!!.start()
                }
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            state?.let {
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
            for (controller in activeControllers) {
                if (controller.key.playbackState == null || controller.key.playbackState!!.state != PlaybackState.STATE_PLAYING) {
                    playInfo.isPlaying = false
                    CSLyricHelper.pauseAsUser(context, playInfo, user)
                    EventTool.cleanLyric()
                    return
                }
                updateLyric(controller.key.playbackState!!.position, context)
            }
            handler.postDelayed(this, 250)
        }
    }

    fun updateLyric(position: Long, context: Context) {
        currentLyric?.let {
            val sentence = LyricUtils.getSentence(it.sentenceList, position, 0, it.offset) ?: return
            val delay: Int = it.calcDelay(position)
            if (sentence.fromTime == lastSentenceFromTime) return
            val curLyric = sentence.content.trim()
            lastSentenceFromTime = sentence.fromTime
            playInfo.isPlaying = true
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
        }
    }

    private fun Lyric.calcDelay(position: Long): Int {
        val nextFoundIndex = LyricUtils.getSentenceIndex(this.sentenceList, position, 0, this.offset) + 1
        if (nextFoundIndex >= this.sentenceList.size) return 1
        val delay: Int = ((this.sentenceList[nextFoundIndex].fromTime - position) / 1000).toInt()
        return delay
    }

    fun initByContext(initContext: Context) {
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
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager!!.createNotificationChannel(channel)
        val notification = Notification.Builder(context, noticeChannelId)
            .setSmallIcon(android.R.drawable.ic_media_pause) // 确保 systemUiContext 能识别的图标
            .setContentTitle("Lyrics Getter Ext")
            .setContentText("已成功加载")
            .setOngoing(true)
            .build()
        notificationManager!!.notify(NOTIFICATION_ID_LRC, notification)
        activeControllers = mutableMapOf()
        Log.i("${BuildConfig.APPLICATION_ID} Config forceRepeat: ${config.forceRepeat}")
        Log.i("${BuildConfig.APPLICATION_ID} Config targetPackages: ${config.targetPackages}")
        Log.i("${BuildConfig.APPLICATION_ID} Config translateDisplayType: ${config.translateDisplayType}")
    }
}

