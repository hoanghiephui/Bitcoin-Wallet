package com.bitcoin.wallet.mobile.ui.activitys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.mobile.Constants
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.base.BaseActivity
import com.bitcoin.wallet.mobile.extension.gone
import com.bitcoin.wallet.mobile.ui.fragments.BitmapBottomDialog
import com.bitcoin.wallet.mobile.ui.fragments.HelpDialogFragment
import com.bitcoin.wallet.mobile.ui.widget.CurrencyAmountView
import com.bitcoin.wallet.mobile.ui.widget.CurrencyCalculatorLink
import com.bitcoin.wallet.mobile.utils.Configuration
import com.bitcoin.wallet.mobile.utils.Event
import com.bitcoin.wallet.mobile.viewmodel.RequestCoinsViewModel
import kotlinx.android.synthetic.main.activity_request_coin.*
import kotlinx.android.synthetic.main.toolbar.*
import org.bitcoinj.core.Address
import org.bitcoinj.script.Script

class RequestCoinActivity : BaseActivity() {
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[RequestCoinsViewModel::class.java]
    }
    private val config: Configuration by lazy {
        application.config
    }
    private val clipboardManager: ClipboardManager by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    private var amountCalculatorLink: CurrencyCalculatorLink? = null

    override fun onAttachedToWindow() {
        setShowWhenLocked(true)
        setSupportActionBar(toolbar)
        toolbar.title = ""
    }

    override fun layoutRes(): Int {
        return R.layout.activity_request_coin
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar(getString(R.string.request_coins))
        savedInstanceState?.let {
            restoreInstanceState(it)
        }
        config.let {
            btcAmountView.setCurrencySymbol(it.format.code())
            btcAmountView.setInputFormat(it.maxPrecisionFormat)
            btcAmountView.setHintFormat(it.format)
        }
        localAmountView.setInputFormat(Constants.LOCAL_FORMAT)
        localAmountView.setHintFormat(Constants.LOCAL_FORMAT)
        amountCalculatorLink = CurrencyCalculatorLink(btcAmountView, localAmountView)
        amountCalculatorLink?.let {
            config.let { configuration ->
                it.exchangeDirection = configuration.lastExchangeDirection
                it.requestFocus()
            }
        }

        if (intent?.hasExtra(INTENT_EXTRA_OUTPUT_SCRIPT_TYPE) == true) {
            viewModel.freshReceiveAddress.overrideOutputScriptType(
                intent
                    .getSerializableExtra(INTENT_EXTRA_OUTPUT_SCRIPT_TYPE) as Script.ScriptType
            )
        }

        viewModel.showBitmapDialog.observe(this, object : Event.Observer<Bitmap>() {
            override fun onEvent(content: Bitmap?) {
                content?.let {
                    BitmapBottomDialog.show(this@RequestCoinActivity, it)
                }
            }
        })

        viewModel.exchangeRate.observe(this, Observer {
            amountCalculatorLink?.exchangeRate = it.rate
        })
        viewModel.bitcoinUri.observe(this, Observer {
            invalidateOptionsMenu()
        })
        viewModel.paymentRequest.observe(this, Observer { paymentRequest ->
            val initiateText = SpannableStringBuilder(
                getString(R.string.initiate_request_qr)
            )
            initiateRequestView.text = initiateText
        })

        viewModel.qrCode.observe(this, Observer { bitmap ->
            val qrDrawable = BitmapDrawable(resources, bitmap)
            qrDrawable.isFilterBitmap = false
            request_coins_qr.setImageDrawable(qrDrawable)
            /*request_coins_qr.setColorFilter(
                ContextCompat.getColor(
                    this,
                    if (isDarkMode) R.color.white else R.color.colorPrimaryDarkTheme
                )
            )*/
            request_coins_qr.setOnClickListener {
                viewModel.showBitmapDialog.setValue(Event(bitmap))
            }
            progressBar5.gone()
        })
        viewModel.showHelpDialog.observe(this, object : Event.Observer<Int>() {
            override fun onEvent(content: Int?) {
                content?.let {
                    HelpDialogFragment.show(this@RequestCoinActivity, it)
                }
            }

        })

        btnRequest.setOnClickListener {
            handleShare()
        }
        help.setOnClickListener {
            viewModel.showHelpDialog.value = Event(R.string.help_request_coins)
        }
        tvTap.setOnClickListener {
            handleCopy()
        }
        btcAmountView.setTextColor(
            ContextCompat.getColor(
                this,
                if (isDarkMode) R.color.white else R.color.colorInvertedBlackThemeAlternate2
            )
        )
    }

    override fun onResume() {
        super.onResume()
        amountCalculatorLink?.setListener(object : CurrencyAmountView.Listener {
            override fun changed() {
                amountCalculatorLink?.let {
                    viewModel.amount.value = it.amount
                }
            }

            override fun focusChanged(hasFocus: Boolean) {
            }
        })
    }

    override fun onPause() {
        amountCalculatorLink?.setListener(null)
        super.onPause()
    }

    override fun onDestroy() {
        amountCalculatorLink?.exchangeDirection?.let {
            config.lastExchangeDirection = it
        }
        super.onDestroy()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val hasBitcoinUri = viewModel.bitcoinUri.value != null
        menu?.findItem(R.id.menu_copy)?.isEnabled = hasBitcoinUri
        menu?.findItem(R.id.menu_share)?.isEnabled = hasBitcoinUri
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_request_coin, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_copy -> {
                handleCopy()
                return true
            }
            R.id.menu_share -> {
                handleShare()
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveInstanceState(outState)
    }

    private fun saveInstanceState(outState: Bundle) {
        val receiveAddress = viewModel.freshReceiveAddress.value
        if (receiveAddress != null)
            outState.putString(KEY_RECEIVE_ADDRESS, receiveAddress.toString())
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        if (savedInstanceState.containsKey(KEY_RECEIVE_ADDRESS))
            viewModel.freshReceiveAddress.setValue(
                Address.fromString(
                    Constants.NETWORK_PARAMETERS,
                    savedInstanceState.getString(KEY_RECEIVE_ADDRESS)!!
                )
            )
    }

    private fun handleCopy() {
        val request = viewModel.bitcoinUri.value
        clipboardManager.primaryClip = ClipData.newRawUri("Bitcoin payment request", request)
        Toast.makeText(this, R.string.clipboard_msg_request, Toast.LENGTH_SHORT).show()
    }

    private fun handleShare() {
        val request = viewModel.bitcoinUri.value
        val builder = ShareCompat.IntentBuilder.from(this)
        builder.setType("text/plain")
        builder.setText(request.toString())
        builder.setChooserTitle(R.string.request_coins_share)
        builder.startChooser()
    }

    companion object {
        const val INTENT_EXTRA_OUTPUT_SCRIPT_TYPE = "output_script_type"
        private const val KEY_RECEIVE_ADDRESS = "receive_address"

        fun start(context: Context, outputScriptType: Script.ScriptType?) {
            val intent = Intent(context, RequestCoinActivity::class.java)
            if (outputScriptType != null)
                intent.putExtra(INTENT_EXTRA_OUTPUT_SCRIPT_TYPE, outputScriptType)
            context.startActivity(intent)
        }
    }
}