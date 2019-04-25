package com.bitcoin.wallet.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.data.live.BlockchainStateLiveData
import com.bitcoin.wallet.mobile.data.live.ExchangeRatesLiveData
import com.bitcoin.wallet.mobile.data.live.WalletBalanceLiveData
import javax.inject.Inject

class ExchangeRatesViewModel @Inject constructor(application: Application) : ViewModel() {
    val exchangeRates: ExchangeRatesLiveData by lazy {
        ExchangeRatesLiveData(application as BitcoinApplication)
    }
    val balance: WalletBalanceLiveData by lazy {
        WalletBalanceLiveData(application as BitcoinApplication)
    }
    val blockchainState: BlockchainStateLiveData by lazy {
        BlockchainStateLiveData(application as BitcoinApplication)
    }

    var query: String? = null
}