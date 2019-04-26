package com.bitcoin.wallet.btc.repository

import androidx.lifecycle.MutableLiveData
import com.bitcoin.wallet.btc.api.BlockchainEndpoint
import com.bitcoin.wallet.btc.base.BaseRepository
import com.bitcoin.wallet.btc.extension.addTo
import com.bitcoin.wallet.btc.extension.applySchedulers
import com.bitcoin.wallet.btc.model.PriceDatum
import com.bitcoin.wallet.btc.model.StatsResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WalletRepository @Inject constructor(
    private val api: BlockchainEndpoint
) : BaseRepository<Any>() {
    override fun insertResultIntoDb(body: Any) {
    }

    private val networkPriceState = MutableLiveData<NetworkState>()
    private val priceData = MutableLiveData<Map<String, PriceDatum>>()
    fun onGetPrice(
        base: String,
        apiKey: String
    ): ListingData<Map<String, PriceDatum>> {
        networkPriceState.postValue(NetworkState.LOADING)
        api.getPriceIndexes(base, apiKey)
            .distinctUntilChanged()
            .applySchedulers()
            .subscribe(
                {
                    networkPriceState.postValue(NetworkState.LOADED)
                    priceData.postValue(it)
                },
                {
                    val error = NetworkState.error(it.message)
                    // publish the error
                    networkPriceState.postValue(error)
                }
            ).addTo(compositeDisposable)
        return ListingData(
            data = priceData,
            networkState = networkPriceState,
            retry = {
            },
            refresh = {}
        )
    }

    //get stats blockchain.info
    private val networkStatState = MutableLiveData<NetworkState>()
    private val statsData = MutableLiveData<StatsResponse>()
    fun getStats(compositeDisposable: CompositeDisposable): ListingData<StatsResponse> {
        networkStatState.postValue(NetworkState.LOADING)
        api.getStats(true)
            .distinctUntilChanged()
            .repeatWhen { t -> t.delay(60, TimeUnit.SECONDS) }
            .applySchedulers()
            .subscribe(
                {
                    networkStatState.postValue(NetworkState.LOADED)
                    statsData.postValue(it)
                },
                {
                    val error = NetworkState.error(it.message)
                    // publish the error
                    networkStatState.postValue(error)
                }
            )
            .addTo(compositeDisposable)
        return ListingData(
            data = statsData,
            networkState = networkStatState,
            refresh = {},
            retry = {}
        )
    }

    /**
     * get data chart home
     */
    private val networkZipChartState = MutableLiveData<NetworkState>()
    private val zipChartData = MutableLiveData<ZipPriceChart>()
    fun onGetZipDataChartPrice(
        base: String,
        quote: String,
        start: Long,
        scale: Int,
        apiKey: String
    ): ListingData<ZipPriceChart> {
        networkZipChartState.postValue(NetworkState.LOADING)
        Observable.zip(
            api.getHistoricPriceSeries(base, quote, start, scale, apiKey),
            api.getPriceIndexes(base, apiKey),
            BiFunction<List<PriceDatum>,
                    Map<String, PriceDatum>,
                    ZipPriceChart>
            { dataChart, price ->
                ZipPriceChart(price, dataChart)
            })
            .distinctUntilChanged()
            .applySchedulers()
            .subscribe(
                {
                    setRetryChartZip(null)
                    networkZipChartState.postValue(NetworkState.LOADED)
                    zipChartData.postValue(it)
                },
                {
                    setRetryChartZip(Action { onGetZipDataChartPrice(base, quote, start, scale, apiKey) })
                    val error = NetworkState.error(it.message)
                    // publish the error
                    networkZipChartState.postValue(error)
                }
            ).addTo(compositeDisposable)
        return ListingData(
            data = zipChartData,
            networkState = networkZipChartState,
            retry = {
                retryZipChartFailed()
            },
            refresh = {}
        )
    }

    /**
     * Keep Completable reference for the retry event
     */
    private var retryZipChartCompletable: Completable? = null

    private fun retryZipChartFailed() {
        retryZipChartCompletable?.applySchedulers()
            ?.subscribe({ }, { it.printStackTrace() })
            ?.addTo(compositeDisposable)
    }

    private fun setRetryChartZip(action: Action?) {
        if (action == null) {
            this.retryZipChartCompletable = null
        } else {
            this.retryZipChartCompletable = Completable.fromAction(action)
        }
    }

    data class ZipPriceChart(
        val price: Map<String, PriceDatum>,
        val dateChart: List<PriceDatum>
    )
}