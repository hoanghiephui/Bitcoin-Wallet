package com.bitcoin.wallet.btc.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.data.AddressBookEntry
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.utils.WalletUtils
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_address_old.*
import kotlinx.android.synthetic.main.item_wallet_address.*
import org.bitcoinj.core.Address
import org.bitcoinj.wallet.Wallet
import java.util.*

class AddressAdapter(private val callback: AddressCallback) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val derivedAddresses = ArrayList<Address>()
    private val randomAddresses = ArrayList<Address>()
    private var wallet: Wallet? = null
    private var addressBook: Map<String, AddressBookEntry>? = null

    fun replaceDerivedAddresses(addresses: Collection<Address>) {
        this.derivedAddresses.clear()
        this.derivedAddresses.addAll(addresses)
        notifyDataSetChanged()
    }

    fun replaceRandomAddresses(addresses: Collection<Address>) {
        this.randomAddresses.clear()
        this.randomAddresses.addAll(addresses)
        notifyDataSetChanged()
    }

    fun setWallet(wallet: Wallet) {
        this.wallet = wallet
        notifyDataSetChanged()
    }

    fun setAddressBook(addressBook: Map<String, AddressBookEntry>) {
        this.addressBook = addressBook
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                AddressViewHolder(parent.inflate(R.layout.item_wallet_address))
            }
            else -> {
                AddressOldViewHolder(parent.inflate(R.layout.item_address_old))
            }
        }
    }

    override fun getItemCount(): Int {
        var count = derivedAddresses.size
        if (randomAddresses.isNotEmpty())
            count += randomAddresses.size + 1
        return count
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            0 -> {
                if (holder is AddressViewHolder) {
                    holder.apply {
                        var address: Address? = null
                        val numDerived = derivedAddresses.size
                        if (position < numDerived)
                            address = derivedAddresses[position]
                        else if (position > numDerived) {
                            address = randomAddresses[position - numDerived - 1]
                        }
                        val isRotateKey: Boolean
                        isRotateKey = if (wallet != null) {
                            val key = wallet?.findKeyFromAddress(address)
                            wallet != null && wallet?.isKeyRotating(key) ?: false
                        } else {
                            false
                        }
                        address?.let {
                            tvAddress.text = WalletUtils.formatAddress(
                                it, Constants.ADDRESS_FORMAT_GROUP_SIZE,
                                Constants.ADDRESS_FORMAT_LINE_SIZE, false
                            )
                            tvAddress.setTextColor(
                                if (isRotateKey) ContextCompat.getColor(
                                    itemView.context,
                                    R.color.fg_insignificant
                                )
                                else ContextCompat.getColor(
                                    itemView.context,
                                    R.color.colorAccentBlack
                                )
                            )
                        }
                        if (addressBook != null) {
                            val entry = addressBook!![address.toString()]
                            if (entry != null) {
                                labelView.text = entry.label
                                labelView.setTextColor(
                                    if (isRotateKey) ContextCompat.getColor(
                                        itemView.context,
                                        R.color.fg_insignificant
                                    )
                                    else ContextCompat.getColor(
                                        itemView.context,
                                        R.color.fg_less_significant
                                    )
                                )
                            } else {
                                labelView.setText(R.string.address_unlabeled)
                                labelView.setTextColor(
                                    ContextCompat.getColor(
                                        itemView.context,
                                        R.color.fg_insignificant
                                    )
                                )
                            }
                        } else {
                            labelView.setText(R.string.address_unlabeled)
                            labelView.setTextColor(
                                ContextCompat.getColor(
                                    itemView.context,
                                    R.color.fg_insignificant
                                )
                            )
                        }
                        tvMessage.visibility = if (isRotateKey) View.VISIBLE else View.GONE
                        btnMore.setOnClickListener {
                            callback.onClickItem(
                                layoutPosition,
                                it,
                                address
                            )
                        }
                    }
                }
            }
            else -> {
                if (holder is AddressOldViewHolder) {
                    holder.tvOld.text = holder.itemView.context.getString(R.string.receiving_random)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val numDerived = derivedAddresses.size
        return when {
            position < numDerived -> 0
            position == numDerived -> 1
            else -> 0
        }
    }

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class AddressOldViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    interface AddressCallback {
        fun onClickItem(position: Int, view: View, address: Address?)
    }
}