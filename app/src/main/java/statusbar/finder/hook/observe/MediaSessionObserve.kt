package statusbar.finder.hook.observe

import android.annotation.SuppressLint
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
import statusbar.finder.data.db.DatabaseHelper
import statusbar.finder.hook.tool.EventTool
import statusbar.finder.misc.Constants.MSG_LYRIC_UPDATE_DONE

/**
 * LyricGetterExt - statusbar.finder.hook.observe
 * @description 参考自 https://github.com/xiaowine/Lyric-Getter/blob/master/app/src/main/kotlin/cn/lyric/getter/observe/MediaSessionObserve.kt
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/17 02:41
 */
@SuppressLint("StaticFieldLeak")
object MediaSessionObserve {
    private lateinit var context: Context
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var requiredLrcTitle: String = ""
    private var curLrcUpdateThread: LrcUpdateThread? = null
    private var currentLyric: Lyric? = null
    private var lastSentenceFromTime: Long = -1
    private var playInfo: PlayInfo = PlayInfo("", BuildConfig.APPLICATION_ID)

    private var activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        controllers?.let {
            currentLyric = null
            EventTool.cleanLyric()
            if (controllers.isEmpty()) return@OnActiveSessionsChangedListener
            activeController?.unregisterCallback(mediaControllerCallback)
            controllers.firstOrNull()?.registerCallback(mediaControllerCallback)
            activeController = controllers.firstOrNull()
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

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                currentLyric = null
                EventTool.cleanLyric()
                requiredLrcTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)

                activeController?.let {
                    if (curLrcUpdateThread == null || !curLrcUpdateThread!!.isAlive) {
                        curLrcUpdateThread = LrcUpdateThread(
                            context,
                            handler,
                            metadata,
                            activeController!!.packageName
                        )
                        curLrcUpdateThread!!.start()
                    }
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
                    CSLyricHelper.pause(context, playInfo)
                }
            }
        }
    }

    private val lyricUpdateRunnable: Runnable = object : Runnable {
        override fun run() {
            if (activeController?.playbackState == null || activeController!!.playbackState!!.state != PlaybackState.STATE_PLAYING) {
                playInfo.isPlaying = false
                CSLyricHelper.pauseAsUser(context, playInfo, UserHandle.getUserHandleForUid(android.os.Process.myUid()))
                EventTool.cleanLyric()
                return
            }
            updateLyric(activeController!!.playbackState!!.position, context)
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
                UserHandle.getUserHandleForUid(android.os.Process.myUid())
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
        EventTool.setContext(context)
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
    }
}

