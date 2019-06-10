package com.bitcoin.wallet.btc.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.model.news.DataItem
import com.bumptech.glide.Glide
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_news.*

class StoriesAdapter(private val callback: MainAdapter.MainCallback) : ListAdapter<DataItem, StoriesAdapter.StoriesViewHolder>(
    object : DiffUtil.ItemCallback<DataItem>() {
        override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
            return oldItem == newItem
        }
    }
) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoriesViewHolder {
        return StoriesViewHolder(parent.inflate(R.layout.item_news), callback, null)
    }

    override fun onBindViewHolder(holder: StoriesViewHolder, position: Int) {
        val item = getItem(position)
        holder.initNews(item)
    }

    class StoriesViewHolder(
        itemView: View,
        private val callback: MainAdapter.MainCallback?,
        private val onClickNews: ((url: String?) -> Unit?)?
    ) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView

        fun initNews(item: DataItem) {
            txtTime.text = ""
            txtTitle.text = item.title
            txtSource.text = item.attributionSource
            item.images?.let {
                if (it.isNotEmpty()) {
                    val urlImage = it[0].url
                    Glide.with(imgNews)
                        .load(urlImage)
                        .into(imgNews)
                } else {
                    Glide.with(imgNews)
                        .load("https://lh3.googleusercontent.com/DLuhme9GxQ0hmrJaRpyJMGsKmiXG-J0US8nYykXvGYRjOIlyr2F7JwUncHb_bN34_G5I=s360")
                        .into(imgNews)
                }
            }
            itemView.setOnClickListener {
                item.linkUrl?.let { it1 ->
                    if (callback != null) {
                        callback.onClickNews(it1)
                    } else {
                        onClickNews?.let { it2 -> it2(it1) }
                    }
                }
            }
        }
    }
}