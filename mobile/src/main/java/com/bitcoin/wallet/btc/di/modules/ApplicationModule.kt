package com.bitcoin.wallet.btc.di.modules

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.paging.PagedList
import androidx.preference.PreferenceManager
import com.bitcoin.wallet.btc.Constants.PAGE_SIZE
import com.bitcoin.wallet.btc.di.annotations.ForApplication
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

    @Singleton
    @Provides
    fun providePagedListConfig(): PagedList.Config {
        return PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .setInitialLoadSizeHint(PAGE_SIZE * 2)
            .setEnablePlaceholders(false)
            .build()
    }
}