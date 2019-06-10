package com.bitcoin.wallet.btc.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.model.blocks.BlocksItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_last_block.*

class BlocksAdapter(private val callback: MainAdapter.MainCallback) : ListAdapter<BlocksItem, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<BlocksItem>() {
        override fun areItemsTheSame(oldItem: BlocksItem, newItem: BlocksItem): Boolean {
            return oldItem.hash == newItem.hash
        }

        override fun areContentsTheSame(oldItem: BlocksItem, newItem: BlocksItem): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_header_block -> HeaderBlockViewHolder(parent.inflate(R.layout.item_header_block))
            else -> BlockItemViewHolder(parent.inflate(R.layout.item_last_block))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_last_block -> {
                if (holder is BlockItemViewHolder) {
                    val item = getItem(position - 1)
                    holder.apply {
                        tvHeight.text = item.height?.toString()
                        tvAge.time = (item.time?.toLong())?.times(1000) ?: 1560140163
                        tvTrans.text = item.txCount?.toString()
                        tvSize.text = item.size?.toString()
                        itemView.setOnClickListener {
                            callback.onClickBlocks(item.hash)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (super.getItemCount() > 0) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && itemCount > 0) {
            R.layout.item_header_block
        } else {
            R.layout.item_last_block
        }
    }

    class BlockItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class HeaderBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        /** Returns the root holder view. */
        override val containerView: View?
            get() = itemView
    }
}