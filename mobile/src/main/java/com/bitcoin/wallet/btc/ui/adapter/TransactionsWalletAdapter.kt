package com.bitcoin.wallet.btc.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import android.text.SpannedString
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.data.AddressBookEntry
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.extension.visible
import com.bitcoin.wallet.btc.ui.adapter.ListItem.TransactionItem
import com.bitcoin.wallet.btc.ui.adapter.TransactionsWalletAdapter.Companion.CONFIDENCE_SYMBOL_DEAD
import com.bitcoin.wallet.btc.ui.adapter.TransactionsWalletAdapter.Companion.CONFIDENCE_SYMBOL_IN_CONFLICT
import com.bitcoin.wallet.btc.ui.adapter.TransactionsWalletAdapter.Companion.CONFIDENCE_SYMBOL_UNKNOWN
import com.bitcoin.wallet.btc.utils.Formats
import com.bitcoin.wallet.btc.utils.WalletUtils
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_transaction.*
import kotlinx.android.synthetic.main.transaction_row_warning.*
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionConfidence
import org.bitcoinj.utils.Fiat
import org.bitcoinj.utils.MonetaryFormat
import org.bitcoinj.wallet.DefaultCoinSelector
import org.bitcoinj.wallet.Wallet
import java.util.*

class TransactionsWalletAdapter constructor(
    private val context: Context,
    private val onClickListener: OnClickListener?
) :
    ListAdapter<ListItem, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<ListItem>() {
            override fun areItemsTheSame(
                oldItem: ListItem,
                newItem: ListItem
            ): Boolean {
                return if (oldItem is TransactionItem) {
                    if (newItem !is TransactionItem) false else oldItem.transactionHash == newItem.transactionHash
                } else {
                    if (newItem !is ListItem.WarningItem) false else (oldItem as ListItem.WarningItem).type == newItem.type
                }
            }

            override fun areContentsTheSame(
                oldItem: ListItem,
                newItem: ListItem
            ): Boolean {
                if (oldItem is TransactionItem) {
                    val newTransactionItem = newItem as TransactionItem
                    if (oldItem.confidenceCircularProgress != newTransactionItem.confidenceCircularProgress)
                        return false
                    if (oldItem.confidenceCircularMaxProgress != newTransactionItem.confidenceCircularMaxProgress)
                        return false
                    if (oldItem.confidenceCircularSize != newTransactionItem.confidenceCircularSize)
                        return false
                    if (oldItem.confidenceCircularMaxSize != newTransactionItem.confidenceCircularMaxSize)
                        return false
                    if (oldItem.confidenceCircularFillColor != newTransactionItem.confidenceCircularFillColor)
                        return false
                    if (oldItem.confidenceCircularStrokeColor != newTransactionItem.confidenceCircularStrokeColor)
                        return false
                    if (oldItem.confidenceTextual != newTransactionItem.confidenceTextual)
                        return false
                    if (oldItem.confidenceTextualColor != newTransactionItem.confidenceTextualColor)
                        return false
                    if (oldItem.time != newTransactionItem.time)
                        return false
                    if (oldItem.timeColor != newTransactionItem.timeColor)
                        return false
                    if (oldItem.address != newTransactionItem.address)
                        return false
                    if (oldItem.addressColor != newTransactionItem.addressColor)
                        return false
                    if (oldItem.addressTypeface != newTransactionItem.addressTypeface)
                        return false
                    if (oldItem.addressSingleLine != newTransactionItem.addressSingleLine)
                        return false
                    if (oldItem.fee != newTransactionItem.fee)
                        return false
                    if (oldItem.feeFormat.format(Coin.COIN).toString() != newTransactionItem.feeFormat.format(Coin.COIN).toString())
                        return false
                    if (oldItem.value != newTransactionItem.value)
                        return false
                    if (oldItem.valueFormat.format(Coin.COIN).toString() != newTransactionItem.valueFormat.format(Coin.COIN).toString())
                        return false
                    if (oldItem.valueColor != newTransactionItem.valueColor)
                        return false
                    if (oldItem.fiat != newTransactionItem.fiat)
                        return false
                    if ((if (oldItem.fiatFormat != null)
                            oldItem.fiatFormat.format(Coin.COIN).toString()
                        else
                            null) != (if (newTransactionItem.fiatFormat != null)
                            newTransactionItem.fiatFormat.format(Coin.COIN).toString()
                        else
                            null)
                    )
                        return false
                    if (oldItem.fiatPrefixColor != newTransactionItem.fiatPrefixColor)
                        return false
                    if (oldItem.message != newTransactionItem.message)
                        return false
                    if (oldItem.messageColor != newTransactionItem.messageColor)
                        return false
                    return if (oldItem.messageSingleLine != newTransactionItem.messageSingleLine) false else oldItem.isSelected == newTransactionItem.isSelected
                } else {
                    return true
                }
            }

            override fun getChangePayload(
                oldItem: ListItem,
                newItem: ListItem
            ): Any? {
                val changes = EnumSet.noneOf(ChangeType::class.java)
                if (oldItem is TransactionItem) {
                    val newTransactionItem = newItem as TransactionItem
                    if (!(oldItem.confidenceCircularProgress == newTransactionItem.confidenceCircularProgress
                                && oldItem.confidenceCircularMaxProgress == newTransactionItem.confidenceCircularMaxProgress
                                && oldItem.confidenceCircularSize == newTransactionItem.confidenceCircularSize
                                && oldItem.confidenceCircularMaxSize == newTransactionItem.confidenceCircularMaxSize
                                && oldItem.confidenceCircularFillColor == newTransactionItem.confidenceCircularFillColor
                                && oldItem.confidenceCircularStrokeColor == newTransactionItem.confidenceCircularStrokeColor
                                && oldItem.confidenceTextual == newTransactionItem.confidenceTextual
                                && oldItem.confidenceTextualColor == newTransactionItem.confidenceTextualColor)
                    )
                        changes.add(ChangeType.CONFIDENCE)
                    if (!(oldItem.time == newTransactionItem.time && oldItem.timeColor == newTransactionItem.timeColor))
                        changes.add(ChangeType.TIME)
                    if (!(oldItem.address == newTransactionItem.address
                                && oldItem.addressColor == newTransactionItem.addressColor
                                && oldItem.addressTypeface == newTransactionItem.addressTypeface
                                && oldItem.addressSingleLine == newTransactionItem.addressSingleLine)
                    )
                        changes.add(ChangeType.ADDRESS)
                    if (!(oldItem.fee == newTransactionItem.fee && oldItem.feeFormat.format(Coin.COIN).toString() == newTransactionItem.feeFormat.format(
                            Coin.COIN
                        ).toString())
                    )
                        changes.add(ChangeType.FEE)
                    if (!(oldItem.value == newTransactionItem.value
                                && oldItem.valueFormat.format(Coin.COIN).toString() == newTransactionItem.valueFormat.format(
                            Coin.COIN
                        ).toString()
                                && oldItem.valueColor == newTransactionItem.valueColor)
                    )
                        changes.add(ChangeType.VALUE)
                    if (!(oldItem.fiat == newTransactionItem.fiat
                                && (if (oldItem.fiatFormat != null)
                            oldItem.fiatFormat.format(Coin.COIN).toString()
                        else
                            null) == (if (newTransactionItem.fiatFormat != null)
                            newTransactionItem.fiatFormat.format(Coin.COIN).toString()
                        else
                            null)
                                && oldItem.fiatPrefixColor == newTransactionItem.fiatPrefixColor)
                    )
                        changes.add(ChangeType.FIAT)
                    if (!(oldItem.message == newTransactionItem.message
                                && oldItem.messageColor == newTransactionItem.messageColor
                                && oldItem.messageSingleLine == newTransactionItem.messageSingleLine)
                    )
                        changes.add(ChangeType.MESSAGE)
                    if (oldItem.isSelected != newTransactionItem.isSelected)
                        changes.add(ChangeType.IS_SELECTED)
                }
                return changes
            }
        }
    ) {

    override fun getItemViewType(position: Int): Int {
        val listItem = getItem(position)
        return if (listItem is ListItem.WarningItem)
            VIEW_TYPE_WARNING
        else if (listItem is TransactionItem)
            VIEW_TYPE_TRANSACTION
        else
            throw IllegalStateException()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_TRANSACTION) {
            TransactionViewHolder(parent.inflate(R.layout.item_transaction))
        } else if (viewType == VIEW_TYPE_WARNING) {
            WarningViewHolder(parent.inflate(R.layout.transaction_row_warning))
        } else {
            throw IllegalStateException("unknown type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = getItem(position)
        if (holder is TransactionViewHolder) {
            val transactionItem = listItem as TransactionItem
            holder.itemView.isActivated = transactionItem.isSelected
            holder.bind(transactionItem)

            val onClickListener = this.onClickListener
            if (onClickListener != null) {
                holder.itemView.setOnClickListener { v ->
                    onClickListener.onTransactionClick(
                        v,
                        transactionItem.transactionHash
                    )
                }
                holder.menuView.setOnClickListener { v ->
                    onClickListener.onTransactionMenuClick(
                        v,
                        transactionItem.transactionHash
                    )
                }
            }
        } else if (holder is WarningViewHolder) {
            val warningItem = listItem as ListItem.WarningItem
            if (warningItem.type == WarningType.BACKUP) {
                if (itemCount == 2 /* 1 transaction, 1 warning */) {
                    holder.messageView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    holder.messageView.text = Html.fromHtml(context.getString(R.string.warning_backup))
                } else {
                    holder.messageView
                        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning_grey_600_24dp, 0, 0, 0)
                    holder.messageView.text = Html.fromHtml(context.getString(R.string.disclaimer_remind_backup))
                }
            } else if (warningItem.type == WarningType.STORAGE_ENCRYPTION) {
                holder.messageView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                holder.messageView.text = Html.fromHtml(context.getString(R.string.warning_storage_encryption))
            } else if (warningItem.type == WarningType.CHAIN_FORKING) {
                holder.messageView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_warning_grey_600_24dp, 0,
                    0, 0
                )
                holder.messageView.text = Html.fromHtml(context.getString(R.string.warning_chain_forking))
            }

            holder.itemView.setOnClickListener { onClickListener?.onWarningClick(it) }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) { // Full bind
            onBindViewHolder(holder, position)
        } else { // Partial bind
            val listItem = getItem(position)
            val transactionHolder = holder as TransactionViewHolder
            val transactionItem = listItem as TransactionItem
            for (payload in payloads) {
                val changes = payload as EnumSet<ChangeType>
                for (change in changes) {
                    if (change == ChangeType.CONFIDENCE)
                        transactionHolder.bindConfidence(transactionItem)
                    else if (change == ChangeType.TIME)
                        transactionHolder.bindTime(transactionItem)
                    else if (change == ChangeType.ADDRESS)
                        transactionHolder.bindAddress(transactionItem)
                    else if (change == ChangeType.FEE)
                        transactionHolder.bindFee(transactionItem)
                    else if (change == ChangeType.VALUE)
                        transactionHolder.bindValue(transactionItem)
                    else if (change == ChangeType.FIAT)
                        transactionHolder.bindFiat(transactionItem)
                    else if (change == ChangeType.MESSAGE)
                        transactionHolder.bindMessage(transactionItem)
                    else if (change == ChangeType.IS_SELECTED)
                        transactionHolder.bindIsSelected(transactionItem)
                }
            }
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
        val menuView: AppCompatImageButton = transaction_row_menu

        fun bind(item: TransactionItem) {
            bindConfidence(item)
            bindTime(item)
            bindAddress(item)
            bindFee(item)
            bindValue(item)
            bindFiat(item)
            bindMessage(item)
            bindIsSelected(item)
        }

        fun bindConfidence(item: TransactionItem) {
            imgType.setColorFilter(item.confidenceCircularFillColor)
            when (item.transaction.confidence.confidenceType) {
                TransactionConfidence.ConfidenceType.PENDING -> {
                    btnConfirm.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_pending)
                    btnConfirm.text = "PENDING"
                    btnConfirm.visible()
                }

                TransactionConfidence.ConfidenceType.BUILDING -> {
                    btnConfirm.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_confirm)
                    btnConfirm.text = "CONFIRMATIONS"
                    btnConfirm.visible()
                }
                else -> {
                    btnConfirm.gone()
                }
            }
        }

        fun bindTime(item: TransactionItem) {
            timeView.text = item.time
        }

        fun bindAddress(item: TransactionItem) {
            addressView.text = item.address
            addressView.setTextColor(item.addressColor)
            addressView.typeface = item.addressTypeface
            addressView.setSingleLine(item.addressSingleLine)
        }

        fun bindFee(item: TransactionItem) {
            viewFee.visibility = if (item.fee != null) View.VISIBLE else View.GONE
            feeView.setAlwaysSigned(true)
            feeView.setFormat(item.feeFormat)
            if (item.fee != null) {
                feeView.text = item.fee.toFriendlyString()
            }
        }

        fun bindValue(item: TransactionItem) {
            valueView.visibility = if (item.value != null) View.VISIBLE else View.GONE
            if (item.value != null) {
                valueView.text = item.value.toFriendlyString()
            }
            valueView.setTextColor(item.valueColor)
        }

        fun bindFiat(item: TransactionItem) {
            fiatView.visibility = if (item.fiat != null) View.VISIBLE else View.GONE
            if (item.fiat != null) {
                fiatView.text = item.fiat.toFriendlyString()
            }
            val value = item.isSend
            if (value) {
                imgType.setImageResource(R.drawable.ic_arrow_top_right_thick)
            } else {
                imgType.setImageResource(R.drawable.ic_arrow_bottom_left_thick)
            }
        }

        fun bindMessage(item: TransactionItem) {
            messageView.visibility = if (item.message != null) View.VISIBLE else View.GONE
            messageView.text = item.message
            messageView.setTextColor(item.messageColor)
            messageView.setSingleLine(item.messageSingleLine)
        }

        fun bindIsSelected(item: TransactionItem) {
            /*itemView.setBackgroundColor(
                if (item.isSelected) ContextCompat.getColor(
                    itemView.context,
                    R.color.bg_panel
                ) else ContextCompat.getColor(itemView.context, android.R.color.co)
            )*/
            bindConfidence(item)
            bindTime(item)
            bindAddress(item)
        }
    }

    class WarningViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
        val messageView: TextView = transaction_row_warning_message
    }

    class ItemAnimator : DefaultItemAnimator() {
        override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder, payloads: List<Any>): Boolean {
            for (payload in payloads) {
                val changes = payload as EnumSet<ChangeType>
                if (changes.contains(ChangeType.IS_SELECTED))
                    return false
            }
            return super.canReuseUpdatedViewHolder(viewHolder, payloads)
        }
    }

    companion object {
        fun buildListItems(
            context: Context, transactions: List<Transaction>,
            warning: WarningType?, wallet: Wallet?,
            addressBook: Map<String, AddressBookEntry>?, format: MonetaryFormat,
            maxConnectedPeers: Int, selectedTransaction: Sha256Hash?
        ): List<ListItem> {
            val noCodeFormat = format.noCode()
            val items = ArrayList<ListItem>(transactions.size + 1)
            if (warning != null)
                items.add(ListItem.WarningItem(warning))
            for (tx in transactions)
                items.add(
                    TransactionItem(
                        context, tx, wallet, addressBook, noCodeFormat, maxConnectedPeers,
                        tx.txId == selectedTransaction
                    )
                )
            return items
        }

        const val CONFIDENCE_SYMBOL_IN_CONFLICT = "\u26A0" // warning sign
        const val CONFIDENCE_SYMBOL_DEAD = "\u271D" // latin cross
        const val CONFIDENCE_SYMBOL_UNKNOWN = "?"

        const val VIEW_TYPE_TRANSACTION = 0
        const val VIEW_TYPE_WARNING = 1
    }
}

