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
import statusbar.finder.R
import statusbar.finder.app.event.LyricSentenceUpdate
import statusbar.finder.app.event.LyricsChange
import statusbar.finder.data.model.LyricItem
import statusbar.finder.data.repository.ActiveRepository
import statusbar.finder.data.repository.LyricRepository.deleteResByOriginIdAndDeleteActive
import statusbar.finder.data.repository.ResRepository
import statusbar.finder.hook.tool.Tool
import statusbar.finder.misc.Constants.*

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
    private lateinit var provideSpinner: Spinner
    private val lyricsList = mutableListOf<LyricItem>()
    private var currentHighlightPos = -1
    private var currentLyricResId: Long = -1L
    private var currentLyric: Lyric? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lrcview)
        recyclerView = findViewById(R.id.rvLyrics)
        etOffset = findViewById(R.id.etOffset)
        provideSpinner = findViewById(R.id.provideSpinner)
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
            currentLyric?.let {
                val newOffset: Long
                try {
                    newOffset = etOffset.getText().toString().toLong()
                } catch (e: NumberFormatException) {
                    Toast.makeText(applicationContext, "Offset not valid", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (Tool.xpActivation) {
                    val intent = Intent(BROADCAST_LYRICS_OFFSET_UPDATE_REQUEST)
                    intent.putExtra("offset", newOffset)
                    intent.putExtra("resId", currentLyricResId)
                    intent.putExtra("packageName", it.packageName)
                    applicationContext.sendBroadcast(intent)
                } else {
                    ResRepository.updateResOffsetById(currentLyricResId, newOffset)
                    MusicListenerService.instance.startSearch()
                }
            } ?: run {
                Toast.makeText(applicationContext, "No Lyrics Found", Toast.LENGTH_SHORT).show()
            }
        }

        val retryBtn = findViewById<ImageButton>(R.id.retry)
        retryBtn.setOnClickListener {
            currentLyric?.let {
                val originId = it.lyricResult.originId
                if (Tool.xpActivation) {
                    val intent = Intent(BROADCAST_LYRICS_DELETE_RESULT_REQUEST)
                    intent.putExtra("originId", originId)
                    intent.putExtra("packageName", it.packageName)
                    applicationContext.sendBroadcast(intent)
                } else {
                    deleteResByOriginIdAndDeleteActive(originId)
                    MusicListenerService.instance.startSearch()
                }
            } ?: run {
                Toast.makeText(applicationContext, "No Lyrics Found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerObservers() {
        // 歌词结果变化观察
        LyricsChange.getInstance().observe(this) { resultData ->
            resultData?.let {
                updateLyricList(it)
            }
        }

        // 当前歌词行更新观察
        LyricSentenceUpdate.getInstance().observe(this) { updateData ->
            updateData?.let {
                updateHighlightPosition(it.lyricsIndex)
            }
        }
    }

    private fun updateLyricList(data: LyricsChange.Data) {
        clear()
        currentLyric = data.lyric
        if (data.lyric == null || data.providers == null) return
        val originLines = data.lyric.sentenceList
        val translatedLines = data.lyric.translatedSentenceList
        val spinnerItems: List<String> = data.providers.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        provideSpinner.adapter = adapter
        provideSpinner.setSelection(spinnerItems.indexOf(data.lyric.lyricResult.source))
        provideSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val selectedKey = spinnerItems[position]
                if (selectedKey == data.lyric.lyricResult.source) return
                val selectedValue = data.providers[selectedKey]
                selectedValue?.let {
                    if (Tool.xpActivation) {
                        val intent = Intent(BROADCAST_LYRICS_ACTIVE_UPDATE_REQUEST)
                        intent.putExtra("originId", data.lyric.lyricResult.originId)
                        intent.putExtra("resId", it)
                        intent.putExtra("packageName", data.lyric.packageName)
                        applicationContext.sendBroadcast(intent)
                    } else {
                        ActiveRepository.updateResultIdByOriginId(data.lyric.lyricResult.originId, it)
                        MusicListenerService.instance.startSearch()
                    }
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) { }
        }
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

        updateSongInfo()
        syncOffset(data.lyric)
        currentLyricResId = data.lyric.lyricResult.resId
        adapter.notifyDataSetChanged()
        currentHighlightPos = -1
    }

    private fun updateSongInfo() {
        val tvSongName = findViewById<TextView>(R.id.tvSongName)
        val tvSongArtist = findViewById<TextView>(R.id.tvSongArtist)
        val tvSongAlbum = findViewById<TextView>(R.id.tvSongAlbum)
        val artistSpaceAlbumView = findViewById<Space>(R.id.artistSpaceAlbumView)
        currentLyric?.let { lyric ->
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
        } ?: run {
            tvSongName.text = ""
            tvSongArtist.text = ""
            tvSongAlbum.text = ""
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

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        currentLyric = null
        updateSongInfo()
        lyricsList.clear()
        adapter.notifyDataSetChanged()
        etOffset.text.clear()
    }
}
