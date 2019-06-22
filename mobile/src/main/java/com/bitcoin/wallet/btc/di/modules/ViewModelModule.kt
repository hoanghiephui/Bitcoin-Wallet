package com.bitcoin.wallet.btc.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bitcoin.wallet.btc.di.annotations.ViewModelKey
import com.bitcoin.wallet.btc.viewmodel.*
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

    @Binds
    @IntoMap
    @ViewModelKey(SweepWalletViewModel::class)
    abstract fun bindSweepWalletViewModel(viewModel: SweepWalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReportIssueViewModel::class)
    abstract fun bindReportIssueViewModel(viewModel: ReportIssueViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(StoryViewModel::class)
    abstract fun bindStoryViewModel(viewModel: StoryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionViewModel::class)
    abstract fun binTransactionViewModel(viewModel: TransactionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BillingViewModel::class)
    abstract fun binBillingViewModel(viewModel: BillingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExplorerViewModel::class)
    abstract fun binExplorerViewModel(viewModel: ExplorerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BlocksViewModel::class)
    abstract fun binBlocksViewModel(viewModel: BlocksViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ToolsViewModel::class)
    abstract fun binToolsViewModel(viewModel: ToolsViewModel): ViewModel
}