package com.bitcoin.wallet.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.data.live.WalletLiveData
import javax.inject.Inject

class BackupWalletViewModel @Inject constructor(val application: Application): ViewModel() {
    val wallet: WalletLiveData by lazy {
        WalletLiveData(application as BitcoinApplication)
    }
    val password = MutableLiveData<String>()
}