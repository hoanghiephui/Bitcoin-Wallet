package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.live.WalletEncryptedLiveData
import com.bitcoin.wallet.btc.ui.fragments.BackupDialog
import com.bitcoin.wallet.btc.utils.Event
import javax.inject.Inject

class MainViewModel @Inject constructor(application: Application) : ViewModel() {
    val backupWalletStatus = MutableLiveData<Event<BackupDialog.BackUpStatus>>()
    val showSuccessDialog = MutableLiveData<Event<Boolean>>()
    val showFailureDialog = MutableLiveData<Event<String>>()
    val walletEncrypted: WalletEncryptedLiveData by lazy {
        WalletEncryptedLiveData(application as BitcoinApplication)
    }
    val backupStatus = MutableLiveData<Event<Uri>>()
}