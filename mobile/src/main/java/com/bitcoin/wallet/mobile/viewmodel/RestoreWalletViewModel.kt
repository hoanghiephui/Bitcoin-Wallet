package com.bitcoin.wallet.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.data.live.WalletBalanceLiveData
import com.bitcoin.wallet.mobile.utils.Event
import javax.inject.Inject

class RestoreWalletViewModel @Inject constructor(application: Application): AndroidViewModel(application) {
    val balance: WalletBalanceLiveData by lazy {
        WalletBalanceLiveData(application as BitcoinApplication)
    }
}