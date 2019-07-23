package com.bitcoin.wallet.btc.ui.activitys

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.BuildConfig
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.ui.adapter.StoriesMoreAdapter
import com.bitcoin.wallet.btc.ui.widget.DividerItemDecoration
import com.bitcoin.wallet.btc.utils.Utils
import com.bitcoin.wallet.btc.viewmodel.StoryViewModel
import kotlinx.android.synthetic.main.activity_list.*

class StoryActivity : BaseActivity() {
    private val storiesMoreAdapter by lazy {
        StoriesMoreAdapter(
            retryCallback = {
                viewModel.retryNews()
            }, onClickNews = {
                it?.let {
                    try {
                        Utils.onOpenLink(
                            this,
                            it,
                            if (isDarkMode) R.color.colorPrimaryDarkTheme else R.color.colorPrimary
                        )
                    } catch (ex: Exception) {
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(it)
                                )
                            )
                        }catch (ex: ActivityNotFoundException) {
                            Toast.makeText(this, "You need to allow the browser to access website", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
    }

    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[StoryViewModel::class.java]
    }

    override fun layoutRes(): Int {
        return R.layout.activity_list
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar(getString(R.string.news))
        recyClear.apply {
            layoutManager = LinearLayoutManager(this@StoryActivity)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@StoryActivity, DividerItemDecoration.VERTICAL, false))
            adapter = storiesMoreAdapter
        }
        viewModel.newsData.observeNotNull(this) {
            storiesMoreAdapter.submitList(it)
        }
        viewModel.networkState.observeNotNull(this) {
            storiesMoreAdapter.onNetworkState(it)
        }
        viewModel.refreshState.observeNotNull(this) {
            freshlayout.isRefreshing = it == NetworkState.LOADING && freshlayout.isRefreshing
        }
        val baseId = intent?.getStringExtra("baseId")
        viewModel.onGetNews(baseId ?: "5b71fc48-3dd3-540c-809b-f8c94d0e68b5")

        freshlayout.setOnRefreshListener {
            viewModel.refreshNews()
        }
    }

    companion object {
        fun open(
            context: Context,
            baseId: String
        ) {
            Intent(context, StoryActivity::class.java).apply {
                putExtra("baseId", baseId)
                context.startActivity(this)
            }
        }
    }
}