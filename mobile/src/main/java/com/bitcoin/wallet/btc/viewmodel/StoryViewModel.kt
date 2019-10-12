package com.bitcoin.wallet.btc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.bitcoin.wallet.btc.base.BaseViewModel
import com.bitcoin.wallet.btc.repository.StoryRepository
import javax.inject.Inject

class StoryViewModel @Inject constructor(storyRepository: StoryRepository) :
    BaseViewModel<StoryRepository>(storyRepository) {

    //get list news
    private val newsRequestData = MutableLiveData<String>()
    private val newsResult = Transformations.map(newsRequestData) {
        repository.onNewsMore(it)
    }
    val newsData = Transformations.switchMap(newsResult) { it.pagedList }
    val networkState = Transformations.switchMap(newsResult) { it.networkState }
    val refreshState = Transformations.switchMap(newsResult) { it.refreshState }

    fun onGetNews(baseId: String) {
        newsRequestData.postValue(baseId)
    }

    /**
     * @method retry get list news
     */
    fun retryNews() {
        newsResult.value?.retry?.invoke()
    }

    fun refreshNews() {
        newsResult.value?.refresh?.invoke()
    }
}