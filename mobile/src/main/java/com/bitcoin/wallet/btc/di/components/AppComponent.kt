package com.bitcoin.wallet.btc.di.components

import android.app.Application
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.di.DaggerWorkerFactory
import com.bitcoin.wallet.btc.di.modules.ActivityBindingModule
import com.bitcoin.wallet.btc.di.modules.ApplicationModule
import com.bitcoin.wallet.btc.di.modules.FragmentBindingModule
import com.bitcoin.wallet.btc.di.modules.NetworkModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        NetworkModule::class,
        ActivityBindingModule::class,
        FragmentBindingModule::class,
        AndroidSupportInjectionModule::class]
)
interface AppComponent : AndroidInjector<DaggerApplication> {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun networkModule(networkModule: NetworkModule): Builder

        fun build(): AppComponent
    }

    fun daggerWorkerFactory(): DaggerWorkerFactory

    fun workerSubComponentBuilder(): WorkerSubComponent.Builder

    fun inject(app: BitcoinApplication)

    override fun inject(instance: DaggerApplication)

    companion object {
        fun getComponent(app: BitcoinApplication): AppComponent = DaggerAppComponent.builder()
            .application(app)
            .networkModule(NetworkModule())
            .build()
    }
}