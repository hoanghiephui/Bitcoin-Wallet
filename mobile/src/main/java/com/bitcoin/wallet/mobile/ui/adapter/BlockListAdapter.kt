package com.bitcoin.wallet.mobile.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.mobile.Constants
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.data.AddressBookEntry
import com.bitcoin.wallet.mobile.extension.inflate
import com.bitcoin.wallet.mobile.ui.widget.CurrencyTextView
import com.bitcoin.wallet.mobile.utils.WalletUtils
import org.bitcoinj.core.*
import org.bitcoinj.utils.MonetaryFormat
import org.bitcoinj.wallet.Wallet
import java.util.*

class BlockListAdapter(context: Context, private val onClickListener: OnClickListener?) :
    ListAdapter<BlockListAdapter.ListItem, BlockListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.blockHash == newItem.blockHash
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.time == newItem.time
        }
    }) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    class ListItem(
        context: Context, block: StoredBlock, time: Date, val format: MonetaryFormat,
        transactions: Set<Transaction>?, wallet: Wallet?,
        addressBook: Map<String, AddressBookEntry>?
    ) {
        val blockHash: Sha256Hash = block.header.hash
        val height: Int = block.height
        val time: String
        val isMiningRewardHalvingPoint: Boolean
        val isDifficultyTransitionPoint: Boolean
        val transactions: MutableList<ListTransaction>

        init {
            val timeMs = block.header.timeSeconds * DateUtils.SECOND_IN_MILLIS
            if (timeMs < time.time - DateUtils.MINUTE_IN_MILLIS)
                this.time = DateUtils.getRelativeDateTimeString(
                    context, timeMs, DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS, 0
                ).toString()
            else
                this.time = context.getString(R.string.just_now)
            this.isMiningRewardHalvingPoint = isMiningRewardHalvingPoint(block)
            this.isDifficultyTransitionPoint = isDifficultyTransitionPoint(block)
            this.transactions = LinkedList()
            if (transactions != null && wallet != null) {
                for (tx in transactions) {
                    val appearsInHashes = tx.appearsInHashes
                    if (appearsInHashes != null && appearsInHashes.containsKey(blockHash))
                        this.transactions.add(ListTransaction(context, tx, wallet, addressBook))
                }
            }
        }

        private fun isMiningRewardHalvingPoint(storedPrev: StoredBlock): Boolean {
            return (storedPrev.height + 1) % 210000 == 0
        }

        private fun isDifficultyTransitionPoint(storedPrev: StoredBlock): Boolean {
            return (storedPrev.height + 1) % Constants.NETWORK_PARAMETERS.interval == 0
        }

        class ListTransaction(
            context: Context, tx: Transaction, wallet: Wallet,
            addressBook: Map<String, AddressBookEntry>?
        ) {
            val fromTo: String
            val address: Address?
            val label: String?
            val value: Coin

            init {
                val isCoinBase = tx.isCoinBase
                val isInternal = tx.purpose == Transaction.Purpose.KEY_ROTATION

                this.value = tx.getValue(wallet)
                val sent = value.signum() < 0
                val self = WalletUtils.isEntirelySelf(tx, wallet)
                if (sent)
                    this.address = WalletUtils.getToAddressOfSent(tx, wallet)
                else
                    this.address = WalletUtils.getWalletAddressOfReceived(tx, wallet)

                if (isInternal || self)
                    this.fromTo = context.getString(R.string.symbol_internal)
                else if (sent)
                    this.fromTo = context.getString(R.string.symbol_to)
                else
                    this.fromTo = context.getString(R.string.symbol_from)

                if (isCoinBase) {
                    this.label = context.getString(R.string.min_coinbase)
                } else if (isInternal || self) {
                    this.label = context.getString(R.string.internal)
                } else if (address != null && addressBook != null) {
                    val entry = addressBook[address.toString()]
                    if (entry != null)
                        this.label = entry.label
                    else
                        this.label = "?"
                } else {
                    this.label = "?"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.item_wallet_block))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listItem = getItem(position)

        holder.heightView.text = listItem.height.toString()
        holder.timeView.text = listItem.time
        holder.miningRewardAdjustmentView.visibility =
            if (listItem.isMiningRewardHalvingPoint) View.VISIBLE else View.GONE
        holder.miningDifficultyAdjustmentView.visibility =
            if (listItem.isDifficultyTransitionPoint) View.VISIBLE else View.GONE
        holder.hashView.text = WalletUtils.formatHash(null, listItem.blockHash.toString(), 8, 0, ' ', true)

        val transactionChildCount = holder.transactionsViewGroup.childCount - ROW_BASE_CHILD_COUNT
        var iTransactionView = 0
        for (tx in listItem.transactions) {
            val view: View
            if (iTransactionView < transactionChildCount) {
                view = holder.transactionsViewGroup.getChildAt(ROW_INSERT_INDEX + iTransactionView)
            } else {
                view = inflater.inflate(R.layout.item_wallet_block_transaction, holder.transactionsViewGroup, false)
                holder.transactionsViewGroup.addView(view, ROW_INSERT_INDEX + iTransactionView)
            }
            bindTransactionView(view, listItem.format, tx)
            iTransactionView++
        }
        val leftoverTransactionViews = transactionChildCount - iTransactionView
        if (leftoverTransactionViews > 0)
            holder.transactionsViewGroup.removeViews(ROW_INSERT_INDEX + iTransactionView, leftoverTransactionViews)

        val onClickListener = this.onClickListener
        if (onClickListener != null) {
            holder.itemView.setOnClickListener { v -> onClickListener.onBlockMenuClick(v, listItem.blockHash) }
        }
    }

    private fun bindTransactionView(row: View, format: MonetaryFormat, tx: ListItem.ListTransaction) {
        // receiving or sending
        val rowFromTo = row.findViewById<View>(R.id.block_row_transaction_fromto) as TextView
        rowFromTo.text = tx.fromTo

        // address
        val rowAddress = row.findViewById<View>(R.id.block_row_transaction_address) as TextView
        rowAddress.text = tx.label ?: tx.address!!.toString()
        rowAddress.typeface = if (tx.label != null) Typeface.DEFAULT else Typeface.MONOSPACE

        // value
        val rowValue = row.findViewById<View>(R.id.block_row_transaction_value) as CurrencyTextView
        rowValue.setAlwaysSigned(true)
        rowValue.setFormat(format)
        rowValue.setAmount(tx.value)
    }

    interface OnClickListener {
        fun onBlockMenuClick(view: View, blockHash: Sha256Hash)
    }

    class ViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val transactionsViewGroup: ViewGroup =
            itemView.findViewById<View>(R.id.block_list_row_transactions_group) as ViewGroup
        val miningRewardAdjustmentView: View = itemView.findViewById(R.id.block_list_row_mining_reward_adjustment)
        val miningDifficultyAdjustmentView: View =
            itemView.findViewById(R.id.block_list_row_mining_difficulty_adjustment)
        val heightView: TextView = itemView.findViewById<View>(R.id.block_list_row_height) as TextView
        val timeView: TextView = itemView.findViewById<View>(R.id.block_list_row_time) as TextView
        val hashView: TextView = itemView.findViewById<View>(R.id.block_list_row_hash) as TextView
        val menuView: ImageButton = itemView.findViewById<View>(R.id.block_list_row_menu) as ImageButton

    }

    companion object {
        fun buildListItems(
            context: Context, blocks: List<StoredBlock>, time: Date,
            format: MonetaryFormat, transactions: Set<Transaction>?, wallet: Wallet?,
            addressBook: Map<String, AddressBookEntry>?
        ): List<ListItem> {
            val items = ArrayList<ListItem>(blocks.size)
            for (block in blocks)
                items.add(ListItem(context, block, time, format, transactions, wallet, addressBook))
            return items
        }

        private const val ROW_BASE_CHILD_COUNT = 2
        private const val ROW_INSERT_INDEX = 1
    }
}