package com.bitcoin.wallet.btc.di.modules

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.bitcoin.wallet.btc.di.annotations.ForApplication
import com.bitcoin.wallet.btc.utils.Configuration
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [ViewModelModule::class])
class ApplicationModule {

    @Provides
    @ForApplication
    fun providesApplicationContext(application: Application): Context {
        return application
    }

    @Provides
    fun sharedPreferences(@ForApplication context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}