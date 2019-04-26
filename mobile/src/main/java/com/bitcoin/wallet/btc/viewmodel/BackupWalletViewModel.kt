package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.live.WalletLiveData
import javax.inject.Inject

class BackupWalletViewModel @Inject constructor(val application: Application): ViewModel() {
    val wallet: WalletLiveData by lazy {
        WalletLiveData(application as BitcoinApplication)
    }
    val password = MutableLiveData<String>()
}