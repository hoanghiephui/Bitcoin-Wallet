package com.bitcoin.wallet.btc.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.model.info.Data
import com.bitcoin.wallet.btc.model.summary.DataItem
import com.bitcoin.wallet.btc.utils.Utils
import com.bumptech.glide.Glide
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_discover.*
import java.text.NumberFormat
import java.util.*

class DiscoverAdapter : ListAdapter<DataItem, DiscoverAdapter.DiscoverViewHolder>(
    object : DiffUtil.ItemCallback<DataItem>() {
        override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
            return oldItem == newItem
        }
    }
) {
    var list: List<Data> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverViewHolder {
        return DiscoverViewHolder(parent.inflate(R.layout.item_discover))
    }

    override fun onBindViewHolder(holder: DiscoverViewHolder, position: Int) {
        val item = getItem(position)

        holder.apply {
            Glide.with(imgCoin)
                .load(list[position].imageUrl)
                .into(imgCoin)
            txtName.text = item.name
            txtShort.text = item.base
            item.latest?.let {
                txtPrice.text = nf.format(it.toDouble())
            }
            item.percentChange?.let {
                if (Utils.isNegative(it)) {
                    txtPercent.text = (number.format(it * 100).plus("%"))
                    txtPercent.setTextColor(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            R.color.product_red_medium
                        )
                    )
                    txtPrice.chipBackgroundColor = ContextCompat.getColorStateList(
                        holder.itemView.context,
                        R.color.product_red_medium
                    )
                } else {
                    txtPercent.text = "+".plus(number.format(it * 100).plus("%"))
                    txtPercent.setTextColor(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            R.color.product_green_medium
                        )
                    )
                    txtPrice.chipBackgroundColor = ContextCompat.getColorStateList(
                        holder.itemView.context,
                        R.color.product_green_medium
                    )
                }
            }
            itemView.setOnClickListener { }
        }
    }

    class DiscoverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        val loc = Locale.US
        val nf = NumberFormat.getCurrencyInstance(loc)
        val number = NumberFormat.getInstance(loc).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        override val containerView: View?
            get() = itemView
    }
}