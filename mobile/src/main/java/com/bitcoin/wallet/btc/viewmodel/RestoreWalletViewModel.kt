package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.live.WalletBalanceLiveData
import javax.inject.Inject

class RestoreWalletViewModel @Inject constructor(application: Application): AndroidViewModel(application) {
    val balance: WalletBalanceLiveData by lazy {
        WalletBalanceLiveData(application as BitcoinApplication)
    }
}