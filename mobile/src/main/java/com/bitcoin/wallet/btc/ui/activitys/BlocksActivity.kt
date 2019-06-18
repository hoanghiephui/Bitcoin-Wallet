package com.bitcoin.wallet.btc.ui.activitys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.ui.adapter.explorer.LatestBlockAdapter
import com.bitcoin.wallet.btc.viewmodel.BlocksViewModel
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.init_ads.*

class BlocksActivity : BaseActivity() {
    private val blockAdapter by lazy {
        LatestBlockAdapter(
            retryCallback = {
                viewModel.retryBlock()
            },
            onClickItem = {
                it?.let { it1 -> ExplorerDetailActivity.open(this, it1) }
            }
        )
    }
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[BlocksViewModel::class.java]
    }
    private var bannerAdView: AdView? = null

    override fun layoutRes(): Int {
        return R.layout.activity_list
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Blocks")
        initRecyclearView()
        val currentDay = intent.getStringExtra("blockDate")
        viewModel.blocks.observeNotNull(this) {
            blockAdapter.submitList(it)
        }
        viewModel.networkBlockState.observeNotNull(this) {
            blockAdapter.onNetworkState(it)
        }
        viewModel.refreshBlockState.observeNotNull(this) {
            freshlayout.isRefreshing = it == NetworkState.LOADING && freshlayout.isRefreshing
        }
        viewModel.onGetBlocks(currentDay)
        loadAdView()
    }

    private fun initRecyclearView() {
        recyClear.apply {
            layoutManager = LinearLayoutManager(this@BlocksActivity)
            setHasFixedSize(true)
            adapter = blockAdapter
        }

        freshlayout.setOnRefreshListener {
            viewModel.refreshBlock()
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
        bannerAdView = AdView(this, getString(R.string.fb_banner_blocks), AdSize.BANNER_HEIGHT_50)
        bannerAdView?.let { nonNullBannerAdView ->
            adViewContainer?.addView(nonNullBannerAdView)
            nonNullBannerAdView.loadAd()
        }
    }

    companion object {
        fun open(context: Context, blockDate: String) {
            Intent(context, BlocksActivity::class.java).apply {
                putExtra("blockDate", blockDate)
                context.startActivity(this)
            }
        }
    }
}