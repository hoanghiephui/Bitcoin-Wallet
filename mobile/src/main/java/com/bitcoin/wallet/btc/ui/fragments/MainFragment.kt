package com.bitcoin.wallet.btc.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.CryptoCurrency
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.TimeSpan
import com.bitcoin.wallet.btc.base.BaseFragment
import com.bitcoin.wallet.btc.data.PaymentIntent
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.extension.listenClickViews
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.extension.visible
import com.bitcoin.wallet.btc.service.BlockchainState
import com.bitcoin.wallet.btc.ui.activitys.*
import com.bitcoin.wallet.btc.ui.activitys.ScanActivity.Companion.REQUEST_CODE_SCAN
import com.bitcoin.wallet.btc.ui.adapter.MainAdapter
import com.bitcoin.wallet.btc.utils.*
import com.bitcoin.wallet.btc.viewmodel.WalletViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.item_top_transaction.*
import org.bitcoinj.core.PrefixedChecksummedBytes
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.VerificationException
import org.bitcoinj.script.Script

class MainFragment : BaseFragment(), View.OnClickListener, MainAdapter.MainCallback {
    private val viewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }
    private var timeSpan = TimeSpan.DAY
    private var cryptoCurrency = CryptoCurrency.BTC

    private val mainAdapter by lazy {
        MainAdapter(this)
    }

    val config: Configuration by lazy {
        baseActivity().application.config
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config.touchLastUsed()
        setHasOptionsMenu(true)
    }

    override fun layoutRes(): Int {
        return R.layout.fragment_main
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        baseActivity().setSupportActionBar(toolbar)
        baseActivity().supportActionBar?.title = ""
        val toggle = ActionBarDrawerToggle(
            baseActivity(),
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        recyClear.apply {
            layoutManager = LinearLayoutManager(baseActivity())
            itemAnimator = null
            adapter = mainAdapter
        }
        listenToDataChanges()
        initClicks()
        viewModel.sendBitcoin.observeNotNull(viewLifecycleOwner) {
            startActivity(Intent(requireActivity(), SendCoinActivity::class.java))
        }

        viewModel.showRestoreWalletDialog.observeNotNull(viewLifecycleOwner) {
            RestoreWalletDialog.show(baseActivity())
        }
        viewModel.showBackupWalletDialog.observeNotNull(viewLifecycleOwner) {
            BackupDialog.show(baseActivity())
        }
        viewModel.backupWalletStatus.observeNotNull(viewLifecycleOwner) {
            object : Event.Observer<BackupDialog.BackUpStatus>() {
                override fun onEvent(content: BackupDialog.BackUpStatus?) {
                    content?.let {
                        Snackbar.make(
                            view.findViewById(android.R.id.content),
                            if (it.isStatus)
                                HtmlCompat.fromHtml(
                                    getString(R.string.export_success, it.mes),
                                    HtmlCompat.FROM_HTML_MODE_COMPACT
                                )
                            else getString(R.string.export_failure, it.mes),
                            Snackbar.LENGTH_INDEFINITE
                        ).apply {
                            setAction("OK") {
                                this.dismiss()
                            }
                            val text =
                                this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                            text.setLines(10)
                            val params = this.view.layoutParams
                            this.view.layoutParams = params
                        }.show()
                    }
                }
            }
        }
        viewModel.requestBitcoin.observeNotNull(viewLifecycleOwner) {
            startActivity(Intent(requireActivity(), RequestCoinActivity::class.java))
        }

        viewModel.showHelpDialog.observe(viewLifecycleOwner, object : Event.Observer<Int>() {
            override fun onEvent(content: Int?) {
                content?.let { HelpDialogFragment.show(baseActivity(), it) }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                val input = data?.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT)

                object : InputParser.StringInputParser(input) {
                    override fun handlePaymentIntent(paymentIntent: PaymentIntent) {
                        SendCoinActivity.start(requireActivity(), paymentIntent)
                    }

                    override fun handlePrivateKey(key: PrefixedChecksummedBytes) {
                        if (true)
                            SweepWalletActivity.start(baseActivity(), key)
                        else
                            super.handlePrivateKey(key)
                    }

                    @Throws(VerificationException::class)
                    override fun handleDirectTransaction(tx: Transaction) {
                        (baseActivity() as MainActivity).application.processDirectTransaction(tx)
                    }

                    override fun error(messageResId: Int, vararg messageArgs: Any) {
                        dialog(requireContext(), null, R.string.btn_scan, messageResId, messageArgs)
                    }
                }.parse()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return false
        } else {
            return true

        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val externalStorageState = Environment.getExternalStorageState()
        val enableRestoreWalletOption =
            Environment.MEDIA_MOUNTED == externalStorageState || Environment.MEDIA_MOUNTED_READ_ONLY == externalStorageState
        menu.findItem(R.id.menu_restore)?.isEnabled = enableRestoreWalletOption
        val isLegacyFallback = viewModel.walletLegacyFallback.value
        if (isLegacyFallback != null) {
            menu.findItem(R.id.menu_legacy)?.isVisible = isLegacyFallback
        }
        val isEncrypted = (baseActivity() as MainActivity).viewModel.walletEncrypted.value
        if (isEncrypted != null) {
            val encryptKeysOption = menu.findItem(R.id.menu_encrypt)
            encryptKeysOption?.setTitle(
                if (isEncrypted)
                    R.string.encrypt_change
                else
                    R.string.encrypt_keys_set
            )
            encryptKeysOption?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(requireActivity(), SettingActivity::class.java))

            }
            R.id.menu_about -> {
                startActivity(Intent(requireActivity(), AboutActivity::class.java))

            }
            R.id.nav_share -> {
            }
            R.id.menu_backup -> {
                viewModel.showBackupWalletDialog.value = Event.simple()

            }
            R.id.menu_restore -> {
                viewModel.showRestoreWalletDialog.value = Event.simple()

            }
            R.id.menu_legacy -> {
                RequestCoinActivity.start(requireActivity(), Script.ScriptType.P2PKH)

            }
            R.id.menu_sweep -> {
                SweepWalletActivity.start(baseActivity())

            }
            R.id.menu_encrypt -> {
                viewModel.showEncryptKeysDialog.value = Event.simple()
            }
            R.id.menu_transaction -> {
                startActivity(Intent(requireActivity(), WalletTransactionsActivity::class.java))
            }
            R.id.menu_network -> {
                startActivity(Intent(requireActivity(), NetworkActivity::class.java))
            }
            R.id.menu_address -> {
                startActivity(Intent(requireActivity(), AddressActivity::class.java))
            }
            R.id.menu_sub -> {
                MakePurchaseDialogFragment.show(baseActivity())
            }
            R.id.menu_explorer -> {
                if (WalletUtils.isPackageInstalled(
                        "com.blockchain.bitcoin.explorer",
                        baseActivity().packageManager
                    )
                ) {
                    val launchIntent =
                        baseActivity().packageManager.getLaunchIntentForPackage("com.blockchain.bitcoin.explorer")
                    startActivity(launchIntent)
                } else {
                    AlertDialog.Builder(baseActivity()).apply {
                        setMessage("You need to open Google Play")
                        setPositiveButton("OK") { dialog, _ ->
                            Intent(Intent.ACTION_VIEW).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                data = Uri.parse("market://details?id=" + "com.blockchain.bitcoin.explorer")
                                startActivity(this)
                            }
                            dialog.dismiss()
                        }
                        setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    }.create().show()
                }
                //ExplorerActivity.open(baseActivity())
            }
            R.id.menu_tools -> {
                ToolsActivity.open(baseActivity())
            }
        }
        return true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tvWarrning -> {
                viewModel.showHelpDialog.setValue(Event(R.string.help_safety))
            }
            R.id.retryLoadingButton -> {
                viewModel.retryZipChart()
            }
        }
    }

    override fun onClickExchange() {
        startActivity(Intent(baseActivity(), ExchangeRatesActivity::class.java))
    }

    override fun onClickSend() {
        viewModel.sendBitcoin.value = Event.simple()
    }

    override fun onClickRequest() {
        viewModel.requestBitcoin.value = Event.simple()
    }

    override fun onClickScanner() {
        ScanActivity.startForResult(baseActivity(), REQUEST_CODE_SCAN)
    }

    override fun onClickTimeChart(timeSpan: TimeSpan) {
        this.timeSpan = timeSpan
        onGetDataHome(timeSpan, cryptoCurrency)
    }

    override fun onClickSegmentChart(cryptoCurrency: CryptoCurrency) {
        this.cryptoCurrency = cryptoCurrency
        onGetDataHome(timeSpan, cryptoCurrency)
    }

    override fun onClickNews(url: String) {
        try {
            Utils.onOpenLink(
                baseActivity(),
                url,
                if (baseActivity().isDarkMode) R.color.colorPrimaryDarkTheme else R.color.colorPrimary
            )
        } catch (ex: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )
        }
    }

    override fun onClickRetry() {
        viewModel.retryZipChart()
    }

    override fun onClickMoreStory() {
        val baseId = getBaseId(cryptoCurrency)
        StoryActivity.open(baseActivity(), baseId)
    }

    override fun onClickMoreDiscover() {
    }

    override fun onClickBlocks(hash: String?, totalSend: Long?, weight: Long?) {
        hash?.let { ExplorerDetailActivity.open(baseActivity(), it) }
    }

    override fun onClickViewAllBlock() {
        BlocksActivity.open(baseActivity(), Utils.onGetDate("yyyy-MM-dd"))
    }

    private fun onGetDataHome(
        timeSpan: TimeSpan,
        cryptoCurrency: CryptoCurrency
    ) {
        val resolution = setDateFormatter(timeSpan)
        val baseId = getBaseId(cryptoCurrency)
        viewModel.onShowDataHome(
            WalletViewModel.RequestHome(
                baseId = baseId, base = "usd", resolution = setDateFormatter(timeSpan),
                urlInfo = "https://www.coinbase.com/api/v2/assets/info",
                urlNews = "https://www.coinbase.com/api/v2/news-articles?asset_id=$baseId&limit=6",
                urlSummary = "https://www.coinbase.com/api/v2/assets/summary?base=USD&resolution=$resolution${"&limit=10"}",
                cryptoCurrency = cryptoCurrency,
                fiatCurrency = "USD",
                timeSpan = timeSpan
            )
        )
    }

    private fun getBaseId(cryptoCurrency: CryptoCurrency): String {
        return when (cryptoCurrency) {
            CryptoCurrency.BTC -> "5b71fc48-3dd3-540c-809b-f8c94d0e68b5"
            CryptoCurrency.ETHER -> "d85dce9b-5b73-5c3c-8978-522ce1d1c1b4"
            CryptoCurrency.BCH -> "45f99e13-b522-57d7-8058-c57bf92fe7a3"
            CryptoCurrency.XLM -> "13b83335-5ede-595b-821e-5bcdfa80560f"
            CryptoCurrency.PAX -> ""
        }
    }

    private fun initViewModelBalance() {
        viewModel.blockchainState.observeNotNull(viewLifecycleOwner) {
            updateViewWalletBalance()
            updateViewDisclaimer()
        }
        viewModel.balance.observeNotNull(viewLifecycleOwner) {
            updateViewWalletBalance()
        }
        viewModel.exchangeRate.observeNotNull(viewLifecycleOwner) {
            updateViewWalletBalance()
        }
        viewModel.disclaimerEnabled.observeNotNull(viewLifecycleOwner) {
            updateViewDisclaimer()
        }
    }

    private fun updateViewDisclaimer() {
        val showDisclaimer = viewModel.disclaimerEnabled.value ?: false
        val blockchainState = viewModel.blockchainState.value
        var progressResId = 0
        if (blockchainState != null) {
            val impediments = blockchainState.impediments
            if (impediments.contains(BlockchainState.Impediment.STORAGE))
                progressResId = R.string.problem_storage
            else if (impediments.contains(BlockchainState.Impediment.NETWORK))
                progressResId = R.string.progress_problem_network
        }

        val text = SpannableStringBuilder()
        if (progressResId != 0)
            text.append(
                HtmlCompat.fromHtml(
                    "<b>" + getString(progressResId) + "</b>",
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
        if (progressResId != 0 && showDisclaimer)
            text.append('\n')
        if (showDisclaimer)
            text.append(
                HtmlCompat.fromHtml(
                    getString(R.string.disclaimer_remind_safety),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
        tvWarrning.text = text
        if (text.isNotEmpty()) tvWarrning.visible() else tvWarrning.gone()
    }

    private fun updateViewWalletBalance() {
        val blockchainState = viewModel.blockchainState.value
        val balance = viewModel.balance.value
        val exchangeRate = viewModel.exchangeRate.value
        mainAdapter.addWalletBance(config.format, exchangeRate, blockchainState, balance)
    }

    private fun initViewModelAddress() {
        viewModel.qrCode.observeNotNull(viewLifecycleOwner) {
            currentAddressQrCardView.setOnClickListener {
                viewModel.showWalletAddressDialog.setValue(Event.simple())
            }
        }

        viewModel.showWalletAddressDialog.observeNotNull(viewLifecycleOwner) {
            val address = viewModel.currentAddress.value
            WalletAddressBottomDialog.show(baseActivity(), address, viewModel.ownName.value)
        }
        viewModel.showEncryptKeysDialog.observeNotNull(viewLifecycleOwner) {
            EncryptKeysDialogFragment.show(baseActivity())
        }
    }

    private fun listenToDataChanges() {
        viewModel.zipHomeResult.observeNotNull(viewLifecycleOwner) {
            mainAdapter.zipHomeData = it
            mainAdapter.timeSpan = timeSpan
            mainAdapter.cryptoCurrency = cryptoCurrency
            mainAdapter.notifyItemRangeChanged(1, mainAdapter.itemCount)
            viewModel.onGetLatestBlocks()
        }
        viewModel.zipHomeNetworkState.observeNotNull(viewLifecycleOwner) {
            mainAdapter.networkState = it
            if (it.msg != null) {
                mainAdapter.notifyItemRangeChanged(1, mainAdapter.itemCount)
            }
        }
        viewModel.blockResult.observeNotNull(viewLifecycleOwner) {
            mainAdapter.addBlocks(it.blocks)
        }
        viewModel.blockNetworkState.observeNotNull(viewLifecycleOwner) {

        }
        onGetDataHome(timeSpan, cryptoCurrency)
        initViewModelBalance()
        initViewModelAddress()
    }

    private fun setDateFormatter(timeSpan: TimeSpan): String {
        return when (timeSpan) {
            TimeSpan.ALL_TIME -> "all"
            TimeSpan.YEAR -> "year"
            TimeSpan.MONTH -> "month"
            TimeSpan.DAY -> "day"
            TimeSpan.WEEK -> "week"
        }
    }

    private fun initClicks() {
        listenClickViews(tvWarrning)
        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(requireActivity(), SettingActivity::class.java))

                }
                R.id.menu_address -> {
                    startActivity(Intent(requireActivity(), AddressActivity::class.java))

                }
                R.id.menu_network -> {
                    startActivity(Intent(requireActivity(), NetworkActivity::class.java))

                }
                R.id.menu_transaction -> {
                    startActivity(Intent(requireActivity(), WalletTransactionsActivity::class.java))
                }
                R.id.menu_exchange -> {
                    startActivity(Intent(requireActivity(), ExchangeRatesActivity::class.java))
                }
                R.id.menu_tools -> {
                    ToolsActivity.open(baseActivity())
                }
            }
            return@setNavigationItemSelectedListener true
        }
    }

    companion object {
        const val TAG = "MainFragment"
    }
}