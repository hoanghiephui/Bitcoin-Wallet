package com.bitcoin.wallet.btc.base

import io.reactivex.disposables.CompositeDisposable

abstract class BaseRepository<T> {
    lateinit var compositeDisposable: CompositeDisposable

    /**
     * Inserts the response into the database while also assigning position indices to items.
     */
    abstract fun insertResultIntoDb(body: T)
}