package com.bitcoin.wallet.btc.repository

import androidx.lifecycle.MutableLiveData
import com.bitcoin.wallet.btc.api.BitcoinEndpoints
import com.bitcoin.wallet.btc.base.BaseRepository
import com.bitcoin.wallet.btc.extension.addTo
import com.bitcoin.wallet.btc.extension.applySchedulers
import com.bitcoin.wallet.btc.model.CoinPriceResponse
import com.bitcoin.wallet.btc.model.index.LookupResponse
import io.reactivex.Completable
import io.reactivex.functions.Action
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ToolsRepository @Inject constructor(private val api: BitcoinEndpoints) : BaseRepository<Any>() {

    override fun insertResultIntoDb(body: Any) {
    }

    private val data = MutableLiveData<CoinPriceResponse>()
    private val networkState = MutableLiveData<NetworkState>()
    fun getPriceCoin(): ListingData<CoinPriceResponse> {
        networkState.postValue(NetworkState.LOADING)
        api.getPriceCoin("https://coin-api.bitcoin.com/v1/bitcoins")
            .distinctUntilChanged()
            .repeatWhen { t -> t.delay(30, TimeUnit.SECONDS) }
            .applySchedulers()
            .subscribe(
                {
                    networkState.postValue(NetworkState.LOADED)
                    setRetryBlock(null)
                    data.postValue(it)
                },
                {
                    networkState.postValue(NetworkState.error(it.message))
                    setRetryBlock(Action { getPriceCoin() })
                }
            )
            .addTo(compositeDisposable)
        return ListingData(
            data = data,
            networkState = networkState,
            refresh = {},
            retry = {
                retryBlockFailed()
            }
        )
    }

    private val dataLookup = MutableLiveData<LookupResponse>()
    private val networkStateLookup = MutableLiveData<NetworkState>()
    fun getLookupCoin(type: String,
                      time: String): ListingData<LookupResponse> {
        networkStateLookup.postValue(NetworkState.LOADING)
        api.getLookupCoin("https://index-api.bitcoin.com/api/v0/$type/lookup?time=$time")
            .distinctUntilChanged()
            .applySchedulers()
            .subscribe(
                {
                    networkStateLookup.postValue(NetworkState.LOADED)
                    setRetryBlock(null)
                    dataLookup.postValue(it)
                },
                {
                    networkStateLookup.postValue(NetworkState.error(it.message))
                    setRetryBlock(Action { getLookupCoin(type, time) })
                }
            )
            .addTo(compositeDisposable)
        return ListingData(
            data = dataLookup,
            networkState = networkStateLookup,
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
}