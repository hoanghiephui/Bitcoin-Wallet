package com.bitcoin.wallet.mobile.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.mobile.Constants
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.data.AddressBookEntry
import com.bitcoin.wallet.mobile.extension.inflate
import com.bitcoin.wallet.mobile.utils.WalletUtils
import kotlinx.android.synthetic.main.item_wallet_address.*

class AddressSendAdapter(private val callback: SendAddressCallback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listItem = ArrayList<AddressBookEntry>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AddressAdapter.AddressViewHolder(parent.inflate(R.layout.item_wallet_address))
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AddressAdapter.AddressViewHolder) {
            holder.apply {
                val item = listItem[position]
                labelView.text = item.label
                tvAddress.text = WalletUtils.formatHash(
                    item.address, Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE, false
                )
                btnMore.setOnClickListener {
                    callback.onClickItemSend(layoutPosition, it, item)
                }
            }
        }
    }

    interface SendAddressCallback {
        fun onClickItemSend(position: Int, view: View, address: AddressBookEntry?)
    }
}