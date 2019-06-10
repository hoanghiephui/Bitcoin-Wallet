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
import com.bitcoin.wallet.btc.CryptoCurrency
import com.bitcoin.wallet.btc.TimeSpan
import com.bitcoin.wallet.btc.api.ZipHomeData
import com.bitcoin.wallet.btc.base.BaseViewModel
import com.bitcoin.wallet.btc.data.live.*
import com.bitcoin.wallet.btc.model.blocks.BlocksResponse
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.repository.WalletRepository
import com.bitcoin.wallet.btc.utils.Event
import com.bitcoin.wallet.btc.utils.Qr
import org.bitcoinj.core.Address
import org.bitcoinj.uri.BitcoinURI
import javax.inject.Inject

class WalletViewModel @Inject constructor(
    repository: WalletRepository,
    private val application: Application
) : BaseViewModel<WalletRepository>(repository) {

    private val zipHomeRequest = MutableLiveData<RequestHome>()
    private val zipHomeData = Transformations.map(zipHomeRequest) {
        repository.getHomeData(
            baseId = it.baseId,
            base = it.base,
            period = it.resolution,
            urlInfo = it.urlInfo,
            urlNews = it.urlNews,
            urlSummary = it.urlSummary,
            cryptoCurrency = it.cryptoCurrency,
            fiatCurrency = it.fiatCurrency,
            timeSpan = it.timeSpan
        )
    }
    val zipHomeResult: LiveData<ZipHomeData> = Transformations.switchMap(zipHomeData) { it.data }
    val zipHomeNetworkState: LiveData<NetworkState> = Transformations.switchMap(zipHomeData) { it.networkState }
    fun onShowDataHome(requestHome: RequestHome) {
        zipHomeRequest.postValue(requestHome)
    }
    /**
     * @method retry get zip data chart
     */
    fun retryZipChart() {
        zipHomeData?.value?.retry?.invoke()
    }

    //get latest blocks
    private val blocksRequest = MutableLiveData<Event<Void>>()
    private val blocksData = Transformations.map(blocksRequest) {
        repository.getLatestBlocks()
    }
    val blockResult: LiveData<BlocksResponse> = Transformations.switchMap(blocksData) { it.data }
    val blockNetworkState: LiveData<NetworkState> = Transformations.switchMap(blocksData) { it.networkState }
    fun onGetLatestBlocks() {
        blocksRequest.postValue(Event.simple())
    }

    //retry latest blocks
    fun retryLatestBlocks() {
        blocksData?.value?.retry?.invoke()
    }

    ////////////////////////////WALLET////////////////////////////
    val showHelpDialog = MutableLiveData<Event<Int>>()
    val showBackupWalletDialog = MutableLiveData<Event<Void>>()
    val showRestoreWalletDialog = MutableLiveData<Event<Void>>()
    val showEncryptKeysDialog = MutableLiveData<Event<Void>>()
    val sendBitcoin = MutableLiveData<Event<Void>>()
    val requestBitcoin = MutableLiveData<Event<Void>>()
    val backupWalletStatus = MutableLiveData<Event<BackUpStatus>>()
    val walletLegacyFallback: WalletLegacyFallbackLiveData by lazy {
        WalletLegacyFallbackLiveData(application as BitcoinApplication)
    }


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

    data class RequestHome(
        val baseId: String,
        val base: String,
        val resolution: String,
        val urlInfo: String,
        val urlNews: String,
        val urlSummary: String,
        val cryptoCurrency: CryptoCurrency,
        val fiatCurrency: String,
        val timeSpan: TimeSpan
    )
}