package com.bitcoin.wallet.btc.ui.adapter.explorer

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.DataBoundListAdapter
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.model.explorer.BlocksItem
import com.bitcoin.wallet.btc.ui.adapter.NetworkViewHolder
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_latest_block_explorer.*
import kotlinx.android.synthetic.main.item_latest_block_group_explorer.*

class ItemLatestBlockAdapter(
    private val onClickItem: (String?) -> Unit,
    private val retryCallback: () -> Unit,
    private val viewAll: () -> Unit
) :
    DataBoundListAdapter<BlocksItem, RecyclerView.ViewHolder>(
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
            R.layout.item_latest_block_group_explorer -> HeaderBlockViewHolder(parent.inflate(R.layout.item_latest_block_group_explorer))
            else -> ItemLatestBlockViewHolder(parent.inflate(R.layout.item_latest_block_explorer))
        }
    }

    override fun bind(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_latest_block_explorer -> {
                if (holder is ItemLatestBlockViewHolder) {
                    val item = getItem(position - 1)
                    holder.apply {
                        tvAge.time = item.time?.times(1000) ?: 0
                        tvHeight.text = item.height?.toString()
                        tvSize.text = item.size?.toString()
                        tvMined.text = item.poolInfo?.poolName ?: "Unknown"
                        tvTransactions.text = item.txlength?.toString()
                        itemView.setOnClickListener {
                            onClickItem(item.hash)
                        }
                    }
                }
            }

            R.layout.item_latest_block_group_explorer -> {
                if (holder is HeaderBlockViewHolder) {
                    holder.btnViewAll.setOnClickListener {
                        viewAll()
                    }
                }
            }

            R.layout.item_network_state -> {
                if (holder is NetworkViewHolder) {
                    holder.bindTo(networkState, position, true)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return (super.getItemCount() + if (hasExtraRow()) 1 else 0) + if (super.getItemCount() > 0) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.item_network_state
        } else if (position == 0 && itemCount > 0) {
            R.layout.item_latest_block_group_explorer
        } else {
            R.layout.item_latest_block_explorer
        }
    }

    class HeaderBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        LayoutContainer {
        /** Returns the root holder view. */
        override val containerView: View?
            get() = itemView
    }

    class ItemLatestBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        LayoutContainer {
        override val containerView: View?
            get() = itemView
    }
}