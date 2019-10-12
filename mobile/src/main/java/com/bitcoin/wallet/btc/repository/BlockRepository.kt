package com.bitcoin.wallet.btc.repository

import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.bitcoin.wallet.btc.api.BitcoinEndpoints
import com.bitcoin.wallet.btc.base.BaseRepository
import com.bitcoin.wallet.btc.model.explorer.BlocksItem
import com.bitcoin.wallet.btc.repository.data.BlockDataSourceFactory
import com.bitcoin.wallet.btc.repository.data.Listing
import javax.inject.Inject

class BlockRepository @Inject constructor(
    private val api: BitcoinEndpoints
) : BaseRepository<Any>() {

    override fun insertResultIntoDb(body: Any) {
    }

    //get address, block detail, transactions
    private val config: PagedList.Config by lazy {
        PagedList.Config.Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(10 * 2)
            .setEnablePlaceholders(false)
            .build()
    }

    fun onGetBlocks(currentDay: String): Listing<BlocksItem> {
        val sourceFactory = BlockDataSourceFactory(api, compositeDisposable, currentDay)
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