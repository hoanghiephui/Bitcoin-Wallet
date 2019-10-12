package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.data.AddressBookEntry
import com.bitcoin.wallet.btc.data.AppDatabase
import com.bitcoin.wallet.btc.data.live.ConfigFormatLiveData
import com.bitcoin.wallet.btc.data.live.TransactionsConfidenceLiveData
import com.bitcoin.wallet.btc.data.live.TransactionsLiveData
import com.bitcoin.wallet.btc.data.live.WalletLiveData
import com.bitcoin.wallet.btc.ui.adapter.ListItem
import com.bitcoin.wallet.btc.ui.adapter.TransactionsWalletAdapter
import com.bitcoin.wallet.btc.ui.adapter.WarningType
import com.bitcoin.wallet.btc.ui.fragments.BackupDialog
import com.bitcoin.wallet.btc.utils.Event
import org.bitcoinj.core.Address
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionConfidence
import java.util.*
import javax.inject.Inject
import kotlin.Comparator

class WalletTransactionsViewModel @Inject constructor(private val application: Application) :
    ViewModel() {
    enum class Direction {
        RECEIVED, SENT
    }

    val transactions: TransactionsLiveData by lazy {
        TransactionsLiveData(application as BitcoinApplication)
    }

    val wallet: WalletLiveData by lazy {
        WalletLiveData(application as BitcoinApplication)
    }

    val transactionsConfidence: TransactionsConfidenceLiveData by lazy {
        TransactionsConfidenceLiveData(application as BitcoinApplication)
    }

    val addressBook: LiveData<List<AddressBookEntry>> by lazy {
        AppDatabase.getDatabase(application).addressBookDao().all
    }

    val configFormat: ConfigFormatLiveData by lazy {
        ConfigFormatLiveData(application as BitcoinApplication)
    }

    val direction = MutableLiveData<Direction>()
    private val selectedTransaction = MutableLiveData<Sha256Hash>()
    val warning = MutableLiveData<WarningType>()
    val list = MediatorLiveData<List<ListItem>>()
    val showBitmapDialog = MutableLiveData<Event<Bitmap>>()
    val showEditAddressBookEntryDialog = MutableLiveData<Event<Address>>()
    val showReportIssueDialog = MutableLiveData<Event<String>>()
    val showBackupWalletDialog = MutableLiveData<Event<Void>>()

    init {
        this.list.addSource(
            transactions
        ) { maybePostList() }
        this.list.addSource(wallet) { maybePostList() }
        this.list.addSource(transactionsConfidence) { maybePostList() }
        this.list.addSource(
            addressBook
        ) { maybePostList() }
        this.list.addSource(
            direction
        ) { maybePostList() }
        this.list.addSource(
            selectedTransaction
        ) { maybePostList() }
        this.list.addSource(
            configFormat
        ) { maybePostList() }
    }

    fun setDirection(direction: Direction?) {
        this.direction.value = direction
    }

    fun setSelectedTransaction(selectedTransaction: Sha256Hash) {
        this.selectedTransaction.value = selectedTransaction
    }

    fun setWarning(warning: WarningType?) {
        this.warning.value = warning
    }

    val backupWalletStatus = MutableLiveData<Event<BackupDialog.BackUpStatus>>()

    private fun maybePostList() {
        AsyncTask.execute {
            org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
            val transactions = this@WalletTransactionsViewModel.transactions.value
            val format = configFormat.value
            val addressBook = AddressBookEntry
                .asMap(this@WalletTransactionsViewModel.addressBook.value)
            if (transactions != null && format != null && addressBook != null) {
                val filteredTransactions = ArrayList<Transaction>(transactions.size)
                val wallet = (application as BitcoinApplication).getWallet()
                val direction = this@WalletTransactionsViewModel.direction.value
                for (tx in transactions) {
                    val sent = tx.getValue(wallet).signum() < 0
                    val isInternal = tx.purpose == Transaction.Purpose.KEY_ROTATION
                    if (direction == Direction.RECEIVED && !sent && !isInternal || direction == null
                        || direction == Direction.SENT && sent && !isInternal
                    )
                        filteredTransactions.add(tx)
                }

                Collections.sort(filteredTransactions, comparator)

                list.postValue(
                    TransactionsWalletAdapter.buildListItems(
                        application,
                        filteredTransactions,
                        warning.value,
                        wallet,
                        addressBook,
                        format,
                        application.maxConnectedPeers(),
                        selectedTransaction.value
                    )
                )
            }
        }
    }

    private val comparator = Comparator<Transaction> { tx1, tx2 ->
        val pending1 = tx1.confidence.confidenceType == TransactionConfidence.ConfidenceType.PENDING
        val pending2 = tx2.confidence.confidenceType == TransactionConfidence.ConfidenceType.PENDING
        if (pending1 != pending2)
            return@Comparator if (pending1) -1 else 1

        val updateTime1 = tx1.updateTime
        val time1 = updateTime1?.time ?: 0
        val updateTime2 = tx2.updateTime
        val time2 = updateTime2?.time ?: 0
        if (time1 != time2) if (time1 > time2) -1 else 1 else tx1.txId.compareTo(tx2.txId)
    }
}