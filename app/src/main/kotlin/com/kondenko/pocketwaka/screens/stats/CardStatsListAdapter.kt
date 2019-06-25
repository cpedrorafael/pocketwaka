package com.kondenko.pocketwaka.screens.stats

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.kondenko.pocketwaka.R
import com.kondenko.pocketwaka.domain.stats.model.StatsItem
import com.kondenko.pocketwaka.utils.extensions.setInvisible
import kotlinx.android.synthetic.main.item_stats_item.view.*

class CardStatsListAdapter(private val context: Context, private val items: List<StatsItem>, private val isSkeleton: Boolean) : RecyclerView.Adapter<CardStatsListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_stats_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: StatsItem) {
            with(itemView) {
                textview_stats_item_percent?.apply {
                    if (item.percent != null) setPercent(item.percent)
                    else if (!isSkeleton) setInvisible()
                }
                textview_stats_item_name.text = item.name
                progressbar_stats_item_percentage.color = item.color
                progressbar_stats_item_percentage.progress = (item.percent?.toFloat() ?: 0f) / 100
                post { itemView.setupItemNameWidth() }
            }
        }
    }

    /**
     * Puts stats item's name outside of the ProgressBar is the name is too long.
     */
    private fun View.setupItemNameWidth() {
        val isTextWiderThanProgress = progressbar_stats_item_percentage.progressBarWidth * 1.1 <= textview_stats_item_name.width
        if (isTextWiderThanProgress && !isSkeleton) {
            textview_stats_item_name.updateLayoutParams {
                width = (progressbar_stats_item_percentage.width - progressbar_stats_item_percentage.progressBarWidth).toInt()
            }
            textview_stats_item_name.setTextColor(ContextCompat.getColor(context, R.color.color_stats_item_dark))
            textview_stats_item_name.x += progressbar_stats_item_percentage.progressBarWidth
        } else {
            textview_stats_item_name.updateLayoutParams {
                width = if (isSkeleton) {
                    progressbar_stats_item_percentage.width
                } else {
                    progressbar_stats_item_percentage.progressBarWidth.toInt()
                }
            }
        }
    }

    /**
     * Set the percent value so that if it's less than 1% it looks like < 1%
     */
    private fun TextView.setPercent(percent: Double) {
        text = if (percent > 1) {
            String.format(context.getString(R.string.stats_card_percent_format_int) as String, percent)
        } else {
            context.getString(R.string.stats_card_less_than_1_percent)
        }
    }

}