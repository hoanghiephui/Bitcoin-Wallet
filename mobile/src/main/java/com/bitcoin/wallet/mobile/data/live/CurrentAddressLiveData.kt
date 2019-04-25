package com.bitcoin.wallet.mobile.data.live

import android.os.AsyncTask
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.Constants
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.listeners.WalletChangeEventListener
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener

class CurrentAddressLiveData(application: BitcoinApplication) : BaseWalletLiveData<Address>(application) {

    private val walletListener = WalletListener()

    override fun onWalletActive(wallet: Wallet?) {
        wallet?.let { addWalletListener(it) }
        load()
    }

    override fun onWalletInactive(wallet: Wallet?) {
        wallet?.let { removeWalletListener(it) }
    }

    private fun addWalletListener(wallet: Wallet) {
        wallet.addCoinsReceivedEventListener(Threading.SAME_THREAD, walletListener)
        wallet.addCoinsSentEventListener(Threading.SAME_THREAD, walletListener)
        wallet.addReorganizeEventListener(Threading.SAME_THREAD, walletListener)
        wallet.addChangeEventListener(Threading.SAME_THREAD, walletListener)
    }

    private fun removeWalletListener(wallet: Wallet) {
        wallet.removeChangeEventListener(walletListener)
        wallet.removeReorganizeEventListener(walletListener)
        wallet.removeCoinsSentEventListener(walletListener)
        wallet.removeCoinsReceivedEventListener(walletListener)
    }

    override fun load() {
        AsyncTask.execute {
            org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
            postValue(wallet?.currentReceiveAddress())
        }
    }

    private inner class WalletListener : WalletCoinsReceivedEventListener, WalletCoinsSentEventListener,
        WalletReorganizeEventListener, WalletChangeEventListener {
        override fun onCoinsReceived(
            wallet: Wallet, tx: Transaction, prevBalance: Coin,
            newBalance: Coin
        ) {
            triggerLoad()
        }

        override fun onCoinsSent(
            wallet: Wallet, tx: Transaction, prevBalance: Coin,
            newBalance: Coin
        ) {
            triggerLoad()
        }

        override fun onReorganize(wallet: Wallet) {
            triggerLoad()
        }

        override fun onWalletChanged(wallet: Wallet) {
            triggerLoad()
        }
    }
}