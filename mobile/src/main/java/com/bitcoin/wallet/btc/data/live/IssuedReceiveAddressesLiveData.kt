package com.bitcoin.wallet.btc.data.live

import android.os.AsyncTask
import com.bitcoin.wallet.btc.BitcoinApplication
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.listeners.KeyChainEventListener

class IssuedReceiveAddressesLiveData(application: BitcoinApplication) :
    BaseWalletLiveData<List<Address>>(application), KeyChainEventListener {

    override fun onWalletActive(wallet: Wallet?) {
        wallet?.addKeyChainEventListener(Threading.SAME_THREAD, this)
        loadAddresses()
    }

    override fun onWalletInactive(wallet: Wallet?) {
        wallet?.removeKeyChainEventListener(this)
    }

    override fun onKeysAdded(keys: List<ECKey>) {
        loadAddresses()
    }

    private fun loadAddresses() {
        val wallet = wallet
        AsyncTask.execute { postValue(wallet?.issuedReceiveAddresses) }
    }
}