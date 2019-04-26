package com.bitcoin.wallet.btc.base

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable

abstract class BaseViewModel<out T : BaseRepository<*>> constructor(val repository: T) : ViewModel() {
    val compositeDisposable = CompositeDisposable()

    init {
        repository.compositeDisposable = compositeDisposable
    }

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }
}