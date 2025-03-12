package statusbar.finder.hook.broadcast

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import statusbar.finder.data.repository.ActiveRepository
import statusbar.finder.data.repository.ResRepository
import statusbar.finder.hook.observe.MediaSessionManagerHelper.getLastBroadcastIntent
import statusbar.finder.hook.observe.MediaSessionManagerHelper.updateLyrics
import statusbar.finder.misc.Constants.*

/**
 * LyricGetterExt - statusbar.finder.hook.broadcast
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/12 13:07
 */
@SuppressLint("MissingPermission")
class LyricRequestBroadcastReceiver : BroadcastReceiver() {
    private val user: UserHandle = UserHandle.getUserHandleForUid(android.os.Process.myUid())

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BROADCAST_LYRICS_CHANGED_REQUEST -> {
                getLastBroadcastIntent()?.let {
                    context.sendBroadcastAsUser(it, user)
                }
            }
            BROADCAST_LYRICS_OFFSET_UPDATE_REQUEST -> {
                val offset = intent.getLongExtra("offset", -1L)
                val resId = intent.getLongExtra("resId", -1L)
                val packageName = intent.getStringExtra("packageName")
                packageName?.let {
                    if (offset != -1L && resId != -1L) {
                        ResRepository.updateResOffsetById(resId, offset)
                        updateLyrics(packageName)
                    }
                }
            }
            BROADCAST_LYRICS_ACTIVE_UPDATE_REQUEST -> {
                val originId = intent.getLongExtra("originId", -1L)
                val resId = intent.getLongExtra("resId", -1L)
                val packageName = intent.getStringExtra("packageName")
                packageName?.let {
                    if (originId != -1L && resId != -1L) {
                        ActiveRepository.updateResultIdByOriginId(originId, resId)
                        updateLyrics(packageName)
                    }
                }
            }
        }
    }
}
