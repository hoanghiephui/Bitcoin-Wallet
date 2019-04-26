package com.bitcoin.wallet.btc.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.base.BaseViewModel
import com.bitcoin.wallet.btc.data.live.*
import com.bitcoin.wallet.btc.model.StatsResponse
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.repository.WalletRepository
import com.bitcoin.wallet.btc.utils.Event
import com.bitcoin.wallet.btc.utils.Qr
import io.reactivex.disposables.CompositeDisposable
import org.bitcoinj.core.Address
import org.bitcoinj.uri.BitcoinURI
import javax.inject.Inject

class WalletViewModel @Inject constructor(
    repository: WalletRepository,
    private val application: Application
) : BaseViewModel<WalletRepository>(repository) {

    //get stats
    private val statsRequestData = MutableLiveData<Boolean>()
    private val disposable = CompositeDisposable()
    private val statsResult = Transformations.map(statsRequestData) {
        repository.getStats(disposable)
    }
    val statsData: LiveData<StatsResponse> = Transformations.switchMap(statsResult) { it.data }
    val statNetworkState: LiveData<NetworkState> = Transformations.switchMap(statsResult) { it.networkState }

    fun onGetStats(request: Boolean) {
        disposable.clear()
        statsRequestData.postValue(request)
    }


    //get list zip chart price
    private val zipRequestChart = MutableLiveData<HashMap<String, String>>()
    private val zipChartResult = Transformations.map(zipRequestChart) {
        repository.onGetZipDataChartPrice(
            base = it["base"]!!,
            quote = it["quote"]!!,
            start = it["start"]?.toLong()!!,
            scale = it["scale"]?.toInt()!!,
            apiKey = it["api_key"]!!
        )
    }
    val zipChart: LiveData<WalletRepository.ZipPriceChart> = Transformations.switchMap(zipChartResult) { it.data }
    val chartNetworkState: LiveData<NetworkState> = Transformations.switchMap(zipChartResult) { it.networkState }

    fun onGetZipDataChart(request: HashMap<String, String>) {
        zipRequestChart.postValue(request)
    }

    /**
     * @method retry get zip data chart
     */
    fun retryZipChart() {
        zipChartResult?.value?.retry?.invoke()
    }

    ////////////////////////////WALLET////////////////////////////
    val showHelpDialog = MutableLiveData<Event<Int>>()
    val showBackupWalletDialog = MutableLiveData<Event<Void>>()
    val showRestoreWalletDialog = MutableLiveData<Event<Void>>()
    val showEncryptKeysDialog = MutableLiveData<Event<Void>>()
    val showReportIssueDialog = MutableLiveData<Event<Void>>()
    val sendBitcoin = MutableLiveData<Event<Void>>()
    val requestBitcoin = MutableLiveData<Event<Void>>()
    val scanAddress = MutableLiveData<Event<String>>()
    val backupWalletStatus = MutableLiveData<Event<BackUpStatus>>()
    val walletLegacyFallback: WalletLegacyFallbackLiveData by lazy {
        WalletLegacyFallbackLiveData(application as BitcoinApplication)
    }
    val chartData = MutableLiveData<Event<Void>>()


    val balance: WalletBalanceLiveData by lazy {
        WalletBalanceLiveData(application as BitcoinApplication)
    }

    val exchangeRate: SelectedExchangeRateLiveData by lazy {
        SelectedExchangeRateLiveData(application as BitcoinApplication)
    }

    val blockchainState: BlockchainStateLiveData by lazy {
        BlockchainStateLiveData(application as BitcoinApplication)
    }

    val currentAddress: CurrentAddressLiveData by lazy {
        CurrentAddressLiveData(application as BitcoinApplication)
    }

    val ownName: ConfigOwnNameLiveData by lazy {
        ConfigOwnNameLiveData(application as BitcoinApplication)
    }
    val disclaimerEnabled: DisclaimerEnabledLiveData by lazy {
        DisclaimerEnabledLiveData(application as BitcoinApplication)
    }
    val qrCode = MediatorLiveData<Bitmap>()
    val bitcoinUri = MediatorLiveData<Uri>()
    val showWalletAddressDialog = MutableLiveData<Event<Void>>()

    init {
        this.qrCode.addSource(currentAddress) { maybeGenerateQrCode() }
        this.qrCode.addSource(ownName) { maybeGenerateQrCode() }
        this.bitcoinUri.addSource(currentAddress) { maybeGenerateBitcoinUri() }
        this.bitcoinUri.addSource(ownName) { maybeGenerateBitcoinUri() }
    }

    private fun maybeGenerateQrCode() {
        val address = currentAddress.value
        if (address != null) {
            AsyncTask.execute { qrCode.postValue(Qr.bitmap(uri(address, ownName.value))) }
        }
    }

    private fun maybeGenerateBitcoinUri() {
        val address = currentAddress.value
        if (address != null) {
            bitcoinUri.value = Uri.parse(uri(address, ownName.value))
        }
    }

    private fun uri(address: Address?, label: String?): String {
        return BitcoinURI.convertToBitcoinURI(address!!, null, label, null)
    }

    data class BackUpStatus(
        val isStatus: Boolean,
        val mes: String
    )

    override fun onCleared() {
        disposable.clear()
        super.onCleared()
    }
}