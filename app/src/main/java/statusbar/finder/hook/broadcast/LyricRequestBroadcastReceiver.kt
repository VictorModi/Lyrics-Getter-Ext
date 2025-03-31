package statusbar.finder.hook.broadcast

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import statusbar.finder.data.repository.ActiveRepository
import statusbar.finder.data.repository.AliasRepository
import statusbar.finder.data.repository.LyricRepository.deleteResByOriginIdAndDeleteActive
import statusbar.finder.data.repository.ResRepository
import statusbar.finder.hook.helper.MediaSessionManagerHelper.getLastBroadcastIntent
import statusbar.finder.hook.helper.MediaSessionManagerHelper.updateLyrics
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
                    context.sendBroadcastAsUser(it.left, user)
                    it.right?.let { lineIntent ->
                        context.sendBroadcastAsUser(lineIntent, user)
                    }
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
            BROADCAST_LYRICS_DELETE_RESULT_REQUEST -> {
                val originId = intent.getLongExtra("originId", -1L)
                val packageName = intent.getStringExtra("packageName")
                packageName?.let {
                    if (originId != -1L) {
                        deleteResByOriginIdAndDeleteActive(originId)
                        updateLyrics(packageName)
                    }
                }
            }
            BROADCAST_LYRICS_UPDATE_ALIAS_REQUEST -> {
                val originId = intent.getLongExtra("originId", -1L)
                val packageName = intent.getStringExtra("packageName")
                val newTitle = intent.getStringExtra("newTitle")
                val newArtist = intent.getStringExtra("newArtist")
                val newAlbum = intent.getStringExtra("newAlbum")

                packageName?.let {
                    AliasRepository.updateAlias(originId, newTitle, newArtist, newAlbum)
                }
            }
        }
    }
}
