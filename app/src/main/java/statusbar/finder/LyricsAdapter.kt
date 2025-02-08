package statusbar.finder

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView

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
        if (item.isHighlight) {
            holder.tvOrigin.setTextColor(ContextCompat.getColor(context, R.color.design_default_color_on_primary))
            holder.tvOrigin.textSize = 20f
            holder.tvOrigin.typeface = Typeface.DEFAULT_BOLD

            holder.tvTranslation.setTextColor(ContextCompat.getColor(context, R.color.design_default_color_on_primary))
            holder.tvTranslation.textSize = 16f
        } else {
            holder.tvOrigin.setTextColor(ContextCompat.getColor(context, R.color.design_default_color_on_secondary))
            holder.tvOrigin.textSize = 16f
            holder.tvOrigin.typeface = Typeface.DEFAULT

            holder.tvTranslation.setTextColor(ContextCompat.getColor(context, R.color.design_default_color_on_secondary))
            holder.tvTranslation.textSize = 14f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = lyrics.size
}
