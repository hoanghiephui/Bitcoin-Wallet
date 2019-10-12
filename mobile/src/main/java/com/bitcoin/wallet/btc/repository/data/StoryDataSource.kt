package com.bitcoin.wallet.btc.repository.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.ItemKeyedDataSource
import com.bitcoin.wallet.btc.Constants.PAGE_SIZE
import com.bitcoin.wallet.btc.api.CoinbaseEndpoint
import com.bitcoin.wallet.btc.extension.addTo
import com.bitcoin.wallet.btc.extension.applySchedulers
import com.bitcoin.wallet.btc.model.news.DataItem
import com.bitcoin.wallet.btc.model.news.NewsResponse
import com.bitcoin.wallet.btc.repository.NetworkState
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action

class StoryDataSource(
    private val api: CoinbaseEndpoint,
    private val compositeDisposable: CompositeDisposable,
    private val baseId: String
) : ItemKeyedDataSource<String, DataItem>() {
    var startingAfter: String? = null
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
        callback: LoadInitialCallback<DataItem>
    ) {
        // update network states.
        // we also provide an initial load state to the listeners so that the UI can know when the
        // very first list is loaded.
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)
        onInitial(null)
            .applySchedulers()
            .subscribe(
                {
                    startingAfter = it.pagination?.nextStartingAfter
                    setRetry(null)
                    networkState.postValue(NetworkState.LOADED)
                    initialLoad.postValue(NetworkState.LOADED)
                    it.data?.let { it1 -> callback.onResult(it1) }
                },
                {
                    setRetry(Action { loadInitial(params, callback) })
                    val error = NetworkState.error(it.message)
                    networkState.postValue(error)
                    initialLoad.postValue(networkState.value)
                })
            .addTo(compositeDisposable)

    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<DataItem>) {
        if (startingAfter == "") {
            return
        }
        // set network value to loading.
        networkState.postValue(NetworkState.LOADING)
        onInitial(startingAfter)
            .applySchedulers()
            .subscribe(
                {
                    startingAfter = it.pagination?.nextStartingAfter
                    setRetry(null)
                    networkState.postValue(NetworkState.LOADED)
                    it.data?.let { it1 -> callback.onResult(it1) }
                },
                {
                    setRetry(Action { loadAfter(params, callback) })
                    val error = NetworkState.error(it.message)
                    networkState.postValue(error)
                })
            .addTo(compositeDisposable)
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<DataItem>) {
    }

    override fun getKey(item: DataItem): String {
        return startingAfter ?: ""
    }

    private fun onInitial(startingAfter: String?): Observable<NewsResponse> {
        return api.getListNews(baseId, PAGE_SIZE, startingAfter)
    }
}