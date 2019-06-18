package com.bitcoin.wallet.btc.repository.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.bitcoin.wallet.btc.api.BitcoinEndpoints
import io.reactivex.disposables.CompositeDisposable

class TransactionsDataSourceFactory(
    private val api: BitcoinEndpoints,
    private val compositeDisposable: CompositeDisposable,
    private val hash: String
) : DataSource.Factory<Int, Any>() {
    val sourceLiveData = MutableLiveData<TransactionsDataSource>()
    override fun create(): DataSource<Int, Any> {
        val dataSource = TransactionsDataSource(api, compositeDisposable, hash)
        sourceLiveData.postValue(dataSource)
        return dataSource
    }
}