package com.bitcoin.wallet.btc.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.DataPagingListAdapter
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.model.news.DataItem

class StoriesMoreAdapter(
    private val retryCallback: () -> Unit,
    private val onClickNews: (url: String?) -> Unit?
) :
    DataPagingListAdapter<DataItem, RecyclerView.ViewHolder>(
        diffCallback = object : DiffUtil.ItemCallback<DataItem>() {
            override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
                return oldItem == newItem
            }
        }
    ) {

    override fun createBinding(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_news -> StoriesAdapter.StoriesViewHolder(
                parent.inflate(R.layout.item_news),
                null,
                onClickNews
            )
            else -> NetworkViewHolder.create(parent, retryCallback)
        }
    }

    override fun bind(binding: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_news -> {
                if (binding is StoriesAdapter.StoriesViewHolder) {
                    getItem(position)?.let { binding.initNews(it) }
                }
            }
            else -> {
                if (binding is NetworkViewHolder) {
                    binding.bindTo(networkState, position, true)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.item_network_state
        } else {
            R.layout.item_news
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }
}