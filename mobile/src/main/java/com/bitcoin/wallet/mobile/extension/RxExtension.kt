package com.bitcoin.wallet.mobile.extension

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

fun Completable.applySchedulers(): Completable = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError { it.printStackTrace() }

fun <T> Flowable<T>.applySchedulers(): Flowable<T> = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError { it.printStackTrace() }

fun <T> Observable<T>.applySchedulers(): Observable<T> = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError { it.printStackTrace() }

fun Disposable.addTo(autoDisposable: CompositeDisposable) {
    autoDisposable.add(this)
}