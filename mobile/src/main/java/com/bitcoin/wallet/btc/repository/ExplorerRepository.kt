package com.bitcoin.wallet.btc.repository

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.bitcoin.wallet.btc.api.BitcoinEndpoints
import com.bitcoin.wallet.btc.base.BaseRepository
import com.bitcoin.wallet.btc.extension.addTo
import com.bitcoin.wallet.btc.extension.applySchedulers
import com.bitcoin.wallet.btc.model.explorer.BlocksResponse
import com.bitcoin.wallet.btc.repository.data.Listing
import com.bitcoin.wallet.btc.repository.data.TransactionsDataSourceFactory
import io.reactivex.Completable
import io.reactivex.functions.Action
import javax.inject.Inject

class ExplorerRepository @Inject constructor(
    private val api: BitcoinEndpoints
) : BaseRepository<Any>() {
    override fun insertResultIntoDb(body: Any) {
    }

    private val networkState = MutableLiveData<NetworkState>()
    private val data = MutableLiveData<BlocksResponse>()
    fun getLatestBlocks(limit: String): ListingData<BlocksResponse> {
        networkState.postValue(NetworkState.LOADING)
        api.getLatestBlocks(limit)
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
                    setRetryBlock(Action {
                        getLatestBlocks(limit)
                    })
                }
            )
            .addTo(compositeDisposable)
        return ListingData(
            data = data,
            networkState = networkState,
            retry = {
                retryBlockFailed()
            },
            refresh = {
                getLatestBlocks(limit)
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

    //get address, block detail, transactions
    private val config: PagedList.Config by lazy {
        PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(10 * 2)
            .setEnablePlaceholders(false)
            .build()
    }

    fun onGetTransactions(block: String): Listing<Any> {
        val sourceFactory = TransactionsDataSourceFactory(api, compositeDisposable, block)
        val pagedList = sourceFactory.toLiveData(config = config)

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        return Listing(
            pagedList = pagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveData.value?.retryAllFailed()
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }
}