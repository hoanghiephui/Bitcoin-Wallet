package com.bitcoin.wallet.btc.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.Constants.TOO_MUCH_BALANCE_THRESHOLD
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseFragment
import com.bitcoin.wallet.btc.data.PaymentIntent
import com.bitcoin.wallet.btc.extension.*
import com.bitcoin.wallet.btc.model.StatsResponse
import com.bitcoin.wallet.btc.repository.Status
import com.bitcoin.wallet.btc.repository.WalletRepository
import com.bitcoin.wallet.btc.service.BlockchainState
import com.bitcoin.wallet.btc.ui.activitys.*
import com.bitcoin.wallet.btc.ui.activitys.ScanActivity.Companion.REQUEST_CODE_SCAN
import com.bitcoin.wallet.btc.ui.adapter.StatsAdapter
import com.bitcoin.wallet.btc.utils.*
import com.bitcoin.wallet.btc.utils.CryptoCurrencies.Companion.getTextColor
import com.bitcoin.wallet.btc.viewmodel.WalletViewModel
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.bottom_bar_menu_layout.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.item_network_state.*
import org.bitcoinj.core.PrefixedChecksummedBytes
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.VerificationException
import org.bitcoinj.script.Script
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : BaseFragment(), View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private val behaviour by lazy { BottomSheetBehavior.from(bottomSheet) }
    private val viewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }
    private var fiatSymbol: String = "$"
    private var timeSpan = TimeSpan.MONTH
    private var cryptoCurrency = CryptoCurrencies.BTC
    private val buttonsList by lazy {
        listOf(
            textview_day,
            textview_week,
            textview_month,
            textview_year,
            tvHr
        )
    }
    private var statsAdapter by autoCleared<StatsAdapter>()
    private val BLOCKCHAIN_UPTODATE_THRESHOLD_MS = DateUtils.HOUR_IN_MILLIS
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
        baseActivity().setSupportActionBar(bottomBar)
        behaviour.setBottomSheetCallback({ state: Int ->
            when (state) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    bottomBar.navigationIcon = requireContext().getDrawableCompat(R.drawable.ic_clear)
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    bottomBar.navigationIcon = requireContext().getDrawableCompat(R.drawable.ic_menu)
                }
            }
        })
        statsAdapter = StatsAdapter()
        recyClear.apply {
            layoutManager = LinearLayoutManager(baseActivity())
            setHasFixedSize(true)
            adapter = statsAdapter
        }
        initChart()
        initViewBalance()
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
                                Html.fromHtml(getString(R.string.export_success, it.mes))
                            else getString(R.string.export_failure, it.mes),
                            Snackbar.LENGTH_INDEFINITE
                        ).apply {
                            setAction("OK") {
                                this.dismiss()
                            }
                            val text = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
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
                            SendCoinActivity.start(requireActivity(), key)
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

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        val externalStorageState = Environment.getExternalStorageState()
        val enableRestoreWalletOption =
            Environment.MEDIA_MOUNTED == externalStorageState || Environment.MEDIA_MOUNTED_READ_ONLY == externalStorageState
        menu?.findItem(R.id.menu_restore)?.isEnabled = enableRestoreWalletOption
        val isLegacyFallback = viewModel.walletLegacyFallback.value
        if (isLegacyFallback != null) {
            menu?.findItem(R.id.menu_legacy)?.isVisible = isLegacyFallback
        }
        val isEncrypted = (baseActivity() as MainActivity).viewModel.walletEncrypted.value
        if (isEncrypted != null) {
            val encryptKeysOption = menu?.findItem(R.id.menu_encrypt)
            encryptKeysOption?.setTitle(
                if (isEncrypted)
                    R.string.encrypt_change
                else
                    R.string.encrypt_keys_set
            )
            encryptKeysOption?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(requireActivity(), SettingActivity::class.java))

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
                //startActivity(Intent(this, SweepWalletActivity::class.java))

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
        }
        return true
    }

    override fun onBackPressed(): Boolean {
        return if (behaviour.state == BottomSheetBehavior.STATE_COLLAPSED) {
            true
        } else {
            behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            false
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnRequestBtc -> {
                viewModel.requestBitcoin.value = Event.simple()
            }
            R.id.btnSendBtc -> {
                viewModel.sendBitcoin.value = Event.simple()
            }
            R.id.tvWarrning -> {
                viewModel.showHelpDialog.setValue(Event(R.string.help_safety))
            }
            R.id.btnScan -> {
                ScanActivity.startForResult(baseActivity(), REQUEST_CODE_SCAN)
            }
            R.id.retryLoadingButton -> {
                viewModel.retryZipChart()
                viewModel.onGetStats(true)
            }
        }
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.btnBitcoin -> {
                cryptoCurrency = CryptoCurrencies.BTC
                updateChartsData(timeSpan, cryptoCurrency)
                showTimeSpanSelected(timeSpan)
                tvCurrency.text = getText(R.string.bitcoin_price)
                textview_price.text = "-----"
                textview_percentage.text = "---"
            }
            R.id.btnEth -> {
                cryptoCurrency = CryptoCurrencies.ETHER
                updateChartsData(timeSpan, cryptoCurrency)
                showTimeSpanSelected(timeSpan)
                tvCurrency.text = getText(R.string.ether_price)
                textview_price.text = "-----"
                textview_percentage.text = "---"
            }
            R.id.btnBch -> {
                cryptoCurrency = CryptoCurrencies.BCH
                updateChartsData(timeSpan, cryptoCurrency)
                showTimeSpanSelected(timeSpan)
                tvCurrency.text = getText(R.string.bch_price)
                textview_price.text = "-----"
                textview_percentage.text = "---"
            }
        }
    }

    private fun initViewBalance() {
        walletBalanceBtc.setPrefixScaleX(0.9f)
        wallet_balance_local.setInsignificantRelativeSize(1f)
        wallet_balance_local.setStrikeThru(false)
        walletBalanceBtc.setStrikeThru(false)
        viewBalance.setOnClickListener {
            startActivity(Intent(baseActivity(), ExchangeRatesActivity::class.java))
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
            text.append(Html.fromHtml("<b>" + getString(progressResId) + "</b>"))
        if (progressResId != 0 && showDisclaimer)
            text.append('\n')
        if (showDisclaimer)
            text.append(Html.fromHtml(getString(R.string.disclaimer_remind_safety)))
        tvWarrning.text = text
        if (text.isNotEmpty()) tvWarrning.visible() else tvWarrning.gone()
    }

    private fun updateViewWalletBalance() {
        val blockchainState = viewModel.blockchainState.value
        val balance = viewModel.balance.value
        val exchangeRate = viewModel.exchangeRate.value

        val showProgress: Boolean

        if (blockchainState?.bestChainDate != null) {
            val blockchainLag = System.currentTimeMillis() - blockchainState.bestChainDate.time
            val blockchainUptodate = blockchainLag < BLOCKCHAIN_UPTODATE_THRESHOLD_MS
            val noImpediments = blockchainState.impediments.isEmpty()

            showProgress = !(blockchainUptodate || !blockchainState.replaying)

            val downloading = getString(
                if (noImpediments)
                    R.string.progress_download
                else
                    R.string.progress_stalled
            )

            when {
                blockchainLag < 2 * DateUtils.DAY_IN_MILLIS -> {
                    val hours = blockchainLag / DateUtils.HOUR_IN_MILLIS
                    viewProgress.text = getString(R.string.progress_hours, downloading, hours)
                }
                blockchainLag < 2 * DateUtils.WEEK_IN_MILLIS -> {
                    val days = blockchainLag / DateUtils.DAY_IN_MILLIS
                    viewProgress.text = getString(R.string.progress_days, downloading, days)
                }
                blockchainLag < 90 * DateUtils.DAY_IN_MILLIS -> {
                    val weeks = blockchainLag / DateUtils.WEEK_IN_MILLIS
                    viewProgress.text = getString(R.string.progress_week, downloading, weeks)
                }
                else -> {
                    val months = blockchainLag / (30 * DateUtils.DAY_IN_MILLIS)
                    viewProgress.text = getString(R.string.progress_months, downloading, months)
                }
            }
        } else {
            showProgress = false
        }

        if (!showProgress) {
            viewBalance.visible()

            if (balance != null) {
                walletBalanceBtc.visible()
                walletBalanceBtc.setFormat(config.format)
                walletBalanceBtc.setAmount(balance)

                if (exchangeRate != null) {
                    val localValue = exchangeRate.rate.coinToFiat(balance)
                    wallet_balance_local.visible()
                    wallet_balance_local.setFormat(
                        Constants.LOCAL_FORMAT.code(
                            0,
                            Constants.PREFIX_ALMOST_EQUAL_TO + exchangeRate.currencyCode
                        )
                    )
                    wallet_balance_local.setAmount(localValue)
                    wallet_balance_local.alpha = 0.8f
                } else {
                    wallet_balance_local.invisible()
                }
            } else {
                walletBalanceBtc.invisible()
            }

            if (balance != null && balance.isGreaterThan(TOO_MUCH_BALANCE_THRESHOLD)) {
                viewBalanceWarning.visible()
                viewBalanceWarning.setText(R.string.balance_too_much)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                viewBalanceWarning.visible()
                viewBalanceWarning.setText(R.string.insecure_device)
            } else {
                viewBalanceWarning.gone()
            }

            viewProgress.gone()
            walletBalanceBtc.visible()
            wallet_balance_local.visible()
        } else {
            viewProgress.visible()
            viewBalance.invisible()
            walletBalanceBtc.invisible()
            wallet_balance_local.invisible()
        }
    }

    private fun initViewModelAddress() {
        viewModel.qrCode.observeNotNull(viewLifecycleOwner) {
            try {
                val qrDrawable = BitmapDrawable(resources, it)
                qrDrawable.isFilterBitmap = true
                currentAddressQrView.setImageDrawable(qrDrawable)

            } catch (ex: Exception) {
                Toast.makeText(baseActivity(), "Error show address, please try again!", Toast.LENGTH_SHORT).show()
            }
            currentAddressQrCardView.setOnClickListener {
                currentAddressQrView.isEnabled = false
                viewModel.showWalletAddressDialog.setValue(Event.simple())
            }
        }

        viewModel.showWalletAddressDialog.observeNotNull(viewLifecycleOwner) {
            val address = viewModel.currentAddress.value
            currentAddressQrView.isEnabled = true
            WalletAddressBottomDialog.show(baseActivity(), address, viewModel.ownName.value)
        }
        viewModel.showEncryptKeysDialog.observeNotNull(viewLifecycleOwner) {
            EncryptKeysDialogFragment.show(baseActivity())
        }
    }

    private fun initChart() {
        chart.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
                        .apply {
                            maximumFractionDigits = 0
                            roundingMode = RoundingMode.HALF_UP
                        }.format(value)}"
                }
            }
            axisLeft.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.isGranularityEnabled = true
            setExtraOffsets(8f, 0f, 0f, 10f)
            setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_gray_medium))
        }
    }

    private fun listenToDataChanges() {
        val loc = Locale.US
        val nf = NumberFormat.getCurrencyInstance(loc)
        val number = NumberFormat.getInstance(loc)
        viewModel.zipChart.observeNotNull(viewLifecycleOwner) {
            showChart(it)
        }
        viewModel.chartNetworkState.observeNotNull(viewLifecycleOwner) {
            errorMessageTextView.text = it.msg
            chart.visibility = if (it.status == Status.SUCCESS) View.VISIBLE else View.INVISIBLE
            loadingProgressBar.visibility = if (it.status == Status.RUNNING) View.VISIBLE else View.GONE
            retryLoadingButton.visibility = if (it.status == Status.FAILED) View.VISIBLE else View.INVISIBLE
        }
        viewModel.statsData.observeNotNull(viewLifecycleOwner) {
            addDataStats(it, number, nf)
        }
        viewModel.statNetworkState.observeNotNull(viewLifecycleOwner) {

        }
        showTimeSpanSelected(TimeSpan.MONTH)
        getMonthPrice(CryptoCurrencies.BTC, "USD")
        viewModel.onGetStats(true)
        initViewModelBalance()
        initViewModelAddress()
    }

    private fun updateChartsData(
        timeSpan: TimeSpan,
        cryptoCurrency: CryptoCurrencies
    ) {
        this.timeSpan = timeSpan
        when (timeSpan) {
            TimeSpan.ALL_TIME -> getAllTimePrice(cryptoCurrency, "USD")
            TimeSpan.YEAR -> getYearPrice(cryptoCurrency, "USD")
            TimeSpan.MONTH -> getMonthPrice(cryptoCurrency, "USD")
            TimeSpan.WEEK -> getWeekPrice(cryptoCurrency, "USD")
            TimeSpan.DAY -> getDayPrice(cryptoCurrency, "USD")
        }
    }

    private fun showTimeSpanSelected(timeSpan: TimeSpan) {
        selectButton(timeSpan)
        setDateFormatter(timeSpan)
    }

    private fun selectButton(timeSpan: TimeSpan) {
        when (timeSpan) {
            TimeSpan.ALL_TIME -> setTextViewSelected(tvHr)
            TimeSpan.YEAR -> setTextViewSelected(textview_year)
            TimeSpan.MONTH -> setTextViewSelected(textview_month)
            TimeSpan.WEEK -> setTextViewSelected(textview_week)
            TimeSpan.DAY -> setTextViewSelected(textview_day)
        }
    }

    private fun setTextViewSelected(selected: TextView) {
        with(selected) {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            this.setTextColor(ContextCompat.getColor(baseActivity(), R.color.colorAccent))
        }
        buttonsList.filterNot { it == selected }
            .map {
                with(it) {
                    paintFlags = paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                }
                it.setTextColor(getTextColor(baseActivity(), R.attr.colorNavigationActive))
            }
    }

    private fun setDateFormatter(timeSpan: TimeSpan) {
        val dateFormat = when (timeSpan) {
            TimeSpan.ALL_TIME -> SimpleDateFormat("yyyy", Locale.US)
            TimeSpan.YEAR -> SimpleDateFormat("MMM ''yy", Locale.US)
            TimeSpan.MONTH, TimeSpan.WEEK -> SimpleDateFormat("dd. MMM", Locale.US)
            TimeSpan.DAY -> SimpleDateFormat("H:00", Locale.US)
        }

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return dateFormat.format(Date(value.toLong() * 1000))
            }
        }

    }

    private fun updateArrow(arrow: ImageView, percentage: TextView, rotation: Float, @ColorRes color: Int) {
        arrow.visible()
        arrow.rotation = rotation
        arrow.setColorFilter(
            ContextCompat.getColor(arrow.context, color),
            PorterDuff.Mode.SRC_ATOP
        )
        percentage.setTextColor(ContextCompat.getColor(arrow.context, color))
    }


    private fun getAllTimePrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String) {
        getHistoricPrice(cryptoCurrency, fiatCurrency, TimeSpan.ALL_TIME)
    }

    private fun getYearPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String) {
        getHistoricPrice(cryptoCurrency, fiatCurrency, TimeSpan.YEAR)
    }

    private fun getMonthPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String) {
        getHistoricPrice(cryptoCurrency, fiatCurrency, TimeSpan.MONTH)
    }

    private fun getWeekPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String) {
        getHistoricPrice(cryptoCurrency, fiatCurrency, TimeSpan.WEEK)
    }

    private fun getDayPrice(cryptoCurrency: CryptoCurrencies, fiatCurrency: String) {
        getHistoricPrice(cryptoCurrency, fiatCurrency, TimeSpan.DAY)
    }
    //endregion

    private fun getHistoricPrice(
        cryptoCurrency: CryptoCurrencies,
        fiatCurrency: String,
        timeSpan: TimeSpan
    ) {
        val scale = when (timeSpan) {
            TimeSpan.ALL_TIME -> FIVE_DAYS
            TimeSpan.YEAR -> ONE_DAY
            TimeSpan.MONTH -> TWO_HOURS
            TimeSpan.WEEK -> ONE_HOUR
            TimeSpan.DAY -> FIFTEEN_MINUTES
        }

        HashMap<String, String>().apply {
            put("base", cryptoCurrency.symbol)
            put("quote", fiatCurrency)
            put("start", getStartTimeForTimeSpan(timeSpan, cryptoCurrency).toString())
            put("scale", scale.toString())
            put("api_key", API_KEY)
            viewModel.onGetZipDataChart(this)
        }
    }

    private fun showChart(it: WalletRepository.ZipPriceChart) {
        textview_price.text = "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
            .apply {
                maximumFractionDigits = 0
                roundingMode = RoundingMode.HALF_UP
            }.format(it.price["USD"]?.price)}"
        val first = it.dateChart.first()
        val last = it.dateChart.last()
        val difference = last.price - first.price
        val percentChange = (difference / first.price) * 100

        textview_percentage.text = "${String.format("%.1f", percentChange)}%"
        when {
            percentChange < 0 -> updateArrow(imageview_arrow, textview_percentage, 180f, R.color.product_red_medium)
            percentChange == 0.0 -> imageview_arrow.invisible()
            else -> updateArrow(imageview_arrow, textview_percentage, 0f, R.color.product_green_medium)
        }

        chart.apply {
            visible()
            val entries = it.dateChart.map { Entry(it.timestamp.toFloat(), it.price.toFloat()) }
            this.data = LineData(LineDataSet(entries, null).apply {
                color = ContextCompat.getColor(baseActivity(), R.color.colorAccent)
                lineWidth = 3f
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
                circleRadius = 1.5f
                setDrawCircleHole(false)
                setCircleColor(ContextCompat.getColor(baseActivity(), R.color.colorAccent))
                setDrawFilled(false)
                isHighlightEnabled = true
                setDrawHighlightIndicators(false)
                marker = ValueMarker(context, R.layout.item_chart)
            })

            animateX(500)
        }
    }

    private fun addDataStats(
        it: StatsResponse,
        number: NumberFormat,
        nf: NumberFormat
    ) {
        val itemList: MutableList<Any> = mutableListOf()
        itemList.add(getString(R.string.block_summary))
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.blocks_mineds),
                value = it.nBlocksMined.toString()
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.time_between_blocks),
                value = it.minutesBetweenBlocks.toString().plus(" minutes")
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.bitcoin_mined),
                value = number.format(it.nBtcMined).plus(" BTC")
            ))
        )

        itemList.add(getString(R.string.maket_summary))

        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.market_price),
                value = nf.format(it.marketPriceUsd)
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.tradeVolume),
                value = nf.format(it.tradeVolumeUsd)
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.tradeVolume),
                value = number.format(it.tradeVolumeBtc) + " BTC"
            ))
        )

        itemList.add(getString(R.string.transaction_summary))
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.total_tran),
                value = number.format(it.totalFeesBtc) + " BTC"
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.number_tran),
                value = number.format(it.nTx)
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.total_out),
                value = number.format(it.totalBtcSent) + " BTC"
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.estimated_tran),
                value = number.format(it.estimatedBtcSent) + " BTC"
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.estimated_usd),
                value = number.format(it.estimatedTransactionVolumeUsd) + " BTC"
            ))
        )

        itemList.add(getString(R.string.hash_rate))

        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.difficulty),
                value = number.format(it.difficulty)
            ))
        )
        itemList.add(
            (StatsAdapter.StatData(
                title =
                getString(R.string.hash_rates),
                value = number.format(it.hashRate) + " GH/s"
            ))
        )
        statsAdapter.itemList = itemList
        statsAdapter.notifyDataSetChanged()
    }

    private fun initClicks() {
        textview_day.setOnClickListener {
            updateChartsData(TimeSpan.DAY, cryptoCurrency)
            showTimeSpanSelected(TimeSpan.DAY)
        }
        textview_week.setOnClickListener {
            updateChartsData(TimeSpan.WEEK, cryptoCurrency)
            showTimeSpanSelected(TimeSpan.WEEK)
        }
        textview_month.setOnClickListener {
            updateChartsData(TimeSpan.MONTH, cryptoCurrency)
            showTimeSpanSelected(TimeSpan.MONTH)
        }
        textview_year.setOnClickListener {
            updateChartsData(TimeSpan.YEAR, cryptoCurrency)
            showTimeSpanSelected(TimeSpan.YEAR)
        }
        tvHr.setOnClickListener {
            updateChartsData(TimeSpan.ALL_TIME, cryptoCurrency)
            showTimeSpanSelected(TimeSpan.ALL_TIME)
        }

        segmented.setOnCheckedChangeListener(this)
        listenClickViews(btnRequestBtc, btnSendBtc, tvWarrning, btnScan, retryLoadingButton)
        navigationView.setNavigationItemSelectedListener {
            behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
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
            }
            return@setNavigationItemSelectedListener true
        }
        bottomBar.setNavigationOnClickListener {
            behaviour.apply {
                state = if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    companion object {
        const val TAG = "MainFragment"
        @JvmStatic
        fun getStartTimeForTimeSpan(timeSpan: TimeSpan, cryptoCurrency: CryptoCurrencies): Long {
            val start = when (timeSpan) {
                TimeSpan.ALL_TIME -> return getFirstMeasurement(cryptoCurrency)
                TimeSpan.YEAR -> 365
                TimeSpan.MONTH -> 30
                TimeSpan.WEEK -> 7
                TimeSpan.DAY -> 1
            }

            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -start) }
            return cal.timeInMillis / 1000
        }

        /**
         * Provides the first timestamp for which we have prices, returned in epoch-seconds
         *
         * @param cryptoCurrency The [CryptoCurrencies] that you want a start date for
         * @return A [Long] in epoch-seconds since the start of our data
         */
        private fun getFirstMeasurement(cryptoCurrency: CryptoCurrencies): Long {
            return when (cryptoCurrency) {
                CryptoCurrencies.BTC -> FIRST_BTC_ENTRY_TIME
                CryptoCurrencies.ETHER -> FIRST_ETH_ENTRY_TIME
                CryptoCurrencies.BCH -> FIRST_BCH_ENTRY_TIME
            }
        }

        const val API_KEY = "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3"
        const val FIFTEEN_MINUTES = 900
        const val ONE_HOUR = 3600
        const val TWO_HOURS = 7200
        const val ONE_DAY = 86400
        const val FIVE_DAYS = 432000
    }

    inner class ValueMarker(
        context: Context,
        layoutResource: Int
    ) : MarkerView(context, layoutResource) {

        private val date = findViewById<TextView>(R.id.textview_marker_date)
        private val price = findViewById<TextView>(R.id.textview_marker_price)

        private var mpPointF: MPPointF? = null

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        override fun refreshContent(e: Entry, highlight: Highlight) {
            date.text = SimpleDateFormat("E, MMM dd, HH:mm").format(Date(e.x.toLong() * 1000))
            price.text = "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
                .apply { maximumFractionDigits = 2 }
                .format(e.y)}"

            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            if (mpPointF == null) {
                // Center the marker horizontally and vertically
                mpPointF = MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
            }

            return mpPointF!!
        }
    }
}