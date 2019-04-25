package com.bitcoin.wallet.mobile.ui.adapter

import android.database.Cursor
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.mobile.Constants
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.data.ExchangeRate
import com.bitcoin.wallet.mobile.data.ExchangeRatesProvider
import com.bitcoin.wallet.mobile.extension.inflate
import com.bitcoin.wallet.mobile.service.BlockchainState
import com.bitcoin.wallet.mobile.ui.widget.CurrencyTextView
import org.bitcoinj.core.Coin
import org.bitcoinj.utils.Fiat
import java.util.*

class ExchangeRatesAdapter(private val onClickListener: OnClickListener?) :
    ListAdapter<ExchangeRatesAdapter.ListItem, ExchangeRatesAdapter.ViewHolder>(object :
        DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.currencyCode == newItem.currencyCode
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            if (oldItem.baseRateAsFiat != newItem.baseRateAsFiat)
                return false
            if (oldItem.baseRateMinDecimals != newItem.baseRateMinDecimals)
                return false
            return if (oldItem.balanceAsFiat != newItem.balanceAsFiat) false else oldItem.isSelected == newItem.isSelected
        }
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.item_exchange_rate))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listItem = getItem(position)
        holder.itemView.setBackgroundResource(if (listItem.isSelected) R.color.bg_list_selected else android.R.color.transparent)
        holder.defaultView.visibility = if (listItem.isSelected) View.VISIBLE else View.INVISIBLE
        holder.currencyCodeView.text = listItem.currencyCode
        holder.rateView.setFormat(Constants.LOCAL_FORMAT.minDecimals(listItem.baseRateMinDecimals))
        if (listItem.baseRateAsFiat.smallestUnitExponent() >= 0) {
            holder.rateView.setAmount(listItem.baseRateAsFiat)
        }
        holder.walletView.setFormat(Constants.LOCAL_FORMAT)
        if (listItem.balanceAsFiat != null) {
            if (listItem.balanceAsFiat.smallestUnitExponent() >= 0) {
                holder.walletView.setAmount(listItem.balanceAsFiat)
            }
            holder.walletView.setStrikeThru(false)
        } else {
            holder.walletView.text = "n/a"
            holder.walletView.setStrikeThru(false)
        }

        val onClickListener = this.onClickListener
        if (onClickListener != null) {
            holder.menuView.setOnClickListener { v ->
                onClickListener.onExchangeRateMenuClick(
                    v,
                    listItem.currencyCode
                )
            }
        }
    }

    interface OnClickListener {
        fun onExchangeRateMenuClick(view: View, currencyCode: String)
    }

    class ListItem(
        exchangeRate: ExchangeRate, val baseRateAsFiat: Fiat, val baseRateMinDecimals: Int,
        val balanceAsFiat: Fiat?, val isSelected: Boolean
    ) {
        val currencyCode: String = exchangeRate.currencyCode

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val defaultView: View = itemView.findViewById(R.id.exchange_rate_row_default)
        val currencyCodeView: TextView = itemView.findViewById(R.id.exchange_rate_row_currency_code)
        val rateView: CurrencyTextView = itemView.findViewById(R.id.exchange_rate_row_rate)
        val walletView: CurrencyTextView = itemView.findViewById(R.id.exchange_rate_row_balance)
        val menuView: ImageButton = itemView.findViewById(R.id.exchange_rate_row_menu)

    }

    companion object {

        fun buildListItems(
            cursor: Cursor, balance: Coin?,
            blockchainState: BlockchainState?, defaultCurrency: String, rateBase: Coin
        ): List<ListItem> {
            val items = ArrayList<ListItem>(cursor.count)
            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {
                val exchangeRate = ExchangeRatesProvider.getExchangeRate(cursor)
                val baseRateAsFiat = exchangeRate.rate.coinToFiat(rateBase)
                val baseRateMinDecimals = if (!rateBase.isLessThan(Coin.COIN)) 2 else 4
                val balanceAsFiat = if (balance != null && (blockchainState == null || !blockchainState.replaying))
                    exchangeRate.rate.coinToFiat(balance)
                else
                    null
                val isDefaultCurrency = exchangeRate.currencyCode == defaultCurrency
                items.add(
                    ListItem(exchangeRate, baseRateAsFiat, baseRateMinDecimals, balanceAsFiat, isDefaultCurrency)
                )
            }
            return items
        }
    }
}