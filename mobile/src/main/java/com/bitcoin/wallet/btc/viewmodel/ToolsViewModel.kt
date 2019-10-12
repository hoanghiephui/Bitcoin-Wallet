package com.bitcoin.wallet.btc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.bitcoin.wallet.btc.base.BaseViewModel
import com.bitcoin.wallet.btc.repository.ToolsRepository
import com.bitcoin.wallet.btc.utils.Event
import javax.inject.Inject

class ToolsViewModel @Inject constructor(repository: ToolsRepository) :
    BaseViewModel<ToolsRepository>(repository) {
    //get price coin
    private val priceCoinRequestData = MutableLiveData<Event<Void>>()
    private val priceCoinResult = Transformations.map(priceCoinRequestData) {
        repository.getPriceCoin()
    }
    val priceCoinData = Transformations.switchMap(priceCoinResult) { it.data }
    val networkState = Transformations.switchMap(priceCoinResult) { it.networkState }

    fun onGetPriceCoin() {
        priceCoinRequestData.postValue(Event.simple())
    }

    /**
     * @method retry get coins
     */
    fun retryPriceCoin() {
        priceCoinResult.value?.retry?.invoke()
    }

    //get history price
    private val historyCoinRequestData = MutableLiveData<String>()
    private val historyPriceCoinResult = Transformations.map(historyCoinRequestData) {
        repository.getLookupCoin("core", it)
    }
    val historyPriceCoinData = Transformations.switchMap(historyPriceCoinResult) { it.data }
    val historyNetworkState = Transformations.switchMap(historyPriceCoinResult) { it.networkState }

    fun onGetHistoryPriceCoin(time: String) {
        historyCoinRequestData.postValue(time)
    }

    /**
     * @method retry get history coins
     */
    fun retryHistoryPriceCoin() {
        historyPriceCoinResult.value?.retry?.invoke()
    }
}