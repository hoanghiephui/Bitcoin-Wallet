package com.bitcoin.wallet.btc.data.live

import android.os.AsyncTask
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import java.util.*

class TransactionsNetworkLiveData constructor(application: BitcoinApplication) :
    BaseWalletLiveData<Set<Transaction>>(application) {

    override fun onWalletActive(wallet: Wallet?) {
        loadTransactions()
    }

    private fun loadTransactions() {
        val wallet = wallet ?: return
        AsyncTask.execute {
            org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
            val transactions = wallet.getTransactions(false)
            val filteredTransactions = HashSet<Transaction>(transactions.size)
            for (tx in transactions) {
                val appearsIn = tx.appearsInHashes
                if (appearsIn != null && !appearsIn.isEmpty())
                    filteredTransactions.add(tx)
            }
            postValue(filteredTransactions)
        }
    }
}