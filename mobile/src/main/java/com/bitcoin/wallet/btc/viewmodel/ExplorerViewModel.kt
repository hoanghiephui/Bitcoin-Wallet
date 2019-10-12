package com.bitcoin.wallet.btc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.bitcoin.wallet.btc.base.BaseViewModel
import com.bitcoin.wallet.btc.repository.ExplorerRepository
import javax.inject.Inject

class ExplorerViewModel @Inject constructor(repository: ExplorerRepository) :
    BaseViewModel<ExplorerRepository>(repository) {

    //get latest blocks
    private val latestBlockRequestData = MutableLiveData<String>()
    private val latestBlocksResult = Transformations.map(latestBlockRequestData) {
        repository.getLatestBlocks(it)
    }
    val latestBlocksData = Transformations.switchMap(latestBlocksResult) { it.data }
    val networkState = Transformations.switchMap(latestBlocksResult) { it.networkState }

    fun onGetLatestBlocks(limit: String) {
        latestBlockRequestData.postValue(limit)
    }

    /**
     * @method retry get list latest blocks
     */
    fun retryLatestBlock() {
        latestBlocksResult.value?.retry?.invoke()
    }

    //get list transaction
    private val transactionRequestData = MutableLiveData<String>()
    private val transactionResult = Transformations.map(transactionRequestData) {
        repository.onGetTransactions(it)
    }
    val transaction = Transformations.switchMap(transactionResult) { it.pagedList }
    val networkTransactionState = Transformations.switchMap(transactionResult) { it.networkState }
    val refreshTransactionState = Transformations.switchMap(transactionResult) { it.refreshState }

    fun onGetTransaction(hash: String) {
        transactionRequestData.postValue(hash)
    }

    /**
     * @method retry get list transaction
     */
    fun retryTransaction() {
        transactionResult.value?.retry?.invoke()
    }

    fun refreshTransactions() {
        transactionResult.value?.refresh?.invoke()
    }
}