package com.bitcoin.wallet.mobile.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bitcoin.wallet.mobile.di.annotations.ViewModelKey
import com.bitcoin.wallet.mobile.viewmodel.*
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindViewModelFactory(appViewModelFactory: AppViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(WalletViewModel::class)
    abstract fun bindWalletViewModel(viewModel: WalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExchangeRatesViewModel::class)
    abstract fun bindExchangeRatesViewModel(viewModel: ExchangeRatesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WalletTransactionsViewModel::class)
    abstract fun bindWalletTransactionsViewModel(viewModel: WalletTransactionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RaiseFeeViewModel::class)
    abstract fun bindRaiseFeeViewModel(viewModel: RaiseFeeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NetworkViewModel::class)
    abstract fun bindNetworkViewModel(viewModel: NetworkViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WalletAddressViewModel::class)
    abstract fun bindWalletAddressViewModel(viewModel: WalletAddressViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ScanViewModel::class)
    abstract fun bindScanViewModel(viewModel: ScanViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SendViewModel::class)
    abstract fun bindSendViewModel(viewModel: SendViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BackupWalletViewModel::class)
    abstract fun bindBackupWalletViewModel(viewModel: BackupWalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RestoreWalletViewModel::class)
    abstract fun bindRestoreWalletViewModel(viewModel: RestoreWalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RequestCoinsViewModel::class)
    abstract fun bindRequestCoinsViewModel(viewModel: RequestCoinsViewModel): ViewModel
}