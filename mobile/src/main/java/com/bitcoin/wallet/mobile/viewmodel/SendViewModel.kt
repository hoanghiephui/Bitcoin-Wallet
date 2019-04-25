package com.bitcoin.wallet.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.base.BaseViewModel
import com.bitcoin.wallet.mobile.data.*
import com.bitcoin.wallet.mobile.data.live.*
import com.bitcoin.wallet.mobile.model.PriceDatum
import com.bitcoin.wallet.mobile.repository.NetworkState
import com.bitcoin.wallet.mobile.repository.WalletRepository
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import javax.inject.Inject

class SendViewModel @Inject constructor(
    application: Application,
    repository: WalletRepository
) : BaseViewModel<WalletRepository>(repository) {
    //get list price
    private val priceRequestData = MutableLiveData<String>()
    private val zipResult = Transformations.map(priceRequestData) {
        repository.onGetPrice(
            base = it,
            apiKey = ""
        )
    }
    val priceList: LiveData<Map<String, PriceDatum>> = Transformations.switchMap(zipResult) { it.data }
    val priceNetworkState: LiveData<NetworkState> = Transformations.switchMap(zipResult) { it.networkState }

    fun onGetPrice(request: String) {
        priceRequestData.postValue(request)
    }


    enum class State {
        REQUEST_PAYMENT_REQUEST, //
        INPUT, // asks for confirmation
        DECRYPTING, SIGNING, SENDING, SENT, FAILED // sending states
    }
    val wallet: WalletLiveData by lazy {
        WalletLiveData(application as BitcoinApplication)
    }
    val addressBook: LiveData<List<AddressBookEntry>> by lazy {
        AppDatabase.getDatabase(application).addressBookDao().all
    }
    val exchangeRate: SelectedExchangeRateLiveData by lazy {
        SelectedExchangeRateLiveData(application as BitcoinApplication)
    }
    val dynamicFees: DynamicFeeLiveData by lazy {
        DynamicFeeLiveData(application as BitcoinApplication)
    }
    val blockchainState: BlockchainStateLiveData by lazy {
        BlockchainStateLiveData(application as BitcoinApplication)
    }
    val balance: WalletBalanceLiveData by lazy {
        WalletBalanceLiveData(application as BitcoinApplication, Wallet.BalanceType.AVAILABLE)
    }
    val progress = MutableLiveData<String>()

    var state: State? = null
    var paymentIntent: PaymentIntent? = null
    var feeCategory = FeeCategory.NORMAL
    var validatedAddress: AddressAndLabel? = null
    var sentTransaction: Transaction? = null
    var dryrunTransaction: Transaction? = null
    var dryrunException: Exception? = null
}