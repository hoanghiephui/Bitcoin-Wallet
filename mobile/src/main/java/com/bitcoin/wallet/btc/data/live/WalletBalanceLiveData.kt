package com.bitcoin.wallet.btc.data.live

import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.utils.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Context
import org.bitcoinj.core.Transaction
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.listeners.WalletChangeEventListener
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener

class WalletBalanceLiveData @JvmOverloads constructor(
    application: BitcoinApplication,
    private val viewModelScope: CoroutineScope,
    private val balanceType: Wallet.BalanceType = Wallet.BalanceType.ESTIMATED
) : BaseWalletLiveData<Coin>(application), SharedPreferences.OnSharedPreferenceChangeListener {
    private val config: Configuration = application.config
    private val walletListener = WalletListener()

    override fun onWalletActive(wallet: Wallet?) {
        addWalletListener(wallet!!)
        config.registerOnSharedPreferenceChangeListener(this)
        load()
    }

    override fun onWalletInactive(wallet: Wallet?) {
        config.unregisterOnSharedPreferenceChangeListener(this)
        removeWalletListener(wallet!!)
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
        viewModelScope.launch {
            loadBlance()
        }
        /*AsyncTask.execute {
            org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
            postValue(wallet?.getBalance(balanceType))
        }*/
    }

    @WorkerThread
    suspend fun loadBlance() = withContext(Dispatchers.IO) {
        Context.propagate(Constants.CONTEXT)
        postValue(wallet?.getBalance(balanceType))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (Configuration.PREFS_KEY_BTC_PRECISION == key)
            load()
    }

    private inner class WalletListener : WalletCoinsReceivedEventListener,
        WalletCoinsSentEventListener,
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
