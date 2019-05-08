package com.bitcoin.wallet.btc.di.modules

import androidx.work.RxWorker
import com.bitcoin.wallet.btc.di.annotations.WorkerKey
import com.bitcoin.wallet.btc.works.NotifyWorker
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class WorkerModule {
    @Binds
    @IntoMap
    @WorkerKey(NotifyWorker::class)
    abstract fun binNotifyWoker(worker: NotifyWorker): RxWorker
}