package com.bitcoin.wallet.btc.repository

import androidx.lifecycle.MutableLiveData
import com.bitcoin.wallet.btc.CryptoCurrency
import com.bitcoin.wallet.btc.TimeSpan
import com.bitcoin.wallet.btc.api.BlockchainEndpoint
import com.bitcoin.wallet.btc.api.CoinbaseEndpoint
import com.bitcoin.wallet.btc.api.ZipHomeData
import com.bitcoin.wallet.btc.base.BaseRepository
import com.bitcoin.wallet.btc.extension.addTo
import com.bitcoin.wallet.btc.extension.applySchedulers
import com.bitcoin.wallet.btc.model.PriceDatum
import com.bitcoin.wallet.btc.model.blocks.BlocksResponse
import com.bitcoin.wallet.btc.model.info.InfoResponse
import com.bitcoin.wallet.btc.model.news.NewsResponse
import com.bitcoin.wallet.btc.model.summary.SummaryResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.Action
import io.reactivex.functions.Function5
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WalletRepository @Inject constructor(
    private val api: BlockchainEndpoint,
    private val coinbase: CoinbaseEndpoint
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

    //get latest blocks
    private val networkStateBlock = MutableLiveData<NetworkState>()
    private val blockData = MutableLiveData<BlocksResponse>()
    fun getLatestBlocks(): ListingData<BlocksResponse> {
        networkStateBlock.postValue(NetworkState.LOADING)
        api.getLastBlocks("https://blockchain.info/latestblocks?format=json&cors=true")
            .distinctUntilChanged()
            .repeatWhen { t -> t.delay(30, TimeUnit.SECONDS) }
            .applySchedulers()
            .subscribe(
                {
                    networkStateBlock.postValue(NetworkState.LOADED)
                    setRetryBlock(null)
                    blockData.postValue(it)
                },
                {
                    networkStateBlock.postValue(NetworkState.error(it.message))
                    setRetryBlock(Action { getLatestBlocks() })
                }
            ).addTo(compositeDisposable)
        return ListingData(
            data = blockData,
            networkState = networkStateBlock,
            refresh = {},
            retry = {
                retryBlockFailed()
            }
        )
    }

    /**
     * Keep Completable reference for the retry event
     */
    private var retryBlockCompletable: Completable? = null

    private fun retryBlockFailed() {
        retryBlockCompletable?.applySchedulers()
            ?.subscribe({ }, { it.printStackTrace() })
            ?.addTo(compositeDisposable)
    }

    private fun setRetryBlock(action: Action?) {
        if (action == null) {
            this.retryBlockCompletable = null
        } else {
            this.retryBlockCompletable = Completable.fromAction(action)
        }
    }

    // get data main home
    private val networkZipHomeState = MutableLiveData<NetworkState>()
    private val zipHomeData = MutableLiveData<ZipHomeData>()
    fun getHomeData(
        baseId: String,
        base: String,
        period: String,
        urlInfo: String,
        urlNews: String,
        urlSummary: String,
        cryptoCurrency: CryptoCurrency,
        fiatCurrency: String,
        timeSpan: TimeSpan
    ): ListingData<ZipHomeData> {
        networkZipHomeState.postValue(NetworkState.LOADING)
        val scale = when (timeSpan) {
            TimeSpan.ALL_TIME -> FIVE_DAYS
            TimeSpan.YEAR -> ONE_DAY
            TimeSpan.MONTH -> TWO_HOURS
            TimeSpan.WEEK -> ONE_HOUR
            TimeSpan.DAY -> FIFTEEN_MINUTES
        }
        var proposedStartTime = getStartTimeForTimeSpan(timeSpan, cryptoCurrency)
        // It's possible that the selected start time is before the currency existed, so check here
        // and show ALL_TIME instead if that's the case.
        if (proposedStartTime < getFirstMeasurement(cryptoCurrency)) {
            proposedStartTime = getStartTimeForTimeSpan(TimeSpan.ALL_TIME, cryptoCurrency)
        }
        Observable.zip(
            api.getHistoricPriceSeries(
                base = cryptoCurrency.symbol,
                quote = fiatCurrency,
                scale = scale,
                start = proposedStartTime
            ),
            coinbase.getInfoCoinbase(urlInfo),
            coinbase.getListNews(urlNews),
            coinbase.getListSummaryCoin(urlSummary),
            coinbase.getStatsCoinbase(baseId, base),
            Function5<List<PriceDatum>, InfoResponse, NewsResponse, SummaryResponse, com.bitcoin.wallet.btc.model.stats.StatsResponse,
                    ZipHomeData> { priceData, info, news, summary, stats ->
                ZipHomeData(priceData, info, news, summary, stats)
            }
        ).distinctUntilChanged()
            .applySchedulers()
            .subscribe(
                {
                    setRetryChartZip(null)
                    networkZipHomeState.postValue(NetworkState.LOADED)
                    zipHomeData.postValue(it)
                },
                {
                    setRetryChartZip(Action {
                        getHomeData(
                            baseId,
                            base,
                            period,
                            urlInfo,
                            urlNews,
                            urlSummary,
                            cryptoCurrency,
                            fiatCurrency,
                            timeSpan
                        )
                    })
                    val error = NetworkState.error(it.message)
                    // publish the error
                    networkZipHomeState.postValue(error)
                }
            ).addTo(compositeDisposable)
        return ListingData(
            data = zipHomeData,
            networkState = networkZipHomeState,
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

    private fun getStartTimeForTimeSpan(
        timeSpan: TimeSpan,
        cryptoCurrency: CryptoCurrency
    ): Long {
        val start = when (timeSpan) {
            TimeSpan.ALL_TIME -> return getFirstMeasurement(cryptoCurrency)
            TimeSpan.YEAR -> 365
            TimeSpan.MONTH -> 30
            TimeSpan.WEEK -> 7
            TimeSpan.DAY -> 1
        }

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -start) }
        return cal.timeInMillis / 1000
    }

    /**
     * Provides the first timestamp for which we have prices, returned in epoch-seconds
     *
     * @param cryptoCurrency The [CryptoCurrency] that you want a start date for
     * @return A [Long] in epoch-seconds since the start of our data
     */
    private fun getFirstMeasurement(cryptoCurrency: CryptoCurrency): Long {
        return when (cryptoCurrency) {
            CryptoCurrency.BTC -> FIRST_BTC_ENTRY_TIME
            CryptoCurrency.ETHER -> FIRST_ETH_ENTRY_TIME
            CryptoCurrency.BCH -> FIRST_BCH_ENTRY_TIME
            CryptoCurrency.XLM -> FIRST_XLM_ENTRY_TIME
            CryptoCurrency.PAX -> TODO("PAX is not yet supported - AND-2003")
        }
    }

    /**
     * A simple class of timescale constants for the [BlockchainEndpoint] methods.
     */
    companion object Scale {
        const val FIFTEEN_MINUTES = 900
        const val ONE_HOUR = 3600
        const val TWO_HOURS = 7200
        const val ONE_DAY = 86400
        const val FIVE_DAYS = 432000

        /**
         * All time start times in epoch-seconds
         */
        // 2010-08-18 00:00:00 UTC
        const val FIRST_BTC_ENTRY_TIME = 1282089600L
        // 2015-08-08 00:00:00 UTC
        const val FIRST_ETH_ENTRY_TIME = 1438992000L
        // 2017-07-24 00:00:00 UTC
        const val FIRST_BCH_ENTRY_TIME = 1500854400L
        // 2014-09-04 00:00:00 UTC
        const val FIRST_XLM_ENTRY_TIME = 1409875200L

    }
}