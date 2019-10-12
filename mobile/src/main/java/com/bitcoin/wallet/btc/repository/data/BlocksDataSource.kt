package com.bitcoin.wallet.btc.repository.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.ItemKeyedDataSource
import com.bitcoin.wallet.btc.api.BitcoinEndpoints
import com.bitcoin.wallet.btc.extension.addTo
import com.bitcoin.wallet.btc.extension.applySchedulers
import com.bitcoin.wallet.btc.model.explorer.BlocksItem
import com.bitcoin.wallet.btc.repository.NetworkState
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action

class BlocksDataSource(
    private val api: BitcoinEndpoints,
    private val compositeDisposable: CompositeDisposable,
    private val currentDay: String
) : ItemKeyedDataSource<String, BlocksItem>() {
    private var mCurrentDay = currentDay
    val networkState = MutableLiveData<NetworkState>()
    val initialLoad = MutableLiveData<NetworkState>()
    /**
     * Keep Completable reference for the retry event
     */
    private var retryCompletable: Completable? = null

    fun retryAllFailed() {
        retryCompletable?.applySchedulers()
            ?.subscribe({ }, { throwable -> throwable.printStackTrace() })
            ?.addTo(compositeDisposable)
    }

    private fun setRetry(action: Action?) {
        if (action == null) {
            this.retryCompletable = null
        } else {
            this.retryCompletable = Completable.fromAction(action)
        }
    }

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<BlocksItem>
    ) {
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)
        api.getLatestBlocksByDate(currentDay)
            .distinctUntilChanged()
            .applySchedulers()
            .subscribe(
                {
                    // clear retry since last request succeeded
                    setRetry(null)
                    networkState.postValue(NetworkState.LOADED)
                    initialLoad.postValue(NetworkState.LOADED)
                    mCurrentDay = it.pagination?.prev ?: currentDay
                    it.blocks?.let { it1 -> callback.onResult(it1) }
                },
                {
                    // keep a Completable for future retry
                    setRetry(Action { loadInitial(params, callback) })
                    val error = NetworkState.error(it.message)
                    networkState.postValue(error)
                    initialLoad.postValue(networkState.value)
                }
            )
            .addTo(compositeDisposable)
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<BlocksItem>) {
        api.getLatestBlocksByDate(params.key)
            .distinctUntilChanged()
            .applySchedulers()
            .subscribe(
                {
                    // clear retry since last request succeeded
                    setRetry(null)
                    networkState.postValue(NetworkState.LOADED)
                    mCurrentDay = it.pagination?.prev ?: currentDay
                    it.blocks?.let { it1 -> callback.onResult(it1) }
                },
                {
                    // keep a Completable for future retry
                    setRetry(Action { loadAfter(params, callback) })
                    val error = NetworkState.error(it.message)
                    networkState.postValue(error)
                }
            )
            .addTo(compositeDisposable)
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<BlocksItem>) {

    }

    override fun getKey(item: BlocksItem): String {
        return mCurrentDay
    }
}