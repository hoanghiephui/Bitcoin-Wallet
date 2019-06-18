package com.bitcoin.wallet.btc.repository.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.bitcoin.wallet.btc.api.BitcoinEndpoints
import com.bitcoin.wallet.btc.model.explorer.BlocksItem
import io.reactivex.disposables.CompositeDisposable

class BlockDataSourceFactory(
    private val api: BitcoinEndpoints,
    private val compositeDisposable: CompositeDisposable,
    private val currentDay: String
) : DataSource.Factory<String, BlocksItem>() {
    val sourceLiveData = MutableLiveData<BlocksDataSource>()
    override fun create(): DataSource<String, BlocksItem> {
        val dataSource = BlocksDataSource(api, compositeDisposable, currentDay)
        sourceLiveData.postValue(dataSource)
        return dataSource
    }
}