interface OnClickListener {
    fun onTransactionClick(view: View, transactionHash: Sha256Hash)

    fun onTransactionMenuClick(view: View, transactionHash: Sha256Hash)

    fun onWarningClick(view: View)
}

enum class WarningType {
    BACKUP, STORAGE_ENCRYPTION, CHAIN_FORKING
}

enum class ChangeType {
    CONFIDENCE, TIME, ADDRESS, FEE, VALUE, FIAT, MESSAGE, IS_SELECTED
}

open class ListItem {
    class TransactionItem(
        context: Context, tx: Transaction, wallet: Wallet?,
        addressBook: Map<String, AddressBookEntry>?, val feeFormat: MonetaryFormat,
        maxConnectedPeers: Int, val isSelected: Boolean
    ) : ListItem() {
        val transactionHash: Sha256Hash
        val confidenceCircularProgress: Int
        val confidenceCircularMaxProgress: Int
        val confidenceCircularSize: Int
        val confidenceCircularMaxSize: Int
        val confidenceCircularFillColor: Int
        val confidenceCircularStrokeColor: Int
        val confidenceTextual: String?
        val confidenceTextualColor: Int
        val time: CharSequence
        val timeColor: Int
        val address: Spanned?
        val addressColor: Int
        val addressTypeface: Typeface
        val addressSingleLine: Boolean
        val fee: Coin?
        val value: Coin?
        val valueFormat: MonetaryFormat
        val valueColor: Int
        val fiat: Fiat?
        val fiatFormat: MonetaryFormat?
        val fiatPrefixColor: Int
        val message: Spanned?
        val messageColor: Int
        val messageSingleLine: Boolean
        val transaction: Transaction
        var isSend = false

        init {
            this.transactionHash = tx.txId
            this.transaction = tx
            val res = context.resources
            val colorSignificant = res.getColor(R.color.fg_significant)
            val colorLessSignificant = res.getColor(R.color.fg_less_significant)
            val colorInsignificant = res.getColor(R.color.fg_insignificant)
            val colorValuePositve = res.getColor(R.color.fg_value_positive)
            val colorValueNegative = res.getColor(R.color.fg_value_negative)
            val colorError = res.getColor(R.color.fg_error)

            val value = tx.getValue(wallet)
            val sent = value.signum() < 0
            isSend = sent
            val self = WalletUtils.isEntirelySelf(tx, wallet)
            val confidence = tx.confidence
            val confidenceType = confidence.confidenceType
            val isOwn = confidence.source == TransactionConfidence.Source.SELF
            val purpose = tx.purpose
            val memo = Formats.sanitizeMemo(tx.memo)

            val textColor: Int
            val lessSignificantColor: Int
            val valueColor: Int
            if (confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
                textColor = colorError
                lessSignificantColor = colorError
                valueColor = colorError
            } else if (DefaultCoinSelector.isSelectable(tx)) {
                textColor = colorSignificant
                lessSignificantColor = colorLessSignificant
                valueColor = if (sent) colorValueNegative else colorValuePositve
            } else {
                textColor = colorInsignificant
                lessSignificantColor = colorInsignificant
                valueColor = colorInsignificant
            }

            // confidence
            if (confidenceType == TransactionConfidence.ConfidenceType.PENDING) {
                this.confidenceCircularMaxProgress = 1
                this.confidenceCircularProgress = 1
                this.confidenceCircularMaxSize = maxConnectedPeers / 2 // magic value
                this.confidenceCircularSize = confidence.numBroadcastPeers()
                this.confidenceCircularFillColor = colorInsignificant
                this.confidenceCircularStrokeColor = Color.TRANSPARENT
                this.confidenceTextual = null
                this.confidenceTextualColor = 0
            } else if (confidenceType == TransactionConfidence.ConfidenceType.IN_CONFLICT) {
                this.confidenceTextual = CONFIDENCE_SYMBOL_IN_CONFLICT
                this.confidenceTextualColor = colorError
                this.confidenceCircularMaxProgress = 0
                this.confidenceCircularProgress = 0
                this.confidenceCircularMaxSize = 0
                this.confidenceCircularSize = 0
                this.confidenceCircularFillColor = 0
                this.confidenceCircularStrokeColor = 0
            } else if (confidenceType == TransactionConfidence.ConfidenceType.BUILDING) {
                this.confidenceCircularMaxProgress = if (tx.isCoinBase)
                    Constants.NETWORK_PARAMETERS.spendableCoinbaseDepth
                else
                    Constants.MAX_NUM_CONFIRMATIONS
                this.confidenceCircularProgress = Math.min(
                    confidence.depthInBlocks,
                    this.confidenceCircularMaxProgress
                )
                this.confidenceCircularMaxSize = 1
                this.confidenceCircularSize = 1
                this.confidenceCircularFillColor = valueColor
                this.confidenceCircularStrokeColor = Color.TRANSPARENT
                this.confidenceTextual = null
                this.confidenceTextualColor = 0
            } else if (confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
                this.confidenceTextual = CONFIDENCE_SYMBOL_DEAD
                this.confidenceTextualColor = colorError
                this.confidenceCircularMaxProgress = 0
                this.confidenceCircularProgress = 0
                this.confidenceCircularMaxSize = 0
                this.confidenceCircularSize = 0
                this.confidenceCircularFillColor = 0
                this.confidenceCircularStrokeColor = 0
            } else {
                this.confidenceTextual = CONFIDENCE_SYMBOL_UNKNOWN
                this.confidenceTextualColor = colorInsignificant
                this.confidenceCircularMaxProgress = 0
                this.confidenceCircularProgress = 0
                this.confidenceCircularMaxSize = 0
                this.confidenceCircularSize = 0
                this.confidenceCircularFillColor = 0
                this.confidenceCircularStrokeColor = 0
            }

            // time
            val time = tx.updateTime
            this.time = if (isSelected)
                DateUtils.formatDateTime(
                    context, time.time,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                )
            else
                DateUtils.getRelativeTimeSpanString(context, time.time)
            this.timeColor = textColor

            // address
            val address = if (sent)
                WalletUtils.getToAddressOfSent(tx, wallet)
            else
                WalletUtils.getWalletAddressOfReceived(tx, wallet)
            val addressLabel: String?
            if (addressBook == null || address == null) {
                addressLabel = null
            } else {
                val entry = addressBook[address.toString()]
                if (entry != null)
                    addressLabel = entry.label
                else
                    addressLabel = null
            }
            if (tx.isCoinBase) {
                this.address = SpannedString
                    .valueOf(context.getString(R.string.min_coinbase))
                this.addressColor = textColor
                this.addressTypeface = Typeface.DEFAULT_BOLD
            } else if (purpose == Transaction.Purpose.RAISE_FEE) {
                this.address = null
                this.addressColor = 0
                this.addressTypeface = Typeface.DEFAULT
            } else if (purpose == Transaction.Purpose.KEY_ROTATION || self) {
                this.address = SpannedString.valueOf(
                    context.getString(R.string.symbol_internal) + " "
                            + context.getString(R.string.internal)
                )
                this.addressColor = lessSignificantColor
                this.addressTypeface = Typeface.DEFAULT_BOLD
            } else if (addressLabel != null) {
                this.address = SpannedString.valueOf(addressLabel)
                this.addressColor = textColor
                this.addressTypeface = Typeface.DEFAULT_BOLD
            } else if (memo != null && memo.size >= 2) {
                this.address = SpannedString.valueOf(memo[1])
                this.addressColor = textColor
                this.addressTypeface = Typeface.DEFAULT_BOLD
            } else if (address != null) {
                this.address = WalletUtils.formatAddress(
                    address, Constants.ADDRESS_FORMAT_GROUP_SIZE,
                    Constants.ADDRESS_FORMAT_LINE_SIZE, false
                )
                this.addressColor = lessSignificantColor
                this.addressTypeface = Typeface.DEFAULT
            } else {
                this.address = SpannedString.valueOf("?")
                this.addressColor = lessSignificantColor
                this.addressTypeface = Typeface.DEFAULT
            }
            this.addressSingleLine = !isSelected

            // fee
            val fee = tx.fee
            val showFee = sent && fee != null && !fee.isZero
            this.fee = if (isSelected && showFee) fee!!.negate() else null

            // value
            this.valueFormat = feeFormat
            if (purpose == Transaction.Purpose.RAISE_FEE) {
                this.valueColor = colorInsignificant
                this.value = fee!!.negate()
            } else if (value.isZero) {
                this.valueColor = 0
                this.value = null
            } else {
                this.valueColor = valueColor
                this.value = if (showFee) value.add(fee!!) else value
            }

            // fiat value
            val exchangeRate = tx.exchangeRate
            if (exchangeRate != null && !value.isZero) {
                this.fiat = exchangeRate.coinToFiat(value)
                this.fiatFormat = Constants.LOCAL_FORMAT.code(
                    0,
                    Constants.PREFIX_ALMOST_EQUAL_TO + exchangeRate.fiat.getCurrencyCode()
                )
                this.fiatPrefixColor = colorInsignificant
            } else {
                this.fiat = null
                this.fiatFormat = null
                this.fiatPrefixColor = 0
            }

            // message
            if (purpose == Transaction.Purpose.KEY_ROTATION) {
                this.message = Html
                    .fromHtml(context.getString(R.string.transaction_purpose))
                this.messageColor = colorSignificant
                this.messageSingleLine = false
            } else if (purpose == Transaction.Purpose.RAISE_FEE) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_purpose_raise_fee))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (isOwn && confidenceType == TransactionConfidence.ConfidenceType.PENDING && confidence.numBroadcastPeers() == 0) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_own_unbroadcasted))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (!isOwn && confidenceType == TransactionConfidence.ConfidenceType.PENDING && confidence.numBroadcastPeers() == 0) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_received_direct))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (!sent && value.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_received_dust))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (!sent && confidenceType == TransactionConfidence.ConfidenceType.PENDING
                && (tx.updateTime == null || wallet!!.lastBlockSeenTimeSecs * 1000 - tx.updateTime.time > Constants.DELAYED_TRANSACTION_THRESHOLD_MS)
            ) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_unconfirmed_delayed))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (!sent && confidenceType == TransactionConfidence.ConfidenceType.PENDING) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_unconfirmed_unlocked))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (!sent && confidenceType == TransactionConfidence.ConfidenceType.IN_CONFLICT) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_in_conflict))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (!sent && confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_received_dead))
                this.messageColor = colorError
                this.messageSingleLine = false
            } else if (!sent && WalletUtils.isPayToManyTransaction(tx)) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_received_pay_to_many))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (!sent && tx.isOptInFullRBF) {
                this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_received_rbf))
                this.messageColor = colorInsignificant
                this.messageSingleLine = false
            } else if (memo != null) {
                this.message = SpannedString.valueOf(memo[0])
                this.messageColor = colorInsignificant
                this.messageSingleLine = isSelected
            } else {
                this.message = null
                this.messageColor = 0
                this.messageSingleLine = false
            }
        }
    }

    class WarningItem(val type: WarningType) : ListItem()
}