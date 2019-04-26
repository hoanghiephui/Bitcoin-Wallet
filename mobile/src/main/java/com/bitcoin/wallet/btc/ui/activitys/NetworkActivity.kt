package com.bitcoin.wallet.btc.ui.activitys

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.data.AddressBookEntry
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.extension.visible
import com.bitcoin.wallet.btc.ui.adapter.BlockListAdapter
import com.bitcoin.wallet.btc.ui.adapter.PeerListAdapter
import com.bitcoin.wallet.btc.ui.widget.TopLinearLayoutManager
import com.bitcoin.wallet.btc.utils.Configuration
import com.bitcoin.wallet.btc.utils.Utils
import com.bitcoin.wallet.btc.viewmodel.NetworkViewModel
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.item_network_state.*
import org.bitcoinj.core.Sha256Hash

class NetworkActivity : BaseActivity(), BlockListAdapter.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private val viewModel: NetworkViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[NetworkViewModel::class.java]
    }
    private val blockAdapter: BlockListAdapter by lazy {
        BlockListAdapter(this, this)
    }
    private val peerAdapter: PeerListAdapter by lazy {
        PeerListAdapter()
    }
    val config: Configuration by lazy {
        application.config
    }

    override fun layoutRes(): Int {
        return R.layout.activity_network
    }
    private var isPeer = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar(getString(R.string.network_monitor))
        (segmented.getChildAt(0) as RadioButton).text = "BLOCK"
        (segmented.getChildAt(1) as RadioButton).text = "PEERS"
        loadingProgressBar.gone()
        recyclerView.apply {
            layoutManager = TopLinearLayoutManager(this@NetworkActivity)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@NetworkActivity, DividerItemDecoration.VERTICAL))
            adapter = blockAdapter
        }
        viewModel.transactions.observe(this, Observer {
            maybeSubmitList()
        })
        viewModel.blocks.observe(this, Observer {
            maybeSubmitList()
        })
        viewModel.time.observe(this, Observer {
            maybeSubmitList()
        })
        viewModel.wallet.observe(this, Observer {
            maybeSubmitList()
        })

        viewModel.peers.observe(this, Observer {
            maybeSubmitListPeer()
            if (it != null)
                for (peer in it)
                    viewModel.hostnames.reverseLookup(peer.address.addr)
        })
        viewModel.hostnames.observe(this, Observer {
            maybeSubmitListPeer()
        })

        segmented.setOnCheckedChangeListener(this)
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.btnOne -> {
                recyclerView.adapter = blockAdapter
                errorMessageTextView.gone()
                recyclerView.visible()
                isPeer = false
            }
            R.id.btnTwo -> {
                recyclerView.adapter = peerAdapter
                errorMessageTextView.visibility =
                    if (peerAdapter.itemCount == 0) View.VISIBLE else View.GONE
                recyclerView.visibility =
                    if (peerAdapter.itemCount != 0) View.VISIBLE else View.GONE
                isPeer = true
            }
        }
    }

    override fun onBlockMenuClick(view: View, blockHash: Sha256Hash) {
        try {
            Utils.onOpenLink(
                this,
                "https://www.blockchain.com/btc/block/$blockHash",
                if (isDarkMode) R.color.colorPrimaryDarkTheme else R.color.colorPrimary
            )
        } catch (ex: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.withAppendedPath(Uri.parse("https://www.blockchain.com/btc/"), "block/$blockHash")
                )
            )
        }
    }

    private fun maybeSubmitList() {
        val blocks = viewModel.blocks.value
        if (blocks != null) {
            val addressBook = AddressBookEntry.asMap(viewModel.addressBook.value)
            blockAdapter.submitList(
                viewModel.time.value?.let {
                    BlockListAdapter.buildListItems(
                        this,
                        blocks,
                        it,
                        config.format,
                        viewModel.transactions.value,
                        viewModel.wallet.value,
                        addressBook
                    )
                }
            )
        }
    }

    private fun maybeSubmitListPeer() {
        val peers = viewModel.peers.value
        if (peers != null)
            peerAdapter.submitList(viewModel.hostnames.value?.let {
                PeerListAdapter.buildListItems(
                    this, peers,
                    it
                )
            })
        errorMessageTextView.text = getString(R.string.peer_empty)
        errorMessageTextView.visibility =
            if ((peerAdapter.itemCount == 0 || peers == null) && isPeer) View.VISIBLE else View.GONE
    }
}