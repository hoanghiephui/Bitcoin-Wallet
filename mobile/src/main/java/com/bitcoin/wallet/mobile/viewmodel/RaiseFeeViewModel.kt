package com.bitcoin.wallet.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.data.live.DynamicFeeLiveData
import javax.inject.Inject

class RaiseFeeViewModel @Inject constructor(application: Application) : ViewModel() {
    val dynamicFees: DynamicFeeLiveData by lazy {
        DynamicFeeLiveData(application as BitcoinApplication)
    }
}