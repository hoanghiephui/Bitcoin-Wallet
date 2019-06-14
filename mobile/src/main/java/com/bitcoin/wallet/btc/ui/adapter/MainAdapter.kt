package com.bitcoin.wallet.btc.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Build
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.CryptoCurrency
import com.bitcoin.wallet.btc.CryptoCurrency.Companion.getTextColor
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.TimeSpan
import com.bitcoin.wallet.btc.api.ZipHomeData
import com.bitcoin.wallet.btc.base.BaseFragment
import com.bitcoin.wallet.btc.data.ExchangeRate
import com.bitcoin.wallet.btc.extension.*
import com.bitcoin.wallet.btc.model.blocks.BlocksItem
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.service.BlockchainState
import com.bitcoin.wallet.btc.ui.widget.DividerItemDecoration
import com.bitcoin.wallet.btc.utils.Utils
import com.facebook.ads.NativeAd
import com.facebook.ads.NativeAdView
import com.facebook.ads.NativeAdViewAttributes
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils.convertDpToPixel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.init_ads.*
import kotlinx.android.synthetic.main.item_network_state.*
import kotlinx.android.synthetic.main.item_top.*
import kotlinx.android.synthetic.main.item_top_chart.*
import kotlinx.android.synthetic.main.item_top_wallet.*
import org.bitcoinj.core.Coin
import org.bitcoinj.utils.MonetaryFormat
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class MainAdapter(private val callback: MainCallback) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var zipHomeData: ZipHomeData? = null
    private var format: MonetaryFormat? = null
    private var exchangeRate: ExchangeRate? = null
    private var blockchainState: BlockchainState? = null
    private var balance: Coin? = null
    private val BLOCKCHAIN_UPTODATE_THRESHOLD_MS = DateUtils.HOUR_IN_MILLIS
    private val loc = Locale.US
    private val nf = NumberFormat.getCurrencyInstance(loc).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val number: NumberFormat = NumberFormat.getInstance(loc).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private var fiatSymbol: String = "$"
    var timeSpan = TimeSpan.DAY
    var cryptoCurrency = CryptoCurrency.BTC
    var networkState = NetworkState.LOADING
    private var mNativeAd: NativeAd? = null
    private var blocksItem: List<BlocksItem>? = null
    private var mHighlightedValues: Array<Highlight> = arrayOf()

    fun onLoadAds(mNativeAd: NativeAd?) {
        this.mNativeAd = mNativeAd
        notifyItemChanged(2)
    }

    fun addWalletBance(
        format: MonetaryFormat?,
        exchangeRate: ExchangeRate?,
        blockchainState: BlockchainState?,
        balance: Coin?
    ) {
        this.format = format
        this.exchangeRate = exchangeRate
        this.blockchainState = blockchainState
        this.balance = balance
        notifyItemChanged(0)
    }

    fun addBlocks(blocksItem: List<BlocksItem>?) {
        this.blocksItem = blocksItem
        notifyItemChanged(3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_top_wallet -> TopWalletViewHolder(parent.inflate(R.layout.item_top_wallet), callback)
            R.layout.item_top -> TopViewHolder(parent.inflate(R.layout.item_top), callback)
            R.layout.item_news -> TopStoriesViewHolder(parent.inflate(R.layout.item_top), callback)
            R.layout.init_ads -> AdsViewHolder(parent.inflate(R.layout.init_ads))
            R.layout.item_last_block -> BlockViewHolder(parent.inflate(R.layout.item_top), callback)
            else -> ChartViewHolder(parent.inflate(R.layout.item_top_chart), fiatSymbol, callback)
        }
    }

    override fun getItemCount(): Int {
        return 5 + if (blocksItem != null) 1 else 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = holder.itemView.context
        when (getItemViewType(position)) {
            R.layout.item_top_wallet -> {
                if (holder is TopWalletViewHolder) {
                    var showProgress = false
                    if (blockchainState != null && blockchainState?.bestChainDate != null) {
                        val blockchainLag = System.currentTimeMillis() - blockchainState?.bestChainDate?.time!!
                        val blockchainUptodate = blockchainLag < BLOCKCHAIN_UPTODATE_THRESHOLD_MS
                        val noImpediments = blockchainState?.impediments?.isEmpty()
                        blockchainState?.let {
                            showProgress = !(blockchainUptodate || !it.replaying)
                        }

                        val downloading = context.getString(
                            if (noImpediments == true)
                                R.string.progress_download
                            else
                                R.string.progress_stalled
                        )

                        when {
                            blockchainLag < 2 * DateUtils.DAY_IN_MILLIS -> {
                                val hours = blockchainLag / DateUtils.HOUR_IN_MILLIS
                                holder.viewProgress.text =
                                    context.getString(R.string.progress_hours, downloading, hours)
                            }
                            blockchainLag < 2 * DateUtils.WEEK_IN_MILLIS -> {
                                val days = blockchainLag / DateUtils.DAY_IN_MILLIS
                                holder.viewProgress.text = context.getString(R.string.progress_days, downloading, days)
                            }
                            blockchainLag < 90 * DateUtils.DAY_IN_MILLIS -> {
                                val weeks = blockchainLag / DateUtils.WEEK_IN_MILLIS
                                holder.viewProgress.text = context.getString(R.string.progress_week, downloading, weeks)
                            }
                            else -> {
                                val months = blockchainLag / (30 * DateUtils.DAY_IN_MILLIS)
                                holder.viewProgress.text =
                                    context.getString(R.string.progress_months, downloading, months)
                            }
                        }
                    } else {
                        showProgress = false
                    }

                    if (!showProgress) {
                        holder.viewBalance.visible()

                        if (balance != null) {
                            holder.walletBalanceBtc.visible()
                            holder.walletBalanceBtc.setFormat(format)
                            holder.walletBalanceBtc.setAmount(balance!!)

                            if (exchangeRate != null) {
                                val localValue = exchangeRate?.rate?.coinToFiat(balance)
                                holder.wallet_balance_local.visible()
                                holder.wallet_balance_local.setFormat(
                                    Constants.LOCAL_FORMAT.code(
                                        0,
                                        Constants.PREFIX_ALMOST_EQUAL_TO + exchangeRate?.currencyCode
                                    )
                                )
                                localValue?.let { holder.wallet_balance_local.setAmount(it) }
                                holder.wallet_balance_local.alpha = 0.8f
                            } else {
                                holder.wallet_balance_local.invisible()
                            }
                        } else {
                            holder.walletBalanceBtc.invisible()
                        }

                        if (balance != null && balance?.isGreaterThan(Constants.TOO_MUCH_BALANCE_THRESHOLD) == true) {
                            holder.viewBalanceWarning.visible()
                            holder.viewBalanceWarning.setText(R.string.balance_too_much)
                        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            holder.viewBalanceWarning.visible()
                            holder.viewBalanceWarning.setText(R.string.insecure_device)
                        } else {
                            holder.viewBalanceWarning.gone()
                        }

                        holder.viewProgress.gone()
                        holder.walletBalanceBtc.visible()
                        holder.wallet_balance_local.visible()
                    } else {
                        holder.viewProgress.visible()
                        holder.viewBalance.invisible()
                        holder.walletBalanceBtc.invisible()
                        holder.wallet_balance_local.invisible()
                    }
                }
            }
            R.layout.item_top_chart -> {
                if (holder is ChartViewHolder) {
                    holder.apply {
                        onSetAbout()
                        onSetStats()
                        onSetSummary()
                        onSetChart(context)

                        if (networkState.msg != null) {
                            loadingProgressBar.gone()
                            errorMessageTextView.text = networkState.msg
                            errorMessageTextView.visible()
                            retryLoadingButton.visible()
                        } else {
                            errorMessageTextView.gone()
                            retryLoadingButton.gone()
                        }
                    }
                }
            }
            R.layout.item_top -> {
                if (holder is TopViewHolder) {
                    holder.apply {
                        discoverAdapter.list = zipHomeData?.infoResponse?.data?.subList(0, 10) ?: mutableListOf()
                        discoverAdapter.submitList(zipHomeData?.summaryResponse?.data?.subList(0, 10))
                        if (zipHomeData?.infoResponse?.data != null) {
                            cardViewMore.visible()
                        } else {
                            cardViewMore.gone()
                        }
                    }
                }
            }
            R.layout.item_news -> {
                if (holder is TopStoriesViewHolder) {
                    holder.apply {
                        storiesAdapter.submitList(zipHomeData?.newsResponse?.data)
                        if (zipHomeData?.newsResponse?.data != null) {
                            cardViewMore.visible()
                        } else {
                            cardViewMore.gone()
                        }
                    }
                }
            }
            R.layout.init_ads -> {
                if (holder is AdsViewHolder) {
                    holder.apply {
                        mNativeAd?.let {
                            val mNativeAdContainer = adViewContainer
                            mNativeAdContainer?.removeAllViews()

                            // Create a NativeAdViewAttributes object and set the attributes
                            val attributes = NativeAdViewAttributes(itemView.context)
                                .setBackgroundColor(itemView.context.getColorFromAttr(R.attr.colorPrimary))
                                .setTitleTextColor(itemView.context.getColorFromAttr(R.attr.colorBgItemDrawer))
                                .setDescriptionTextColor(itemView.context.getColorFromAttr(R.attr.colorMenu))
                                .setButtonBorderColor(itemView.context.getColorFromAttr(R.attr.colorAccent))
                                .setButtonTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                                .setButtonColor(itemView.context.getColorFromAttr(R.attr.colorAccent))

                            // Use NativeAdView.render to generate the ad View
                            val mAdView = NativeAdView.render(itemView.context, it, attributes)

                            mNativeAdContainer?.addView(
                                mAdView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    (Resources.getSystem().displayMetrics.density * BaseFragment.DEFAULT_HEIGHT_DP).toInt()
                                )
                            )
                        }
                        adViewContainer.isVisible = mNativeAd != null
                    }
                }
            }
            R.layout.item_last_block -> {
                if (holder is BlockViewHolder) {
                    holder.cardViewMore.isVisible = blocksItem != null
                    holder.blocksAdapter.submitList(blocksItem)
                }
            }
        }
    }

    private fun ChartViewHolder.onSetChart(context: Context) {
        showTimeSpanSelected(timeSpan)
        showSegmentCoin(cryptoCurrency)
        //chart
        val data = zipHomeData?.priceResponse
        data?.let {
            chart.visible()
            cardAbout.visible()
            loadingProgressBar.gone()
            val entries = it.map { priceDatum ->
                Entry(priceDatum.timestamp.toFloat(), priceDatum.price.toFloat())
            }
            val set1 = LineDataSet(entries, null).apply {
                color = ContextCompat.getColor(context, R.color.colorAccent)
                lineWidth = 3f
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
                circleRadius = 1.5f
                setDrawCircleHole(false)
                setCircleColor(ContextCompat.getColor(context, R.color.colorAccent))
                setDrawFilled(false)
                isHighlightEnabled = true
                setDrawHorizontalHighlightIndicator(false)
                setDrawHighlightIndicators(false)
                enableDashedHighlightLine(10f, 5f, 0f)
            }
            chart.marker = ValueMarker(context, R.layout.item_chart)
            chart.legend.form = Legend.LegendForm.LINE
            chart.data = LineData(set1)
            chart.animateX(500)
            val leftAxis = chart.axisLeft
            leftAxis.removeAllLimitLines()
            val yMin = chart.yMin
            val yMax = chart.yMax
            val step = (yMax - yMin) / 4.0f
            for (i in 1..3) {
                val stepLine = LimitLine(yMin + i.toFloat() * step)
                styleStepLine(stepLine, 50, context)
                leftAxis.addLimitLine(stepLine)
            }
            val yMinLine = LimitLine(yMin, "Low: ${nf.format(yMin)}")
            styleLimitLine(yMinLine, ContextCompat.getColor(context, R.color.colorAccent))
            leftAxis.addLimitLine(yMinLine)
            val yMaxLine = LimitLine(yMax, "High: ${nf.format(yMax)}").apply {
                labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
            }
            styleLimitLine(yMaxLine, ContextCompat.getColor(context, R.color.colorAccent))
            leftAxis.addLimitLine(yMaxLine)
            setMinMaxMarkers()
            showMinMaxPriceMarkers()
            buttonsList.forEach { textView ->
                textView.isEnabled = true
            }
            btnBitcoin.isEnabled = true
            btnBch.isEnabled = true
            btnXlm.isEnabled = true
            btnEth.isEnabled = true
        }
    }

    private fun ChartViewHolder.showMinMaxPriceMarkers() {
        chart.setDrawMarkers(true)
        chart.highlightValues(mHighlightedValues)
    }

    private fun ChartViewHolder.getHilights(): MutableList<Highlight> {
        val highlights: MutableList<Highlight> = mutableListOf()
        val chartData = chart.data
        for (item: Int in 0..chartData.dataSetCount) {
            val xMin = chartData.getDataSetByIndex(item).xMin
            val xMax = chartData.getDataSetByIndex(item).xMax
            val yMin = chartData.getDataSetByIndex(item).yMin
            val yMax = chartData.getDataSetByIndex(item).yMax
            for (j: Int in xMin.toInt()..(xMax + 1).toInt()) {
                val y = chartData.getDataSetByIndex(item).getEntryForXValue(j.toFloat(), Float.NaN).y
                if (y == yMin || y == yMax) {
                    highlights.add(Highlight (j.toFloat(), y, item))
                }
            }
        }
        return highlights
    }

    private fun ChartViewHolder.setMinMaxMarkers() {
        mHighlightedValues = Array(2) { Highlight(chart.data.xMin, chart.data.yMin, 0); Highlight(chart.data.xMax, chart.data.yMax, 0) }
    }

    private fun ChartViewHolder.onSetSummary() {
        zipHomeData?.summaryResponse?.data?.forEach {
            if (it.base == cryptoCurrency.symbol) {
                it.latest?.toDouble()?.let { latest ->
                    textview_price.text = nf.format(latest)
                }
                it.percentChange?.let { percentChange ->
                    if (Utils.isNegative(percentChange)) {
                        textview_percentage.text = number.format(percentChange * 100).plus("%")
                    } else {
                        textview_percentage.text =
                            "+".plus(number.format(percentChange * 100).plus("%"))
                    }
                    when {
                        percentChange < 0 -> updateArrow(
                            imageview_arrow,
                            textview_percentage,
                            180f,
                            R.color.product_red_medium
                        )
                        percentChange == 0.0 -> imageview_arrow.invisible()
                        else -> updateArrow(
                            imageview_arrow,
                            textview_percentage,
                            0f,
                            R.color.product_green_medium
                        )
                    }
                }
                return@forEach
            }
        }
    }

    private fun ChartViewHolder.onSetStats() {
        zipHomeData?.statsResponse?.data?.let {
            it.marketCap?.toDouble()?.let { cap ->
                txtMarketCap.text = nf.format(cap)
            }
            it.volume24h?.toDouble()?.let { volume ->
                txtVolume.text = nf.format(volume)
            }
            it.allTimeHigh?.toDouble()?.let { all ->
                txtAllTime.text = nf.format(all)
            }
            it.circulatingSupply?.toDouble()?.let { data ->
                txtSupply.text = number.format(data).plus(" ".plus(it.base))
            }
        }
    }

    private fun ChartViewHolder.onSetAbout() {
        zipHomeData?.infoResponse?.data?.let {
            if (it.isNotEmpty()) {
                it.forEach { data ->
                    if (data.symbol == cryptoCurrency.symbol) {
                        txtTileCoin.text = "About ".plus(data.name)
                        txtAbout.text = data.description
                        return@forEach
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (blocksItem != null) {
            return when (position) {
                0 -> R.layout.item_top_wallet
                1 -> R.layout.item_top_chart
                2 -> R.layout.init_ads
                3 -> R.layout.item_last_block
                4 -> R.layout.item_top
                else -> R.layout.item_news
            }
        } else {
            return when (position) {
                0 -> R.layout.item_top_wallet
                1 -> R.layout.item_top_chart
                2 -> R.layout.init_ads
                3 -> R.layout.item_top
                else -> R.layout.item_news
            }
        }
    }

    private fun styleStepLine(limitLine: LimitLine, alpha: Int, context: Context) {
        styleLimitLine(
            limitLine,
            ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.colorAccent), alpha)
        )
    }

    private fun styleLimitLine(limitLine: LimitLine, lineColor: Int) {
        limitLine.lineColor = lineColor
        limitLine.lineWidth = 0.6f
        limitLine.enableDashedLine(5.0f, 5.0f, 0.0f)
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
            price.text = "$fiatSymbol${number
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

    class TopWalletViewHolder(
        itemView: View,
        private val callback: MainCallback
    ) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView

        init {
            initViewBalance()
        }

        private fun initViewBalance() {
            walletBalanceBtc.setPrefixScaleX(0.9f)
            wallet_balance_local.setInsignificantRelativeSize(1f)
            wallet_balance_local.setStrikeThru(false)
            walletBalanceBtc.setStrikeThru(false)
            viewBalance.setOnClickListener {
                callback.onClickExchange()
            }
            btnRequestBtc.setOnClickListener {
                callback.onClickRequest()
            }
            btnSendBtc.setOnClickListener {
                callback.onClickSend()
            }
            btnScan.setOnClickListener {
                callback.onClickScanner()
            }

        }
    }

    class TopViewHolder(
        itemView: View,
        private val callback: MainCallback
    ) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        val discoverAdapter by lazy {
            DiscoverAdapter()
        }
        override val containerView: View?
            get() = itemView

        init {
            txtTitle.text = "Discover More Assets"
            txtViewMore.text = "View all assets"
            recyClear.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL, false))
                adapter = discoverAdapter
            }
            txtViewMore.gone()
            txtViewMore.setOnClickListener {
                callback.onClickMoreDiscover()
            }
        }
    }

    class ChartViewHolder(
        itemView: View,
        private val fiatSymbol: String,
        private val callback: MainCallback
    ) : RecyclerView.ViewHolder(itemView), LayoutContainer, View.OnClickListener {
        override val containerView: View?
            get() = itemView

        val buttonsList by lazy {
            listOf(
                itemView.findViewById<TextView>(R.id.tvDay),
                itemView.findViewById(R.id.tvWeek),
                itemView.findViewById(R.id.tvMonth),
                itemView.findViewById(R.id.tvYear),
                itemView.findViewById(R.id.tvAll)
            )
        }

        init {
            initChart()
            listenClickViews(
                tvDay,
                tvWeek,
                tvMonth,
                tvYear,
                tvAll,
                btnBitcoin,
                btnEth,
                btnBch,
                btnXlm,
                retryLoadingButton
            )
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.tvDay -> {
                    callback.onClickTimeChart(TimeSpan.DAY)
                    showTimeSpanSelected(TimeSpan.DAY)
                }
                R.id.tvWeek -> {
                    callback.onClickTimeChart(TimeSpan.WEEK)
                    showTimeSpanSelected(TimeSpan.WEEK)
                }
                R.id.tvMonth -> {
                    callback.onClickTimeChart(TimeSpan.MONTH)
                    showTimeSpanSelected(TimeSpan.MONTH)
                }
                R.id.tvYear -> {
                    callback.onClickTimeChart(TimeSpan.YEAR)
                    showTimeSpanSelected(TimeSpan.YEAR)
                }
                R.id.tvAll -> {
                    callback.onClickTimeChart(TimeSpan.ALL_TIME)
                    showTimeSpanSelected(TimeSpan.ALL_TIME)
                }
                R.id.btnBitcoin -> {
                    loadingProgressBar.visible()
                    callback.onClickSegmentChart(CryptoCurrency.BTC)
                    tvCurrency.text = v.context.getText(R.string.bitcoin_price)
                    textview_price.text = "-----"
                    textview_percentage.text = "---"
                }
                R.id.btnEth -> {
                    loadingProgressBar.visible()
                    callback.onClickSegmentChart(CryptoCurrency.ETHER)
                    tvCurrency.text = v.context.getText(R.string.ether_price)
                    textview_price.text = "-----"
                    textview_percentage.text = "---"
                }
                R.id.btnBch -> {
                    loadingProgressBar.visible()
                    callback.onClickSegmentChart(CryptoCurrency.BCH)
                    tvCurrency.text = v.context.getText(R.string.bch_price)
                    textview_price.text = "-----"
                    textview_percentage.text = "---"
                }
                R.id.btnXlm -> {
                    loadingProgressBar.visible()
                    callback.onClickSegmentChart(CryptoCurrency.XLM)
                    tvCurrency.text = v.context.getText(R.string.bch_price)
                    textview_price.text = "-----"
                    textview_percentage.text = "---"
                }
                R.id.retryLoadingButton -> {
                    loadingProgressBar.visible()
                    retryLoadingButton.gone()
                    errorMessageTextView.gone()
                    callback.onClickRetry()
                }
            }
            buttonsList.forEach {
                it.isEnabled = false
            }
            btnBitcoin.isEnabled = false
            btnBch.isEnabled = false
            btnXlm.isEnabled = false
            btnEth.isEnabled = false
        }

        fun showTimeSpanSelected(timeSpan: TimeSpan) {
            selectButton(timeSpan)
            setDateFormatter(timeSpan)
            loadingProgressBar.visible()
        }

        fun showSegmentCoin(cryptoCurrency: CryptoCurrency) {
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> {
                    btnBitcoin.isChecked = true
                    btnEth.isChecked = false
                    btnBch.isChecked = false
                    btnXlm.isChecked = false
                }
                CryptoCurrency.ETHER -> {
                    btnBitcoin.isChecked = false
                    btnEth.isChecked = true
                    btnBch.isChecked = false
                    btnXlm.isChecked = false
                }
                CryptoCurrency.BCH -> {
                    btnBitcoin.isChecked = false
                    btnEth.isChecked = false
                    btnBch.isChecked = true
                    btnXlm.isChecked = false
                }
                else -> {
                    btnBitcoin.isChecked = false
                    btnEth.isChecked = false
                    btnBch.isChecked = false
                    btnXlm.isChecked = true
                }
            }
        }

        private fun selectButton(timeSpan: TimeSpan) {
            when (timeSpan) {
                TimeSpan.ALL_TIME -> setTextViewSelected(tvAll)
                TimeSpan.YEAR -> setTextViewSelected(tvYear)
                TimeSpan.MONTH -> setTextViewSelected(tvMonth)
                TimeSpan.WEEK -> setTextViewSelected(tvWeek)
                TimeSpan.DAY -> setTextViewSelected(tvDay)
            }
        }

        private fun setTextViewSelected(selected: TextView) {
            with(selected) {
                paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                this.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            }
            buttonsList.filterNot { it == selected }
                .map {
                    with(it) {
                        paintFlags = paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                    }
                    it.setTextColor(getTextColor(context = it.context, attrId = R.attr.colorNavigationActive))
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

        private fun initChart() {
            chart.apply {
                setDrawGridBackground(false)
                setDrawBorders(false)
                setScaleEnabled(false)
                setPinchZoom(true)
                setTouchEnabled(true)
                isHighlightPerDragEnabled = true
                isDragEnabled = true
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
                axisLeft.isEnabled = true
                axisLeft.setDrawLabels(false)
                axisLeft.setDrawAxisLine(false)
                axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                axisLeft.xOffset = 15.0f
                axisLeft.axisLineWidth = 1.0f
                axisLeft.gridColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
                axisLeft.gridLineWidth = 1.0f
                axisLeft.spaceTop = 20.0f
                axisLeft.spaceBottom = 0.0f
                axisLeft.setLabelCount(5, true)
                val gapSize = convertDpToPixel(80f)
                val lineLength = convertDpToPixel(5000f)
                axisLeft.enableGridDashedLine(lineLength, gapSize, lineLength)
                axisRight.isEnabled = false
                xAxis.setDrawGridLines(false)
                xAxis.textColor = ContextCompat.getColor(context, R.color.primary_gray_medium)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.isGranularityEnabled = true
                xAxis.setDrawAxisLine(true)
                xAxis.yOffset = 15.0f
                setExtraOffsets(8f, 0f, 0f, 10f)
                setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_gray_medium))
            }
        }
    }

    class TopStoriesViewHolder(
        itemView: View,
        private val callback: MainCallback
    ) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        val storiesAdapter by lazy { StoriesAdapter(callback) }
        override val containerView: View?
            get() = itemView

        init {
            txtTitle.text = "Top Stories"
            txtViewMore.text = "View more stories"
            recyClear.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                setHasFixedSize(true)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL, true))
                adapter = storiesAdapter
            }
            txtViewMore.visible()
            txtViewMore.setOnClickListener {
                callback.onClickMoreStory()
            }
        }
    }

    class AdsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView

        init {
            adViewContainer.gone()
        }
    }

    class BlockViewHolder(
        itemView: View,
        private val callback: MainCallback
    ) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
        val blocksAdapter by lazy { BlocksAdapter(callback) }

        init {
            cardViewMore.gone()
            txtTitle.text = "Latest Blocks"
            recyClear.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                setHasFixedSize(true)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL, false))
                adapter = blocksAdapter
            }
            txtViewMore.gone()
        }
    }

    interface MainCallback {
        fun onClickExchange()
        fun onClickSend()
        fun onClickRequest()
        fun onClickScanner()
        fun onClickTimeChart(timeSpan: TimeSpan)
        fun onClickSegmentChart(cryptoCurrency: CryptoCurrency)
        fun onClickNews(url: String)
        fun onClickRetry()
        fun onClickMoreStory()
        fun onClickMoreDiscover()
        fun onClickBlocks(hash: String?, totalSend: Long?, weight: Long?)
    }
}