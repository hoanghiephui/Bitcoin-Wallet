package com.bitcoin.wallet.btc.ui.activitys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.getTextString
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.extension.hideKeyboard
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.utils.Utils.convertDate
import com.bitcoin.wallet.btc.utils.Utils.onGetDate
import com.bitcoin.wallet.btc.utils.WalletUtils
import com.bitcoin.wallet.btc.viewmodel.ToolsViewModel
import com.facebook.ads.NativeBannerAdView
import com.google.android.material.snackbar.Snackbar
import com.tsongkha.spinnerdatepicker.DatePicker
import com.tsongkha.spinnerdatepicker.DatePickerDialog
import com.tsongkha.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import kotlinx.android.synthetic.main.activity_tools.*
import kotlinx.android.synthetic.main.init_ads.*
import java.text.NumberFormat
import java.util.*

class ToolsActivity : BaseActivity(), DatePickerDialog.OnDateSetListener {
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[ToolsViewModel::class.java]
    }

    override fun layoutRes(): Int {
        return R.layout.activity_tools
    }

    private val nf = NumberFormat.getCurrencyInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Price Converter")
        initSpiner()
        viewModel.priceCoinData.observeNotNull(this) {
            if (edtUsd.getTextString().isNotEmpty()) {
                val price =
                    if (spinner.getItemAtPosition(spinner.selectedItemPosition) == "BTC") it?.data?.bTC?.times(edtUsd.getTextString().toDouble())
                    else it?.data?.bCH?.times(edtUsd.getTextString().toDouble())
                price?.let { data ->
                    edtCoin.text = nf.format(data)
                }
            }
        }
        viewModel.networkState.observeNotNull(this) {
            progressBar2.isVisible = it == NetworkState.LOADING
            btnCalculate.text = if (it.msg == null) "Calculate Exchange" else "Retry"
        }
        viewModel.onGetPriceCoin()

        logicPriceCoin()
        viewModel.historyPriceCoinData.observeNotNull(this) {
            val price = it.lookup?.price
            tvPriceHistory.text = nf.apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 1
            }.format(price)
        }
        viewModel.historyNetworkState.observeNotNull(this) {
            progressBar3.isVisible = it == NetworkState.LOADING
            btnRetry.isVisible = it.msg != null
        }

        tvDate.text = onGetDate("MM/dd/yyyy")
        viewModel.onGetHistoryPriceCoin(onGetDate("yyyy-MM-dd"))
        rootDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val monthOfYear = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)
            showDate(year, monthOfYear, day, R.style.DatePickerSpinner, calendar)
        }

        btnRetry.setOnClickListener {
            viewModel.retryHistoryPriceCoin()
        }
        mViewType = NativeBannerAdView.Type.HEIGHT_100
        createAndLoadNativeBannerAd(getString(R.string.fb_banner_native_tools))
        adViewContainer.apply {
            gone()
            background = ContextCompat.getDrawable(this@ToolsActivity, R.drawable.bg_up_next)
            setPadding(8)
            Constants.setMargin(this, WalletUtils.dpToPx(context, 10))
        }
    }

    private fun showDate(year: Int, monthOfYear: Int, dayOfMonth: Int, spinnerTheme: Int, maxDate: Calendar?) {
        if (maxDate != null) {
            val maxDay = maxDate.get(Calendar.DAY_OF_MONTH)
            val maxMonth = maxDate.get(Calendar.MONTH)
            val maxYear = maxDate.get(Calendar.YEAR)
            SpinnerDatePickerDialogBuilder()
                .context(this)
                .callback(this)
                .spinnerTheme(spinnerTheme)
                .defaultDate(year, monthOfYear, dayOfMonth)
                .maxDate(maxYear, maxMonth, maxDay)
                .build()
                .show()
        } else {
            SpinnerDatePickerDialogBuilder()
                .context(this)
                .callback(this)
                .spinnerTheme(spinnerTheme)
                .defaultDate(year, monthOfYear, dayOfMonth)
                .build()
                .show()
        }
    }

    /**
     * @param view        The view associated with this listener.
     * @param year        The year that was set
     * @param monthOfYear The month that was set (0-11) for compatibility
     * with [java.util.Calendar].
     * @param dayOfMonth  The day of the month that was set.
     */
    override fun onDateSet(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val time = year.toString().plus("-")
            .plus(monthOfYear + 1)
            .plus("-")
            .plus(dayOfMonth)
        tvDate.text = convertDate(
            time, "MM/dd/yyyy", "yyyy-MM-dd"
        )
        viewModel.onGetHistoryPriceCoin(convertDate(
            time, "yyyy-MM-dd", "yyyy-MM-dd"
        ))
    }

    private fun logicPriceCoin() {
        btnCalculate.setOnClickListener {
            it.hideKeyboard()
            if (viewModel.networkState.value?.msg == null) {
                if (TextUtils.isEmpty(edtUsd.getTextString())) {
                    Snackbar.make(it, "Enter the amount to convert.", Snackbar.LENGTH_LONG).show()
                    edtCoin.text = ""
                    return@setOnClickListener
                }
                val price = if (spinner.getItemAtPosition(spinner.selectedItemPosition) == "BTC")
                    viewModel.priceCoinData.value?.data?.bTC?.times(edtUsd.getTextString().toDouble())
                else viewModel.priceCoinData.value?.data?.bCH?.times(edtUsd.getTextString().toDouble())
                price?.let {
                    edtCoin.text = nf.format(it)
                }
            } else {
                viewModel.retryPriceCoin()
            }
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                if (TextUtils.isEmpty(edtUsd.getTextString()) || viewModel.networkState.value == NetworkState.LOADING) {
                    return
                }
                val price = if (parent?.getItemAtPosition(position).toString() == "BTC")
                    viewModel.priceCoinData.value?.data?.bTC?.times(edtUsd.getTextString().toDouble())
                else viewModel.priceCoinData.value?.data?.bCH?.times(edtUsd.getTextString().toDouble())
                price?.let {
                    edtCoin.text = nf.format(it)
                }
            }

        }
    }

    private fun initSpiner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.coin, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val adapterCoin = ArrayAdapter.createFromResource(
            this,
            R.array.price_array, android.R.layout.simple_spinner_item
        )
        adapterCoin.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCoin.adapter = adapterCoin
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, ToolsActivity::class.java))
        }
    }
}