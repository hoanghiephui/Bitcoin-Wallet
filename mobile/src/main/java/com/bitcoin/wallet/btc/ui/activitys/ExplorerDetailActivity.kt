package com.bitcoin.wallet.btc.ui.activitys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.ui.adapter.TransactionsExtraAdapter
import com.bitcoin.wallet.btc.ui.fragments.WalletAddressBottomDialog
import com.bitcoin.wallet.btc.viewmodel.ExplorerViewModel
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.init_ads.*
import org.bitcoinj.core.Address

class ExplorerDetailActivity : BaseActivity() {
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[ExplorerViewModel::class.java]
    }

    private val transactionAdapter by lazy {
        TransactionsExtraAdapter(retryCallback = {
            viewModel.retryTransaction()
        }, showQrCode = {
            WalletAddressBottomDialog.show(this, Address.fromString(Constants.NETWORK_PARAMETERS, it), null)
        })
    }
    private var bannerAdView: AdView? = null

    override fun layoutRes(): Int {
        return R.layout.activity_list
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Transactions Explorer")
        val input = intent.getStringExtra("input")
        initRecyclerView()
        viewModel.transaction.observeNotNull(this) {
            transactionAdapter.submitList(it)
        }
        viewModel.networkTransactionState.observeNotNull(this) {
            transactionAdapter.onNetworkState(it)
        }
        viewModel.refreshTransactionState.observeNotNull(this) {
            freshlayout.isRefreshing = it == NetworkState.LOADING && freshlayout.isRefreshing
        }
        viewModel.onGetTransaction(input)
        loadAdView()
    }

    private fun initRecyclerView() {
        recyClear.apply {
            layoutManager = LinearLayoutManager(this@ExplorerDetailActivity)
            adapter = transactionAdapter
        }

        freshlayout.setOnRefreshListener {
            viewModel.refreshTransactions()
        }
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        super.onDestroy()
    }

    private fun loadAdView() {
        bannerAdView?.destroy()
        bannerAdView = null
        bannerAdView = AdView(this, getString(R.string.fb_banner_explorer_detail), AdSize.BANNER_HEIGHT_50)
        bannerAdView?.let {nonNullBannerAdView ->
            adViewContainer?.addView(nonNullBannerAdView)
            nonNullBannerAdView.loadAd()
        }
    }

    companion object {
        fun open(
            context: Context,
            input: String
        ) {
            Intent(context, ExplorerDetailActivity::class.java)
                .apply {
                    putExtra("input", input)
                    context.startActivity(this)
                }
        }
    }
}