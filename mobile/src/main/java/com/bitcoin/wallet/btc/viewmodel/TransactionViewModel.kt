package com.bitcoin.wallet.btc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.bitcoin.wallet.btc.base.BaseViewModel
import com.bitcoin.wallet.btc.repository.TransactionsBlockRepository
import javax.inject.Inject

class TransactionViewModel @Inject constructor(repository: TransactionsBlockRepository) :
    BaseViewModel<TransactionsBlockRepository>(repository) {

    //get list transactions
    private val transactionRequestData = MutableLiveData<String>()
    private val transactionResult = Transformations.map(transactionRequestData) {
        repository.getTransactionsBlock(it)
    }
    val transactionData = Transformations.switchMap(transactionResult) { it.data }
    val networkState = Transformations.switchMap(transactionResult) { it.networkState }
    val refreshState = Transformations.switchMap(transactionResult) { it.networkState }

    fun onGetTransactions(url: String) {
        transactionRequestData.postValue(url)
    }

    /**
     * @method retry get list transaction
     */
    fun retryTransactions() {
        transactionResult?.value?.retry?.invoke()
    }

    fun refreshTransaction() {
        transactionResult.value?.refresh?.invoke()
    }

}