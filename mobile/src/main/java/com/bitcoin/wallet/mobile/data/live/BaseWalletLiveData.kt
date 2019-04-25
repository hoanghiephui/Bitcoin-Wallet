package com.bitcoin.wallet.mobile.data.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bitcoin.wallet.mobile.BitcoinApplication
import org.bitcoinj.wallet.Wallet

abstract class BaseWalletLiveData<T> : ThrottelingLiveData<T> {
    private val application: BitcoinApplication
    private val broadcastManager: LocalBroadcastManager
    private val handler = Handler()
    protected var wallet: Wallet? = null
        private set

    private val onWalletLoadedListener = object : BitcoinApplication.OnWalletLoadedListener {
        override fun onWalletLoaded(wallet: Wallet?) {
            handler.post {
                this@BaseWalletLiveData.wallet = wallet
                onWalletActive(wallet)
            }
        }
    }

    private val walletReferenceChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (wallet != null)
                onWalletInactive(wallet)
            loadWallet()
        }
    }

    constructor(application: BitcoinApplication) : super() {
        this.application = application
        this.broadcastManager = LocalBroadcastManager.getInstance(application)
    }

    constructor(application: BitcoinApplication, throttleMs: Long) : super(throttleMs) {
        this.application = application
        this.broadcastManager = LocalBroadcastManager.getInstance(application)
    }

    override fun onActive() {
        broadcastManager.registerReceiver(
            walletReferenceChangeReceiver,
            IntentFilter(BitcoinApplication.ACTION_WALLET_REFERENCE_CHANGED)
        )
        loadWallet()
    }

    override fun onInactive() {
        if (wallet != null)
            onWalletInactive(wallet)
        broadcastManager.unregisterReceiver(walletReferenceChangeReceiver)
    }

    private fun loadWallet() {
        application.getWalletAsync(onWalletLoadedListener)
    }

    protected abstract fun onWalletActive(wallet: Wallet?)

    protected open fun onWalletInactive(wallet: Wallet?) {}
}
