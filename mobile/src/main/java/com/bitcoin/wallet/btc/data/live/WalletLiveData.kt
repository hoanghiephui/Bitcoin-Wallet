package com.bitcoin.wallet.btc.data.live

import com.bitcoin.wallet.btc.BitcoinApplication
import org.bitcoinj.wallet.Wallet

class WalletLiveData(application: BitcoinApplication) : BaseWalletLiveData<Wallet>(application, 0) {

    override fun onWalletActive(wallet: Wallet?) {
        postValue(wallet)
    }
}