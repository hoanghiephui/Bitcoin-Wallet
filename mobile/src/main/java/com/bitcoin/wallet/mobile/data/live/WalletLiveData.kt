package com.bitcoin.wallet.mobile.data.live

import com.bitcoin.wallet.mobile.BitcoinApplication
import org.bitcoinj.wallet.Wallet

class WalletLiveData(application: BitcoinApplication) : BaseWalletLiveData<Wallet>(application, 0) {

    override fun onWalletActive(wallet: Wallet?) {
        postValue(wallet)
    }
}