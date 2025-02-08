package statusbar.finder

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.zhaiyifan.lyric.LyricUtils
import cn.zhaiyifan.lyric.model.Lyric
import com.github.kyuubiran.ezxhelper.Log
import statusbar.finder.livedata.LyricSentenceUpdate
import statusbar.finder.livedata.LyricsChange
import statusbar.finder.livedata.LyricsResultChange
import statusbar.finder.provider.ILrcProvider

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
    private val lyricsList = mutableListOf<LyricItem>()
    private var currentHighlightPos = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lrcview)  // 确保这个布局文件正确且包含 RecyclerView

        // 在 setContentView 后初始化 recyclerView
        recyclerView = findViewById(R.id.rvLyrics)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = LyricsAdapter(lyricsList)
        recyclerView.adapter = adapter

        // 注册LiveData观察者
        registerObservers()

        // 更新歌词列表
        updateLyricList(MusicListenerService.instance.lyric)
    }

    private fun registerObservers() {
        // 歌词结果变化观察
        LyricsChange.getInstance().observe(this) { resultData ->
            resultData?.lyric?.let { lyric ->
                updateLyricList(lyric)
            }
        }

        // 当前歌词行更新观察
        LyricSentenceUpdate.getInstance().observe(this) { updateData ->
            updateData?.let {
                it.lyricsIndex?.let { pos -> updateHighlightPosition(pos) }
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
            // 查找当前 origin 对应的 translated
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

        adapter.notifyDataSetChanged()
        currentHighlightPos = -1
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
