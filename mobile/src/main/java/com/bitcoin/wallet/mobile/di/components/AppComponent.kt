package com.bitcoin.wallet.mobile.di.components

import android.app.Application
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.di.modules.ActivityBindingModule
import com.bitcoin.wallet.mobile.di.modules.ApplicationModule
import com.bitcoin.wallet.mobile.di.modules.FragmentBindingModule
import com.bitcoin.wallet.mobile.di.modules.NetworkModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ApplicationModule::class,
    NetworkModule::class,
    ActivityBindingModule::class,
    FragmentBindingModule::class,
    AndroidSupportInjectionModule::class])
interface AppComponent : AndroidInjector<DaggerApplication> {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        @BindsInstance
        fun networkModule(networkModule: NetworkModule): Builder

        fun build(): AppComponent
    }

    fun inject(app: BitcoinApplication)

    override fun inject(instance: DaggerApplication)

    companion object {
        fun getComponent(app: BitcoinApplication): AppComponent = DaggerAppComponent.builder()
            .application(app)
            .networkModule(NetworkModule())
            .build()
    }
}