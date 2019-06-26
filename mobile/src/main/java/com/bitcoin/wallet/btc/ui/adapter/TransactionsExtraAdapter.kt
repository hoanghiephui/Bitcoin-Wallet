package com.bitcoin.wallet.btc.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.DataPagingListAdapter
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.extension.invisible
import com.bitcoin.wallet.btc.extension.visible
import com.bitcoin.wallet.btc.model.explorer.address.AddressResponse
import com.bitcoin.wallet.btc.model.explorer.details.BlockDetailResponse
import com.bitcoin.wallet.btc.model.explorer.transaction.TxsItem
import com.bitcoin.wallet.btc.model.explorer.tx.TxResponse
import com.bitcoin.wallet.btc.model.explorer.tx.VinItem
import com.bitcoin.wallet.btc.model.explorer.tx.VoutItem
import com.bitcoin.wallet.btc.repository.data.TransactionsDataSource
import com.bitcoin.wallet.btc.ui.fragments.WalletAddressBottomDialog
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_explorer_address.*
import kotlinx.android.synthetic.main.item_explorer_title.*
import kotlinx.android.synthetic.main.item_explorer_title.tvTitle
import kotlinx.android.synthetic.main.item_explorer_transactions.*
import kotlinx.android.synthetic.main.item_explorer_transactions.tvFees
import kotlinx.android.synthetic.main.item_input.*
import kotlinx.android.synthetic.main.item_top_transaction.*
import kotlinx.android.synthetic.main.item_transaction_extra_top.*
import kotlinx.android.synthetic.main.item_transaction_extra_top.btnCopyHash
import kotlinx.android.synthetic.main.item_transaction_extra_top.tvAgeS
import kotlinx.android.synthetic.main.item_transaction_extra_top.tvHashBlock
import kotlinx.android.synthetic.main.item_transaction_extra_top.tvTimestamp
import java.text.NumberFormat
import java.util.*

