package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.live.WalletBalanceLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

class RestoreWalletViewModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {
    private val viewModelScope = CoroutineScope(Job() + Dispatchers.Main)
    val balance: WalletBalanceLiveData by lazy {
        WalletBalanceLiveData(application as BitcoinApplication, viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancel()
    }
}