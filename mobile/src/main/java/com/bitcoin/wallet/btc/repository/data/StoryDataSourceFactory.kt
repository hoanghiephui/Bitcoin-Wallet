package com.bitcoin.wallet.btc.repository.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.bitcoin.wallet.btc.api.CoinbaseEndpoint
import com.bitcoin.wallet.btc.model.news.DataItem
import io.reactivex.disposables.CompositeDisposable

class StoryDataSourceFactory(
    private val api: CoinbaseEndpoint,
    private val compositeDisposable: CompositeDisposable,
    private val baseId: String
) : DataSource.Factory<String, DataItem>() {
    val sourceLiveData = MutableLiveData<StoryDataSource>()

    override fun create(): DataSource<String, DataItem> {
        val dataSource = StoryDataSource(api, compositeDisposable, baseId)
        sourceLiveData.postValue(dataSource)
        return dataSource
    }
}