class TransactionsExtraAdapter(private val retryCallback: () -> Unit,
                               private val showQrCode: (String?) -> Unit,
                               private val clickTransactionId: (String?) -> Unit
) :
    DataPagingListAdapter<Any, RecyclerView.ViewHolder>(
        diffCallback = object : DiffUtil.ItemCallback<Any>() {
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
        minimumFractionDigits = 8
        maximumFractionDigits = 8
    }

    override fun createBinding(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_explorer_transactions -> TransactionsViewHolder(parent.inflate(R.layout.item_explorer_transactions))
            R.layout.item_network_state -> NetworkViewHolder.create(parent, retryCallback)
            R.layout.item_explorer_title -> TitleViewHolder(parent.inflate(R.layout.item_explorer_title))
            R.layout.item_explorer_address -> AddressViewHolder(parent.inflate(R.layout.item_explorer_address))
            R.layout.item_transaction_extra_top -> BlocksViewHolder(parent.inflate(R.layout.item_transaction_extra_top))
            R.layout.item_input, 1234 -> TransactionDetailViewHolder(parent.inflate(R.layout.item_input))
            R.layout.item_top_transaction -> TopTransactionDetailViewHolder(parent.inflate(R.layout.item_top_transaction))
            else -> throw IllegalArgumentException("unknown view type")
        }
    }

    override fun bind(binding: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_explorer_transactions -> {
                if (binding is TransactionsViewHolder) {
                    binding.apply {
                        val txs = getItem(position) as TxsItem
                        tvAgeT.time = txs.time?.times(1000) ?: 0
                        tvTransactionId.text = txs.txid
                        tvConfirmT.text = txs.confirmations?.toString()
                        tvInputs.text = txs.vin?.size?.toString()
                        tvOutputs.text = txs.vout?.size?.toString()
                        tvOutput.text = number.format(txs.valueOut ?: 0)?.plus(" BTC")
                        tvFees.text = number.format(txs.fees ?: 0)?.plus(" BTC")
                        tvSizeT.text = txs.size?.toString()
                        tvTransactionId.setOnClickListener {
                            clickTransactionId(txs.txid)
                        }
                    }
                }
            }

            R.layout.item_network_state -> {
                if (binding is NetworkViewHolder) {
                    binding.bindTo(networkState, position, true)
                }
            }

            R.layout.item_explorer_title -> {
                if (binding is TitleViewHolder) {
                    val title = getItem(position) as TransactionsDataSource.Response.Title
                    binding.apply {
                        tvTitle.text = title.title
                        chipCout.text = title.content
                    }
                }
            }

            R.layout.item_explorer_address -> {
                if (binding is AddressViewHolder) {
                    binding.apply {
                        val address = getItem(position) as AddressResponse
                        tvAddressS.text = address.addrStr
                        tvAddress.text = address.addrStr
                        tvBtcReceived.text = number.format(address.totalReceived).plus(" BTC")
                        tvBtcSent.text = number.format(address.totalSent).plus(" BTC")
                        tvBtcBalance.text = number.format(address.balance).plus(" BTC")
                        btnCopy.setOnClickListener {
                            address.addrStr?.let {
                                WalletAddressBottomDialog.copyTextToClipboard(this.itemView.context, it)
                                Toast.makeText(this.itemView.context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                        btnQrCode.setOnClickListener {
                            showQrCode(address.addrStr)
                        }
                    }
                }
            }

            R.layout.item_transaction_extra_top -> {
                if (binding is BlocksViewHolder) {
                    binding.apply {
                        val summary = getItem(position) as BlockDetailResponse
                        tvHashBlock.text = summary.hash
                        tvBlockH.text = summary.height?.toString()
                        tvPreBlock.text = if (summary.previousblockhash != null) (summary.height?.minus(1)).toString() else ""
                        txMining.text = if (summary.nextblockhash != null) (summary.height?.plus(1)).toString() else "Mining"
                        imageView5.setImageResource(if (summary.nextblockhash != null) R.drawable.ic_block_selected else R.drawable.ic_block_mining)
                        tvAgeS.time = summary.time?.times(1000) ?: 0
                        tvTimestamp.text = summary.time?.toString()
                        tvDifficulty.text = number.also {
                            it.minimumFractionDigits = 2
                            it.maximumFractionDigits = 2
                        }.format(summary.difficulty)
                        tvBits.text = summary.bits
                        tvNonceS.text = summary.nonce?.toString()
                        tvHeightS.text = summary.height?.toString()
                        tvConfirmS.text = summary.confirmations?.toString()
                        tvSizeS.text = summary.size?.toString().plus(" kB")
                        tvBlockReward.text = number.format(summary.reward).plus(" BTC")
                        tvMerkle.text = summary.merkleroot
                        tvChainwork.text = summary.chainwork
                        btnCopyHash.setOnClickListener {
                            summary.hash?.let {
                                WalletAddressBottomDialog.copyTextToClipboard(this.itemView.context, it)
                                Toast.makeText(this.itemView.context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            R.layout.item_top_transaction -> {
                if (binding is TopTransactionDetailViewHolder) {
                    binding.apply {
                        val item = getItem(position) as TxResponse
                        tvHashBlock.text = item.txid
                        tvCoinbase.text = if (item.isCoinBase == true) "Yes" else "No"
                        item.confirmations?.let {
                            if (it > 6) {
                                btnConfirm.text = "Confirmed"
                                btnConfirm.background =
                                    ContextCompat.getDrawable(itemView.context, R.drawable.bg_confirm)
                                progressConfirm.invisible()
                            } else {
                                btnConfirm.text = it.toString().plus("/6 Confirmations")
                                btnConfirm.background =
                                    ContextCompat.getDrawable(itemView.context, R.drawable.bg_pending)
                                progressConfirm.visible()
                                progressConfirm.progress = it
                            }
                        }
                        tvAgeS.time = item.blocktime?.times(1000) ?: 0
                        tvTimestamp.text = item.blocktime?.toString()
                        tvBlock.text = item.blockheight?.toString()
                        tvFees.text = "0.00000000 BTC"
                        item.fees?.let {
                            tvFees.text = number.format(it).plus(" BTC")
                        }
                        tvConfirmSs.text = item.confirmations?.toString()
                        tvTotalInput.text = "0.00000000 BTC"
                        item.valueIn?.let {
                            tvTotalInput.text = number.format(it).plus(" BTC")
                        }
                        tvTotalOut.text = "0.00000000 BTC"
                        item.valueOut?.let {
                            tvTotalOut.text = number.format(it).plus(" BTC")
                        }
                        tvSizes.text = item.size?.toString()
                        btnCopyHash.setOnClickListener {
                            item.txid?.let {
                                WalletAddressBottomDialog.copyTextToClipboard(this.itemView.context, it)
                                Toast.makeText(this.itemView.context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            R.layout.item_input -> {
                if (binding is TransactionDetailViewHolder) {
                    binding.apply {
                        val item = getItem(position)
                        if (item is VinItem) {
                            if (item.addr == null) {
                                tvType.text = "No input"
                                tvTypeS.text = "(Newly mined coins)"
                                group.gone()
                            } else {
                                itemView.setBackgroundColor(if (position %2 == 0) ContextCompat.getColor(itemView.context, R.color.colorInvertedAlternate) else
                                    ContextCompat.getColor(itemView.context, R.color.colorInvertedAlpha))
                                group.visible()
                                tvAddressT.text = item.addr
                                tvTypeS.text = ""
                                item.value?.let {
                                    tvAmount.text = number.format(it).plus(" BTC")
                                }
                                tvN.text = item.N?.toString()
                                tvType.text = "Input"
                                tvAddressT.setOnClickListener {
                                    WalletAddressBottomDialog.copyTextToClipboard(this.itemView.context, tvAddressT.text.toString())
                                    Toast.makeText(this.itemView.context, "Copied", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
            1234 -> {
                if (binding is TransactionDetailViewHolder) {
                    binding.apply {
                        val item = getItem(position)
                        if (item is VoutItem) {
                            itemView.setBackgroundColor(if (position %2 == 0) ContextCompat.getColor(itemView.context, R.color.colorInvertedAlternate) else
                                ContextCompat.getColor(itemView.context, R.color.colorInvertedAlpha))
                            tvType.text = "Output"
                            tvAddressT.text = item.scriptPubKey?.addresses?.get(0) ?: "Unparsed address"
                            item.value?.let {
                                tvAmount.text = it.plus(" BTC")
                            }
                            tvN.text = item.N?.toString()
                            tvTypeS.text = if (item.spentTxId != null) "Spent" else "Unspent"
                            tvAddressT.setOnClickListener {
                                WalletAddressBottomDialog.copyTextToClipboard(this.itemView.context, tvAddressT.text.toString())
                                Toast.makeText(this.itemView.context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.item_network_state
        } else if (getItem(position) is TxsItem) {
            R.layout.item_explorer_transactions
        } else if (getItem(position) is TransactionsDataSource.Response.Title) {
            R.layout.item_explorer_title
        } else if (getItem(position) is AddressResponse) {
            R.layout.item_explorer_address
        } else if (getItem(position) is VinItem) {
            R.layout.item_input
        } else if (getItem(position) is VoutItem) {
            1234
        } else if (getItem(position) is TxResponse) {
            R.layout.item_top_transaction
        } else {
            R.layout.item_transaction_extra_top
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class BlocksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class TransactionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class TransactionDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class TopTransactionDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }
}