package com.bitcoin.wallet.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.data.AddressBookEntry
import com.bitcoin.wallet.mobile.data.AppDatabase
import com.bitcoin.wallet.mobile.data.live.*
import javax.inject.Inject

class NetworkViewModel @Inject constructor(val application: Application) : ViewModel() {
    val blocks: BlocksLiveData by lazy {
        BlocksLiveData(application as BitcoinApplication)
    }
    val transactions: TransactionsNetworkLiveData by lazy {
        TransactionsNetworkLiveData(application as BitcoinApplication)
    }
    val wallet: WalletLiveData by lazy {
        WalletLiveData(application as BitcoinApplication)
    }
    val time: TimeLiveData by lazy {
        TimeLiveData(application as BitcoinApplication)
    }
    val addressBook: LiveData<List<AddressBookEntry>> by lazy {
        AppDatabase.getDatabase(this.application).addressBookDao().all
    }

    val peers: PeersLiveData by lazy {
        PeersLiveData(application as BitcoinApplication)
    }
    val hostnames: HostNameLiveData by lazy {
        HostNameLiveData()
    }
}