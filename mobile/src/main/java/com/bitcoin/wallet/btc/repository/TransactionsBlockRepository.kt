package com.bitcoin.wallet.btc.repository

import androidx.lifecycle.MutableLiveData
import com.bitcoin.wallet.btc.api.BlockchainEndpoint
import com.bitcoin.wallet.btc.base.BaseRepository
import com.bitcoin.wallet.btc.extension.addTo
import com.bitcoin.wallet.btc.extension.applySchedulers
import com.bitcoin.wallet.btc.model.transactions.TransactionsResponse
import io.reactivex.Completable
import io.reactivex.functions.Action
import javax.inject.Inject

class TransactionsBlockRepository @Inject constructor(private val api: BlockchainEndpoint) :
    BaseRepository<Any>() {
    override fun insertResultIntoDb(body: Any) {
    }

    private val data = MutableLiveData<TransactionsResponse>()
    private val networkState = MutableLiveData<NetworkState>()
    fun getTransactionsBlock(url: String): ListingData<TransactionsResponse> {
        networkState.postValue(NetworkState.LOADING)
        api.getTransactionsBlock(url)
            .distinctUntilChanged()
            .applySchedulers()
            .subscribe(
                {
                    networkState.postValue(NetworkState.LOADED)
                    setRetryBlock(null)
                    data.postValue(it)
                },
                {
                    networkState.postValue(NetworkState.error(it.message))
                    setRetryBlock(Action { getTransactionsBlock(url) })
                }
            )
            .addTo(compositeDisposable)
        return ListingData(
            data = data,
            networkState = networkState,
            retry = {
                retryBlockFailed()
            },
            refresh = {}
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