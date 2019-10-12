package com.bitcoin.wallet.btc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.bitcoin.wallet.btc.base.BaseViewModel
import com.bitcoin.wallet.btc.repository.BlockRepository
import javax.inject.Inject

class BlocksViewModel @Inject constructor(repository: BlockRepository) :
    BaseViewModel<BlockRepository>(repository) {
    //get list transaction
    private val blockRequestData = MutableLiveData<String>()
    private val blockResult = Transformations.map(blockRequestData) {
        repository.onGetBlocks(it)
    }
    val blocks = Transformations.switchMap(blockResult) { it.pagedList }
    val networkBlockState = Transformations.switchMap(blockResult) { it.networkState }
    val refreshBlockState = Transformations.switchMap(blockResult) { it.refreshState }

    fun onGetBlocks(currentDay: String) {
        blockRequestData.postValue(currentDay)
    }

    /**
     * @method retry get list blocks
     */
    fun retryBlock() {
        blockResult.value?.retry?.invoke()
    }

    fun refreshBlock() {
        blockResult.value?.refresh?.invoke()
    }
}