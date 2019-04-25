package com.bitcoin.wallet.mobile.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.extension.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_stats.*

class StatsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var itemList: MutableList<Any> = mutableListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_stats -> StatViewHolder(parent.inflate(R.layout.item_stats))
            else -> TitleViewHolder(parent.inflate(R.layout.item_title))
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (itemList[position] is String) {
            R.layout.item_title
        } else {
            R.layout.item_stats
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = itemList[position]
        when(getItemViewType(position)) {
            R.layout.item_stats -> {
                if (holder is StatViewHolder && item is StatData) {
                    holder.apply {
                        tvTitle.text = item.title
                        tvValue.text = item.value
                        if (position % 2 ==0) {
                            tvTitle.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorInvertedDarkThemeAlpha2))
                            tvValue.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorInvertedDarkThemeAlpha2))
                        } else {
                            tvTitle.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
                            tvValue.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
                        }
                    }
                }
            }
            else -> {
                if (holder is TitleViewHolder && item is String) {
                    holder.apply {
                        tvTitle.text = item
                    }
                }
            }
        }
    }

    class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    data class StatData(
        val title: String,
        val value: String
    )
}