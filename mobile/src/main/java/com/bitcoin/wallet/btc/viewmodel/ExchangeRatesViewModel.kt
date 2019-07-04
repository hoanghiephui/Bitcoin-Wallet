package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.live.BlockchainStateLiveData
import com.bitcoin.wallet.btc.data.live.ExchangeRatesLiveData
import com.bitcoin.wallet.btc.data.live.WalletBalanceLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

class ExchangeRatesViewModel @Inject constructor(application: Application) : ViewModel() {
    private val viewModelScope = CoroutineScope(Job() + Dispatchers.Main)
    val exchangeRates: ExchangeRatesLiveData by lazy {
        ExchangeRatesLiveData(application as BitcoinApplication)
    }
    val balance: WalletBalanceLiveData by lazy {
        WalletBalanceLiveData(application as BitcoinApplication, viewModelScope)
    }
    val blockchainState: BlockchainStateLiveData by lazy {
        BlockchainStateLiveData(application as BitcoinApplication)
    }

    var query: String? = null

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
    }
}