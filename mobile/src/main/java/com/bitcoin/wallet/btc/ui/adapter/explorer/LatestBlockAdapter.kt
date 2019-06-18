package com.bitcoin.wallet.btc.ui.adapter.explorer

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.DataPagingListAdapter
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.model.explorer.BlocksItem
import com.bitcoin.wallet.btc.ui.adapter.NetworkViewHolder
import kotlinx.android.synthetic.main.item_latest_block_explorer.*

class LatestBlockAdapter(
    private val retryCallback: () -> Unit,
    private val onClickItem: (String?) -> Unit
) : DataPagingListAdapter<BlocksItem, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<BlocksItem>() {
        override fun areItemsTheSame(oldItem: BlocksItem, newItem: BlocksItem): Boolean {
            return oldItem.hash == newItem.hash
        }

        override fun areContentsTheSame(oldItem: BlocksItem, newItem: BlocksItem): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun createBinding(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_network_state -> NetworkViewHolder.create(parent, retryCallback)
            else -> ItemLatestBlockAdapter.ItemLatestBlockViewHolder(parent.inflate(R.layout.item_latest_block_explorer))
        }
    }

    override fun bind(binding: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_latest_block_explorer -> {
                if (binding is ItemLatestBlockAdapter.ItemLatestBlockViewHolder) {
                    val item = getItem(position)
                    binding.apply {
                        tvAge.time = item?.time?.times(1000) ?: 0
                        tvHeight.text = item?.height?.toString()
                        tvSize.text = item?.size?.toString()
                        tvMined.text = item?.poolInfo?.poolName ?: "Unknown"
                        tvTransactions.text = item?.txlength?.toString()
                        itemView.setOnClickListener {
                            onClickItem(item?.hash)
                        }
                    }
                }
            }

            R.layout.item_network_state -> {
                if (binding is NetworkViewHolder) {
                    binding.bindTo(networkState, position, true)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return (super.getItemCount() + if (hasExtraRow()) 1 else 0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.item_network_state
        } else {
            R.layout.item_latest_block_explorer
        }
    }
}