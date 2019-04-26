package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.AddressBookEntry
import com.bitcoin.wallet.btc.data.AppDatabase
import com.bitcoin.wallet.btc.data.live.*
import com.bitcoin.wallet.btc.utils.Event
import org.bitcoinj.core.Address
import javax.inject.Inject

class WalletAddressViewModel @Inject constructor(application: Application) : ViewModel() {
    val issuedReceiveAddresses: IssuedReceiveAddressesLiveData by lazy {
        IssuedReceiveAddressesLiveData(application = application as BitcoinApplication)
    }
    val importedAddresses: ImportedAddressLiveData by lazy {
        ImportedAddressLiveData(application as BitcoinApplication)
    }
    val addressBook: LiveData<List<AddressBookEntry>> by lazy {
        AppDatabase.getDatabase(application).addressBookDao().all
    }
    val wallet: WalletLiveData by lazy {
        WalletLiveData(application as BitcoinApplication)
    }
    val ownName: ConfigOwnNameLiveData by lazy {
        ConfigOwnNameLiveData(application as BitcoinApplication)
    }
    val showBitmapDialog = MutableLiveData<Event<Bitmap>>()
    val showEditAddressBookEntryDialog = MutableLiveData<Event<Address>>()

    val addressToExclude: AddressToExcludeLiveData by lazy {
        AddressToExcludeLiveData(application = application as BitcoinApplication)
    }
    var addressBookSend: LiveData<List<AddressBookEntry>>? = null
    val clip: ClipLiveData by lazy {
        ClipLiveData(application = application as BitcoinApplication)
    }
}