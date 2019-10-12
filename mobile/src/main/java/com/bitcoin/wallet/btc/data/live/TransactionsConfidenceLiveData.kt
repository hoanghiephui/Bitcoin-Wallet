package com.bitcoin.wallet.btc.data.live

import com.bitcoin.wallet.btc.BitcoinApplication
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.Wallet

class TransactionsConfidenceLiveData(application: BitcoinApplication) :
    BaseWalletLiveData<Void>(application),
    TransactionConfidenceEventListener {

    override fun onWalletActive(wallet: Wallet?) {
        wallet?.addTransactionConfidenceEventListener(Threading.SAME_THREAD, this)
    }

    override fun onWalletInactive(wallet: Wallet?) {
        wallet?.removeTransactionConfidenceEventListener(this)
    }

    override fun onTransactionConfidenceChanged(wallet: Wallet, tx: Transaction) {
        triggerLoad()
    }

    override fun load() {
        postValue(null)
    }
}