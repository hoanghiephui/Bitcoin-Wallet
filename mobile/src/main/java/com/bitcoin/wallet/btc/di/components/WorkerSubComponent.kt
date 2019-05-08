package com.bitcoin.wallet.btc.di.components

import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.bitcoin.wallet.btc.di.modules.WorkerModule
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Provider

@Subcomponent(modules = [WorkerModule::class])
interface WorkerSubComponent {

    fun workers(): Map<Class<out RxWorker>, Provider<RxWorker>>

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun workerParameters(param: WorkerParameters): Builder

        fun build(): WorkerSubComponent
    }
}