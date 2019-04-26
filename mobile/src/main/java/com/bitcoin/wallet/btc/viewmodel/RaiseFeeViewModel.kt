package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.live.DynamicFeeLiveData
import javax.inject.Inject

class RaiseFeeViewModel @Inject constructor(application: Application) : ViewModel() {
    val dynamicFees: DynamicFeeLiveData by lazy {
        DynamicFeeLiveData(application as BitcoinApplication)
    }
}