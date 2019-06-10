package com.bitcoin.wallet.btc.repository

import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.bitcoin.wallet.btc.api.CoinbaseEndpoint
import com.bitcoin.wallet.btc.base.BaseRepository
import com.bitcoin.wallet.btc.model.news.DataItem
import com.bitcoin.wallet.btc.model.news.NewsResponse
import com.bitcoin.wallet.btc.repository.data.Listing
import com.bitcoin.wallet.btc.repository.data.StoryDataSourceFactory
import javax.inject.Inject

class StoryRepository @Inject constructor(
    private val api: CoinbaseEndpoint,
    private val config: PagedList.Config
) : BaseRepository<NewsResponse>() {

    override fun insertResultIntoDb(body: NewsResponse) {
    }

    fun onNewsMore(baseId: String): Listing<DataItem> {
        val sourceFactory = StoryDataSourceFactory(api, compositeDisposable, baseId)
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