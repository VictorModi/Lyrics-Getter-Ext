package statusbar.finder

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

/**
 * LyricGetterExt - statusbar.finder
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/8 11:01
 */
class LyricsAdapter(private val lyrics: List<LyricItem>) :
    RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrigin: TextView = view.findViewById(R.id.tvOrigin)
        val tvTranslation: TextView = view.findViewById(R.id.tvTranslation)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lyrics[position]

        // 设置文本内容
        holder.tvOrigin.text = item.origin
        item.translation?.let {
            holder.tvTranslation.text = it
            holder.tvTranslation.visibility = View.VISIBLE
        } ?: run {
            holder.tvTranslation.visibility = View.GONE
        }

        // 高亮样式
        val context = holder.itemView.context
        val highlightColor = MaterialColors.getColor(context, R.attr.colorSecondary, Color.BLACK)
        val normalColor = MaterialColors.getColor(context, R.attr.colorControlHighlight, Color.GRAY)

        if (item.isHighlight) {
            holder.tvOrigin.setTextColor(highlightColor)
            holder.tvOrigin.textSize = 20f
            holder.tvOrigin.typeface = Typeface.DEFAULT_BOLD

            holder.tvTranslation.setTextColor(highlightColor)
            holder.tvTranslation.textSize = 16f
        } else {
            holder.tvOrigin.setTextColor(normalColor)
            holder.tvOrigin.textSize = 16f
            holder.tvOrigin.typeface = Typeface.DEFAULT

            holder.tvTranslation.setTextColor(normalColor)
            holder.tvTranslation.textSize = 14f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = lyrics.size

    fun isDarkMode(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
