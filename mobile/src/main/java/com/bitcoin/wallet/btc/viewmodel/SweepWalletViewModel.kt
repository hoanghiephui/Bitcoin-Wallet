package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.live.DynamicFeeLiveData
import org.bitcoinj.core.PrefixedChecksummedBytes
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import javax.inject.Inject

class SweepWalletViewModel @Inject constructor(private val application: Application): ViewModel() {
    enum class State {
        DECODE_KEY, // ask for password
        CONFIRM_SWEEP, // displays balance and asks for confirmation
        PREPARATION, SENDING, SENT, FAILED // sending states
    }
    val dynamicFees by lazy {
        DynamicFeeLiveData(application as BitcoinApplication)
    }
    val progress = MutableLiveData<String>()

    var state = State.DECODE_KEY
    var privateKeyToSweep: PrefixedChecksummedBytes? = null
    var walletToSweep: Wallet? = null
    var sentTransaction: Transaction? = null
}