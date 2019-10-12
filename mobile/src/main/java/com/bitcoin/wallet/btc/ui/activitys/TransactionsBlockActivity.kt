package com.bitcoin.wallet.btc.ui.activitys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.Constants.API_KEY
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.model.SummaryModel
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.ui.adapter.TransactionsBlockAdapter
import com.bitcoin.wallet.btc.viewmodel.TransactionViewModel
import kotlinx.android.synthetic.main.activity_list.*

class TransactionsBlockActivity : BaseActivity() {
    private val transactionsBlockAdapter by lazy {
        TransactionsBlockAdapter(retryCallback = {
            viewModel.retryTransactions()
        })
    }
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[TransactionViewModel::class.java]
    }

    override fun layoutRes(): Int {
        return R.layout.activity_list
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar(getString(R.string.transactions))
        recyClear.apply {
            layoutManager = LinearLayoutManager(this@TransactionsBlockActivity)
            adapter = transactionsBlockAdapter
        }
        val weight = intent.getLongExtra("weight", 0L).toString()
        val totalSend = intent.getLongExtra("totalSend", 0L).toString()
        viewModel.transactionData.observeNotNull(this) {
            val listItem: MutableList<Any> = mutableListOf()
            val listSummary: MutableList<SummaryModel> = mutableListOf()
            listItem.add("Block #".plus(it.height))
            listItem.add("BlockHash ".plus(it.hash))
            listItem.add("Summary")
            listSummary.apply {
                add(SummaryModel("Number Of Transactions", it.nTx?.toString()))
                add(
                    SummaryModel(
                        "Height",
                        it.height?.toString()?.plus(if (it.mainChain == true) " (Mainchain)" else "")
                    )
                )
                add(SummaryModel("Output Total", totalSend.plus(" BTC")))
                add(SummaryModel("Timestamp", it.time?.toString()))
                //add(SummaryModel("Mined by", it.poolInfo?.poolName ?: ""))
                add(SummaryModel("Merkle Root", it.mrklRoot))
                add(SummaryModel("Previous Block", it.height?.minus(1)?.toString()))
                add(SummaryModel("Weight", weight.plus(" kWU")))
                add(SummaryModel("Bits", it.bits?.toString()))
                add(SummaryModel("Size (bytes)", it.size?.toString()))
                add(SummaryModel("Version", it.ver?.toString()))
                add(SummaryModel("Nonce", it.nonce?.toString()))
                add(
                    SummaryModel(
                        "Next Block",
                        if (it.nextBlock != null) it.height?.plus(1)?.toString() else "N/A"
                    )
                )
                listItem.addAll(listSummary)
                listItem.add(getString(R.string.transactions))
                it.tx?.let { it1 -> listItem.addAll(it1) }
                transactionsBlockAdapter.submitList(listItem)
            }
        }
        viewModel.networkState.observeNotNull(this) {
            transactionsBlockAdapter.onNetworkState(it)
            freshlayout.isRefreshing = freshlayout.isRefreshing && it == NetworkState.LOADING
        }
        loadApi()
        freshlayout.setOnRefreshListener {
            loadApi()
        }
    }

    private fun loadApi() {
        val hash = intent.getStringExtra("hash")
        viewModel.onGetTransactions("https://blockchain.info/rawblock/$hash?api_key=$API_KEY")
    }

    companion object {
        fun open(
            context: Context, hash: String?,
            weight: Long?, totalSend: Long?
        ) {
            Intent(context, TransactionsBlockActivity::class.java).apply {
                putExtra("hash", hash)
                putExtra("weight", weight)
                putExtra("totalSend", totalSend)
                context.startActivity(this)
            }
        }
    }
}