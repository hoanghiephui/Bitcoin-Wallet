package com.bitcoin.wallet.btc.di.modules

import com.bitcoin.wallet.btc.di.scopes.PerActivity
import com.bitcoin.wallet.btc.ui.activitys.*
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBindingModule {
    @PerActivity
    @ContributesAndroidInjector
    abstract fun mainActivity(): MainActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun exchangeRatesActivity(): ExchangeRatesActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun walletTransactionsActivity(): WalletTransactionsActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun networkActivity(): NetworkActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun addressActivity(): AddressActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun scanActivity(): ScanActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun sendCoinActivity(): SendCoinActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun requestCoinActivity(): RequestCoinActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun settingActivity(): SettingActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun aboutActivity(): AboutActivity
}