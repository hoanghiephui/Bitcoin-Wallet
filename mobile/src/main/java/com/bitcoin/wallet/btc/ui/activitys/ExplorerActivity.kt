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
import com.bitcoin.wallet.btc.extension.getTextString
import com.bitcoin.wallet.btc.extension.hideKeyboard
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.ui.adapter.explorer.ItemLatestBlockAdapter
import com.bitcoin.wallet.btc.viewmodel.ExplorerViewModel
import kotlinx.android.synthetic.main.activity_explorer_bitcoin.*

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
    private var currentDay = ""

    override fun layoutRes(): Int {
        return R.layout.activity_explorer_bitcoin
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Bitcoin Explorer")
        initRecyclerView()
        initViewModel()
        edtSearch.setOnKeyListener(this)
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

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, ExplorerActivity::class.java))
        }
    }
}