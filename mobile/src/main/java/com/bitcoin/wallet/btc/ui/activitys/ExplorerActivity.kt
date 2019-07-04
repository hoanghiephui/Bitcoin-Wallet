package com.bitcoin.wallet.btc.ui.activitys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.*
import com.bitcoin.wallet.btc.ui.adapter.explorer.ItemLatestBlockAdapter
import com.bitcoin.wallet.btc.viewmodel.ExplorerViewModel
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_explorer_bitcoin.*
import kotlinx.android.synthetic.main.init_ads.*

class ExplorerActivity : BaseActivity(), View.OnKeyListener {
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[ExplorerViewModel::class.java]
    }
    private val blockAdapter by lazy {
        ItemLatestBlockAdapter(
            retryCallback = {
                viewModel.retryLatestBlock()
            },
            onClickItem = {
                it?.let { it1 -> ExplorerDetailActivity.open(this, it1) }
            },
            viewAll = {
                BlocksActivity.open(this, currentDay)
            }
        )
    }
    private var bannerAdView: AdView? = null
    private var currentDay = ""
    private var adView: com.google.android.gms.ads.AdView? = null

    override fun layoutRes(): Int {
        return R.layout.activity_explorer_bitcoin
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Bitcoin Explorer")
        initRecyclerView()
        initViewModel()
        edtSearch.setOnKeyListener(this)
        loadAdView()
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KEYCODE_ENTER && edtSearch.getTextString().isNotEmpty()) {
            ExplorerDetailActivity.open(this, edtSearch.getTextString())
            edtSearch.text = null
        }
        return false
    }

    private fun initViewModel() {
        viewModel.latestBlocksData.observeNotNull(this) {
            blockAdapter.submitList(it.blocks)
            currentDay = it.pagination?.current ?: ""
        }
        viewModel.networkState.observeNotNull(this) {
            blockAdapter.onNetworkState(it)
        }
        viewModel.onGetLatestBlocks("15")
    }

    private fun initRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExplorerActivity)
            setHasFixedSize(true)
            adapter = blockAdapter
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                recyclerView.hideKeyboard()
            }
        })
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        adView?.destroy()
        adView= null
        super.onDestroy()
    }

    override fun onError(ad: Ad, error: AdError) {
        loadGoogleAdView()
        super.onError(ad, error)
    }

    private fun loadAdView() {
        bannerAdView?.destroy()
        bannerAdView = null
        bannerAdView = AdView(this, getString(R.string.fb_banner_explorer), AdSize.BANNER_HEIGHT_50)
        bannerAdView?.let { nonNullBannerAdView ->
            adViewContainer?.addView(nonNullBannerAdView)
            nonNullBannerAdView.setAdListener(this)
            nonNullBannerAdView.loadAd()
        }
    }

    private fun loadGoogleAdView() {
        adView?.destroy()
        adView = com.google.android.gms.ads.AdView(this)
        val adRequest = AdRequest.Builder().build()
        adView?.let {
            it.adSize = com.google.android.gms.ads.AdSize.BANNER
            it.adUnitId = getString(R.string.ads_banner_explorer)
            adViewContainer?.addView(it)
            it.adListener = object: com.google.android.gms.ads.AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    adViewContainer.visible()
                }

                override fun onAdFailedToLoad(p0: Int) {
                    super.onAdFailedToLoad(p0)
                    adViewContainer.gone()
                }
            }
            it.loadAd(adRequest)
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, ExplorerActivity::class.java))
        }
    }
}