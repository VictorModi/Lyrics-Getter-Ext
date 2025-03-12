package statusbar.finder.app.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import statusbar.finder.app.event.LyricSentenceUpdate
import statusbar.finder.app.event.LyricsChange
import statusbar.finder.misc.Constants.BROADCAST_LYRICS_CHANGED
import statusbar.finder.misc.Constants.BROADCAST_LYRIC_SENTENCE_UPDATE

/**
 * LyricGetterExt - statusbar.finder.app
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/1 11:53
 */
class LyricsBroadcastReceiver : BroadcastReceiver() {
    private val gson = Gson()

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val json = it.getStringExtra("data")
            json?.let {
                Log.d("LyricsBroadcastReceiver", json)
                when (intent.action) {
                    BROADCAST_LYRICS_CHANGED -> {
                        val data = gson.fromJson(json, LyricsChange.Data::class.java)
                        LyricsChange.getInstance().notifyResult(data)
                    }
                    BROADCAST_LYRIC_SENTENCE_UPDATE -> {
                        val data = gson.fromJson(json, LyricSentenceUpdate.Data::class.java)
                        LyricSentenceUpdate.getInstance().notifyLyrics(data)
                    }
                }
            }
        }
    }
}
