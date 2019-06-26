package com.bitcoin.wallet.btc.ui.activitys

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.data.AddressBookDao
import com.bitcoin.wallet.btc.data.AppDatabase
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.extension.listenClickViews
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.extension.visible
import com.bitcoin.wallet.btc.ui.adapter.OnClickListener
import com.bitcoin.wallet.btc.ui.adapter.TransactionsWalletAdapter
import com.bitcoin.wallet.btc.ui.adapter.WarningType
import com.bitcoin.wallet.btc.ui.fragments.*
import com.bitcoin.wallet.btc.ui.widget.TopLinearLayoutManager
import com.bitcoin.wallet.btc.utils.*
import com.bitcoin.wallet.btc.viewmodel.WalletTransactionsViewModel
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_wallet_transaction.*
import kotlinx.android.synthetic.main.init_ads.*
import org.bitcoinj.core.Address
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.script.ScriptException

class WalletTransactionsActivity : BaseActivity(), OnClickListener, View.OnClickListener {
    val viewModel: WalletTransactionsViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletTransactionsViewModel::class.java)
    }
    private val addressBookDao: AddressBookDao by lazy {
        AppDatabase.getDatabase(this).addressBookDao()
    }
    private val devicePolicyManager: DevicePolicyManager by lazy {
        getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    private val transAdapter: TransactionsWalletAdapter by lazy {
        TransactionsWalletAdapter(this, this)
    }
    private val config: Configuration by lazy {
        application.config
    }
    private var bannerAdView: AdView? = null
    private var adView: com.google.android.gms.ads.AdView? = null

    override fun layoutRes(): Int {
        return R.layout.activity_wallet_transaction
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar(getString(R.string.transactions))
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = TopLinearLayoutManager(this@WalletTransactionsActivity)
            itemAnimator = TransactionsWalletAdapter.ItemAnimator()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                private val PADDING = 2 * resources.getDimensionPixelOffset(R.dimen.card_padding_vertical)

                override fun getItemOffsets(
                    outRect: Rect, view: View, parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)

                    val position = parent.getChildAdapterPosition(view)
                    if (position == 0)
                        outRect.top += PADDING
                    else if (position == parent.adapter!!.itemCount - 1)
                        outRect.bottom += PADDING
                }
            })
            adapter = transAdapter
        }
        viewModel.direction.observe(this, Observer {
            invalidateOptionsMenu()
        })
        viewModel.list.observe(this, Observer {
            transAdapter.submitList(it)
        })
        viewModel.transactions.observe(this, Observer {
            if (it.isEmpty()) {
                viewGroup.visible()

                val direction = viewModel.direction.value
                val emptyText = SpannableStringBuilder(
                    getString(
                        R.string.empty_text_received
                    )
                )
                emptyText.setSpan(
                    StyleSpan(Typeface.BOLD), 0, emptyText.length,
                    SpannableStringBuilder.SPAN_POINT_MARK
                )
                if (direction !== WalletTransactionsViewModel.Direction.SENT)
                    emptyText.append("\n\n")
                        .append(getString(R.string.empty_text_how))
                emptyView.text = emptyText
            } else {
                viewGroup.gone()
            }
        })

        viewModel.showBitmapDialog.observe(this, object : Event.Observer<Bitmap>() {
            override fun onEvent(bitmap: Bitmap) {
                BitmapBottomDialog.show(this@WalletTransactionsActivity, bitmap)
            }
        })

        viewModel.showEditAddressBookEntryDialog.observe(this, object : Event.Observer<Address>() {
            override fun onEvent(content: Address?) {
                content?.let { EditAddressBookEntryFragment.edit(supportFragmentManager, it) }
            }
        })
        viewModel.showReportIssueDialog.observe(this, object : Event.Observer<String>() {
            override fun onEvent(content: String?) {
                content?.let {
                    ReportIssueDialog.show(this@WalletTransactionsActivity, R.string.report_transaction,
                        R.string.report_issue_mes, "Reported issue", it
                    )
                }
            }

        })
        viewModel.showBackupWalletDialog.observeNotNull(this) {
            BackupDialog.show(this)
        }

        viewModel.backupWalletStatus.observe(this, object : Event.Observer<BackupDialog.BackUpStatus>() {
            override fun onEvent(content: BackupDialog.BackUpStatus?) {
                content?.let {
                    onShowSnackbar(
                        if (it.isStatus)
                            Html.fromHtml(getString(R.string.export_success, it.mes)).toString()
                        else getString(R.string.export_failure, it.mes)
                        , object : CallbackSnack {
                            override fun onOke() {

                            }
                        }, 10
                    )
                }
            }
        })
        viewModel.setDirection(null)
        listenClickViews(btnRequestCoin)
        loadAdView()
    }

    override fun onResume() {
        super.onResume()
        warning()?.let { viewModel.setWarning(it) }
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        adView?.destroy()
        adView = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        for (fragment in supportFragmentManager.fragments) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun warning(): WarningType? {
        val storageEncryptionStatus = devicePolicyManager.storageEncryptionStatus
        return if (config.remindBackup())
            WarningType.BACKUP
        else if (storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE || storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY)
            WarningType.STORAGE_ENCRYPTION
        else
            null
    }

    override fun onTransactionClick(view: View, transactionHash: Sha256Hash) {
        transactionHash.let { viewModel.setSelectedTransaction(it) }
    }

    override fun onTransactionMenuClick(view: View, transactionHash: Sha256Hash) {
        val wallet = viewModel.wallet.value
        val tx = wallet?.getTransaction(transactionHash)
        val txSent = tx!!.getValue(wallet).signum() < 0
        val txAddress = if (txSent)
            WalletUtils.getToAddressOfSent(tx, wallet)
        else
            WalletUtils.getWalletAddressOfReceived(tx, wallet)
        val txSerialized = tx.unsafeBitcoinSerialize()
        val txRotation = tx.purpose == Transaction.Purpose.KEY_ROTATION

        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_wallet_transactions)
        val editAddressMenuItem = popupMenu.menu
            .findItem(R.id.wallet_transactions_context_edit_address)
        if (!txRotation && txAddress != null) {
            editAddressMenuItem.isVisible = true
            val isAdd = addressBookDao.resolveLabel(txAddress.toString()) == null
            val isOwn = wallet.isAddressMine(txAddress)

            if (isOwn)
                editAddressMenuItem.setTitle(
                    if (isAdd)
                        R.string.edit_address_add_receive
                    else
                        R.string.edit_address_edit_receive
                )
            else
                editAddressMenuItem.setTitle(
                    if (isAdd)
                        R.string.edit_address_book_entry
                    else
                        R.string.edit_address
                )
        } else {
            editAddressMenuItem.isVisible = false
        }

        popupMenu.menu.findItem(R.id.wallet_transactions_context_show_qr).isVisible =
            !txRotation && txSerialized.size < SHOW_QR_THRESHOLD_BYTES
        popupMenu.menu.findItem(R.id.wallet_transactions_context_raise_fee).isVisible =
            RaiseFeeDialogFragment.feeCanLikelyBeRaised(wallet, tx)
        popupMenu.menu.findItem(R.id.wallet_transactions_context_browse).isVisible = true
        popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.wallet_transactions_context_edit_address -> {
                        viewModel.showEditAddressBookEntryDialog.value = Event<Address>(txAddress)
                        return true
                    }

                    R.id.wallet_transactions_context_show_qr -> {
                        val qrCodeBitmap = Qr.bitmap(Qr.encodeCompressBinary(txSerialized))
                        viewModel.showBitmapDialog.value = Event(qrCodeBitmap)
                        return true
                    }

                    R.id.wallet_transactions_context_raise_fee -> {
                        RaiseFeeDialogFragment.show(supportFragmentManager, tx)
                        return true
                    }

                    R.id.wallet_transactions_context_report_issue -> {
                        handleReportIssue(tx)
                        return true
                    }

                    R.id.wallet_transactions_context_browse -> {
                        if (!txRotation) {
                            val block = tx.txId.toString()
                            ExplorerDetailActivity.open(this@WalletTransactionsActivity, block)
                            //log.info("Viewing transaction {} on {}", tx.txId, blockExplorerUri) todo
                        }
                        return true
                    }
                    R.id.menu_confirm -> {
                        try {
                            Utils.onOpenLink(
                                this@WalletTransactionsActivity,
                                "https://www.buybitcoinworldwide.com/confirmations/",
                                if (isDarkMode) R.color.colorPrimaryDarkTheme else R.color.colorPrimary
                            )
                        } catch (ex: Exception) {
                            try {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.buybitcoinworldwide.com/confirmations/")
                                    )
                                )
                            } catch (ex: Exception) {
                                Toast.makeText(this@WalletTransactionsActivity, "Error the browse", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                return false
            }

            private fun handleReportIssue(tx: Transaction) {
                val contextualData = StringBuilder()
                try {
                    contextualData.append(tx.getValue(wallet).toFriendlyString()).append(" total value")
                } catch (x: ScriptException) {
                    contextualData.append(x.message)
                }

                contextualData.append('\n')
                if (tx.hasConfidence())
                    contextualData.append("  confidence: ").append(tx.confidence).append('\n')
                contextualData.append(tx.toString())

                viewModel.showReportIssueDialog.value = Event(contextualData.toString())
            }
        })
        popupMenu.show()
    }

    override fun onWarningClick(view: View) {
        when (warning()) {
            WarningType.BACKUP -> {
                viewModel.showBackupWalletDialog.value = Event.simple()
            }
            WarningType.STORAGE_ENCRYPTION -> {
                startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnRequestCoin -> {
                startActivity(Intent(this, RequestCoinActivity::class.java))
                finish()
            }
        }
    }

    override fun onError(ad: Ad, error: AdError) {
        super.onError(ad, error)
        loadGoogleAdView()
    }

    private fun loadAdView() {
        bannerAdView?.destroy()
        bannerAdView = null
        bannerAdView = AdView(this, getString(R.string.fb_banner_transaction), AdSize.BANNER_HEIGHT_50)
        bannerAdView?.let {nonNullBannerAdView ->
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
            it.adSize = com.google.android.gms.ads.AdSize.SMART_BANNER
            it.adUnitId = getString(R.string.ads_wallet_transactions)
            adViewContainer?.addView(it)
            it.loadAd(adRequest)
        }
    }

    companion object {
        const val SHOW_QR_THRESHOLD_BYTES = 2500
    }
}