package statusbar.finder.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.zhaiyifan.lyric.model.Lyric
import statusbar.finder.data.model.LyricItem
import statusbar.finder.R
import statusbar.finder.app.event.LyricSentenceUpdate
import statusbar.finder.app.event.LyricsChange
import statusbar.finder.data.repository.LyricRepository
import statusbar.finder.data.repository.ResRepository
import statusbar.finder.hook.tool.Tool
import statusbar.finder.misc.Constants.BROADCAST_LYRICS_CHANGED_REQUEST
import statusbar.finder.misc.Constants.BROADCAST_LYRICS_OFFSET_UPDATE_REQUEST

/**
 * LyricGetterExt - statusbar.finder
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/8 14:23
 */
class LyricsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LyricsAdapter
    private lateinit var etOffset: EditText
    private val lyricsList = mutableListOf<LyricItem>()
    private var currentHighlightPos = -1
    private var currentLyricResId: Long = -1L
    private var currentLyric: Lyric? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lrcview)  // 确保这个布局文件正确且包含 RecyclerView

        // 在 setContentView 后初始化 recyclerView
        recyclerView = findViewById(R.id.rvLyrics)
        etOffset = findViewById(R.id.etOffset)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = LyricsAdapter(lyricsList)
        recyclerView.adapter = adapter

        registerObservers()
        if (Tool.xpActivation) {
            val intent = Intent(BROADCAST_LYRICS_CHANGED_REQUEST)
            applicationContext.sendBroadcast(intent)
        } else {
            MusicListenerService.instance?.lyric?.let {
                currentLyric = it
            }
        }

        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            if (currentLyricResId != -1L && currentLyric != null) {
                var newOffset = 0L
                try {
                    newOffset = etOffset.getText().toString().toLong()
                } catch (e: NumberFormatException) {
                    Toast.makeText(applicationContext, "Offset not valid", Toast.LENGTH_SHORT).show()
                }
                if (Tool.xpActivation) {
                    val intent = Intent(BROADCAST_LYRICS_OFFSET_UPDATE_REQUEST)
                    intent.putExtra("offset", newOffset)
                    intent.putExtra("resId", currentLyricResId)
                    intent.putExtra("packageName", currentLyric!!.packageName)
                    applicationContext.sendBroadcast(intent)
                } else {
                    ResRepository.updateResOffsetById(currentLyricResId, newOffset)
                    currentLyric?.offset = newOffset
                    LyricsChange.getInstance().notifyResult(LyricsChange.Data(currentLyric))
                }
                Toast.makeText(applicationContext, "Updated Offset Successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "No Lyrics Found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerObservers() {
        // 歌词结果变化观察
        LyricsChange.getInstance().observe(this) { resultData ->
            resultData?.lyric?.let { lyric ->
                currentLyric = lyric
                updateLyricList(lyric)
            }
        }

        // 当前歌词行更新观察
        LyricSentenceUpdate.getInstance().observe(this) { updateData ->
            updateData?.let {
                updateHighlightPosition(it.lyricsIndex)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateLyricList(lyric: Lyric) {
        lyricsList.clear()
        val originLines = lyric.sentenceList
        val translatedLines = lyric.translatedSentenceList

        // 用来处理配对后的数据
        var translatedIndex = 0

        originLines.forEachIndexed { _, origin ->
            var matchedTranslation: String? = null
            while (translatedIndex < translatedLines.size && translatedLines[translatedIndex].fromTime <= origin.fromTime) {
                matchedTranslation = translatedLines[translatedIndex].content
                translatedIndex++
            }
            lyricsList.add(
                LyricItem(
                    origin = origin.content,
                    translation = matchedTranslation
                )
            )
        }

        updateSongInfo(lyric)
        syncOffset(lyric)
        currentLyricResId = lyric.lyricResult.resId
        adapter.notifyDataSetChanged()
        currentHighlightPos = -1
    }

    private fun updateSongInfo(lyric: Lyric) {
        val tvSongName = findViewById<TextView>(R.id.tvSongName)
        val tvSongArtist = findViewById<TextView>(R.id.tvSongArtist)
        val tvSongAlbum = findViewById<TextView>(R.id.tvSongAlbum)
        val artistSpaceAlbumView = findViewById<Space>(R.id.artistSpaceAlbumView)

        tvSongName.text = lyric.title
        tvSongArtist.text = lyric.artist
        lyric.album?.let {
            tvSongAlbum.text = it
            artistSpaceAlbumView.visibility = View.VISIBLE
            tvSongAlbum.visibility = View.VISIBLE
        } ?: run {
            artistSpaceAlbumView.visibility = View.GONE
            tvSongAlbum.visibility = View.GONE
        }
    }

    private fun syncOffset(lyric: Lyric) {
        etOffset.setText(lyric.offset.toString())
    }

    private fun updateHighlightPosition(newPosition: Int) {
        if (newPosition == currentHighlightPos) return
        if (newPosition !in 0 until lyricsList.size) return

        // 更新前一个高亮行
        lyricsList.getOrNull(currentHighlightPos)?.let {
            it.isHighlight = false
            adapter.notifyItemChanged(currentHighlightPos)
        }

        // 设置新高亮行
        lyricsList[newPosition].isHighlight = true
        adapter.notifyItemChanged(newPosition)
        currentHighlightPos = newPosition

        // 自动滚动到居中位置
        smoothScrollToCenter(newPosition)
    }

    private fun smoothScrollToCenter(position: Int) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        when {
            position < firstVisible -> recyclerView.smoothScrollToPosition(position)
            position > lastVisible -> recyclerView.smoothScrollToPosition(position)
            else -> {
                // 精确居中滚动
                val targetView = layoutManager.findViewByPosition(position)
                targetView?.let {
                    val center = recyclerView.height / 2
                    val offset = it.top - center
                    recyclerView.smoothScrollBy(0, offset)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView.clearOnScrollListeners()
    }
}
