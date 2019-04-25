package com.bitcoin.wallet.mobile.di.modules

import com.bitcoin.wallet.mobile.di.scopes.PerFragment
import com.bitcoin.wallet.mobile.ui.fragments.*
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentBindingModule {
    @PerFragment
    @ContributesAndroidInjector
    abstract fun provideMainFragment(): MainFragment

    @PerFragment
    @ContributesAndroidInjector
    abstract fun provideRaiseFeeDialogFragment(): RaiseFeeDialogFragment

    @PerFragment
    @ContributesAndroidInjector
    abstract fun provideBackupDialog(): BackupDialog

    @PerFragment
    @ContributesAndroidInjector
    abstract fun provideRestoreWalletDialog(): RestoreWalletDialog

    @PerFragment
    @ContributesAndroidInjector
    abstract fun provideWalletAddressBottomDialog(): WalletAddressBottomDialog

    @PerFragment
    @ContributesAndroidInjector
    abstract fun provideEncryptKeysDialogFragment(): EncryptKeysDialogFragment

    @PerFragment
    @ContributesAndroidInjector
    abstract fun provideHelpDialogFragment(): HelpDialogFragment
}