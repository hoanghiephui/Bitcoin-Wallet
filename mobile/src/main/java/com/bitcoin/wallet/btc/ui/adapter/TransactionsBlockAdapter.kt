package com.bitcoin.wallet.btc.ui.adapter

import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.extension.invisible
import com.bitcoin.wallet.btc.model.SummaryModel
import com.bitcoin.wallet.btc.model.transactions.TxItem
import com.bitcoin.wallet.btc.repository.NetworkState
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_summary.*
import kotlinx.android.synthetic.main.item_top_block.*
import kotlinx.android.synthetic.main.item_transaction_block.*
import java.text.NumberFormat
import java.util.*

class TransactionsBlockAdapter(private val retryCallback: () -> Unit) :
    ListAdapter<Any, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return oldItem != newItem
            }
        }
    ) {

    private val loc = Locale.US
    val number: NumberFormat = NumberFormat.getInstance(loc).apply {
        //minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_transaction_block -> TransactionViewHolder(parent.inflate(R.layout.item_transaction_block))
            R.layout.item_network_state -> NetworkViewHolder.create(parent, retryCallback)
            R.layout.item_summary -> SummaryViewHolder(parent.inflate(R.layout.item_summary))
            R.layout.item_top_block -> TopViewHolder(parent.inflate(R.layout.item_top_block))
            else -> throw IllegalArgumentException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_transaction_block -> {
                if (getItem(position) is TxItem) {
                    if (holder is TransactionViewHolder) {
                        holder.apply {
                            val item = getItem(position) as TxItem
                            item.let {
                                tvBlock.text = it.hash
                                if (it.fee != null) {
                                    tvFee.text = "FEE: ".plus(number.format(it.fee).plus(" BTC"))
                                }
                                viewFrom.removeAllViews()
                                val layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                layoutParams.setMargins(0, 15, 0, 0)
                                it.inputs?.map { vinItem ->
                                    val viewF =
                                        LayoutInflater.from(itemView.context)
                                            .inflate(R.layout.item_address, null)
                                    val tvAddress = viewF.findViewById<TextView>(R.id.tvAddress)
                                    val tvValue = viewF.findViewById<TextView>(R.id.tvValue)
                                    if (vinItem.prevOut == null) {
                                        tvAddress.text = "No Inputs (Newly Generated Coins)"
                                    } else {
                                        tvAddress.text = vinItem.prevOut.addr
                                        tvValue.text =
                                            number.format(vinItem.prevOut.value)?.plus(" BTC")
                                    }
                                    viewFrom.addView(viewF, layoutParams)
                                }
                                viewTo.removeAllViews()
                                //var value: Long ? = null
                                it.out?.map { voutItem ->
                                    val viewT =
                                        LayoutInflater.from(itemView.context)
                                            .inflate(R.layout.item_address, null)
                                    val tvAddress = viewT.findViewById<TextView>(R.id.tvAddress)
                                    val tvValue = viewT.findViewById<TextView>(R.id.tvValue)
                                    if (voutItem.addr == null) {
                                        tvAddress.text = "Unable to decode output address"
                                        tvAddress.setTextColor(
                                            ContextCompat.getColor(
                                                itemView.context,
                                                R.color.fg_error
                                            )
                                        )
                                    } else {
                                        tvAddress.text = voutItem.addr
                                        tvValue.text = number.format(voutItem.value).plus(" BTC")
                                        tvAddress.setTextColor(
                                            ContextCompat.getColor(
                                                itemView.context,
                                                R.color.color_address
                                            )
                                        )
                                    }
                                    viewTo.addView(viewT, layoutParams)
                                    //value = value?.plus(voutItem.value ?: 0)
                                }
                                btnConfirm.invisible()
                                var value = 0L
                                it.out?.map {
                                    value = value.plus(it.value ?: 0L)
                                }
                                tvCountBitcoin.text = number.format(value).plus(" BTC")
                                tvDay.time = it.time?.times(1000) ?: 1000000
                            }
                        }
                    }
                }
            }
            R.layout.item_summary -> {
                if (getItem(position) is SummaryModel && holder is SummaryViewHolder) {
                    val item = getItem(position) as SummaryModel
                    holder.apply {
                        tvTitle.text = item.title
                        if (position == 9 || position == 15) {
                            tvDesc.setTextColor(
                                ContextCompat.getColor(
                                    itemView.context,
                                    R.color.colorAccent
                                )
                            )
                        }
                        if (position == 6) {
                            var times: String
                            val timeMs = item.desc?.toLong()?.times(DateUtils.SECOND_IN_MILLIS)
                            timeMs?.let {
                                times = if (timeMs < Date().time - DateUtils.MINUTE_IN_MILLIS)
                                    DateUtils.getRelativeDateTimeString(
                                        itemView.context, timeMs, DateUtils.MINUTE_IN_MILLIS,
                                        DateUtils.WEEK_IN_MILLIS, 0
                                    ).toString()
                                else
                                    itemView.context.getString(R.string.now)
                                tvDesc.text = times
                            }
                        } else {
                            tvDesc.text = item.desc
                        }
                    }
                }
            }
            R.layout.item_top_block -> {
                if (getItem(position) is String && holder is TopViewHolder) {
                    holder.apply {
                        if (position == 1) {
                            tvBlockTop.background =
                                ContextCompat.getDrawable(
                                    itemView.context,
                                    R.drawable.radius_line_small
                                )
                            tvBlockTop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        } else {
                            tvBlockTop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        }
                        tvBlockTop.text = getItem(position) as String
                    }
                }
            }
            R.layout.item_network_state -> (holder as NetworkViewHolder).bindTo(
                networkState,
                position,
                true
            )
        }
    }

    override fun getItemCount(): Int {
        return (super.getItemCount() + if (super.getItemCount() > 0) 1 else 0) + if (hasExtraRow()) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.item_network_state
        } else if (getItem(position) is TxItem) {
            R.layout.item_transaction_block
        } else if (getItem(position) is String) {
            R.layout.item_top_block
        } else {
            R.layout.item_summary
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class TopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    /**
     * Set the current network state to the adapter
     * but this work only after the initial load
     * and the adapter already have list to add new loading raw to it
     * so the initial loading state the activity responsible for handle it
     *
     * @param newNetworkState the new network state
     */
    var networkState: NetworkState? = null

    fun onNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newNetworkState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    private fun hasExtraRow(): Boolean {
        return networkState != null && networkState != NetworkState.LOADED
    }
}