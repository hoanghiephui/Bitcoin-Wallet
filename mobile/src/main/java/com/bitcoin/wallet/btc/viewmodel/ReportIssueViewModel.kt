package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.live.WalletLiveData
import javax.inject.Inject

class ReportIssueViewModel @Inject constructor(private val application: Application) : ViewModel() {
    val wallet by lazy {
        WalletLiveData(application as BitcoinApplication)
    }
}