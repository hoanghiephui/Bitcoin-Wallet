package com.bitcoin.wallet.mobile.ui.activitys

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.mobile.Constants
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.base.BaseActivity
import com.bitcoin.wallet.mobile.data.*
import com.bitcoin.wallet.mobile.extension.gone
import com.bitcoin.wallet.mobile.extension.invisible
import com.bitcoin.wallet.mobile.extension.visible
import com.bitcoin.wallet.mobile.service.BlockchainService
import com.bitcoin.wallet.mobile.ui.activitys.ScanActivity.Companion.REQUEST_CODE_SCAN
import com.bitcoin.wallet.mobile.ui.adapter.ListItem
import com.bitcoin.wallet.mobile.ui.adapter.ReceivingAddressViewAdapter
import com.bitcoin.wallet.mobile.ui.adapter.TransactionsWalletAdapter
import com.bitcoin.wallet.mobile.ui.fragments.ProgressDialogFragment
import com.bitcoin.wallet.mobile.ui.widget.CurrencyAmountView
import com.bitcoin.wallet.mobile.ui.widget.CurrencyCalculatorLink
import com.bitcoin.wallet.mobile.ui.widget.DialogBuilder
import com.bitcoin.wallet.mobile.utils.*
import com.bitcoin.wallet.mobile.viewmodel.SendViewModel
import com.google.common.base.Joiner
import kotlinx.android.synthetic.main.activity_send_coin.*
import kotlinx.android.synthetic.main.item_transaction.*
import kotlinx.android.synthetic.main.item_wallet_address.*
import org.bitcoinj.core.*
import org.bitcoinj.protocols.payments.PaymentProtocol
import org.bitcoinj.wallet.KeyChain
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.bouncycastle.crypto.params.KeyParameter
import java.io.FileNotFoundException
import java.util.*

class SendCoinActivity : BaseActivity() {
    private val viewModel: SendViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[SendViewModel::class.java]
    }
    private val config: Configuration by lazy {
        application.config
    }
    private val addressBookDao: AddressBookDao by lazy {
        AppDatabase.getDatabase(this).addressBookDao()
    }
    private val contentResolvers: ContentResolver by lazy {
        application.contentResolver
    }

    private var receivingAddressViewAdapter: ReceivingAddressViewAdapter? = null
    private var amountCalculatorLink: CurrencyCalculatorLink? = null
    private val handler = Handler()
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var sentTransactionViewHolder: TransactionsWalletAdapter.TransactionViewHolder? = null


    override fun layoutRes(): Int {
        return R.layout.activity_send_coin
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Send")
        backgroundThread = HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND).also {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
        if (savedInstanceState == null) {
            val action = intent.action
            val intentUri = intent.data
            val scheme = intentUri?.scheme
            val mimeType = intent.type

            if ((Intent.ACTION_VIEW == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action)
                && intentUri != null && "bitcoin" == scheme
            ) {
                initStateFromBitcoinUri(intentUri)
            } else if (Intent.ACTION_VIEW == action && PaymentProtocol.MIMETYPE_PAYMENTREQUEST == mimeType) {
                val paymentRequest = BitcoinIntegration.paymentRequestFromIntent(intent)

                if (intentUri != null)
                    initStateFromIntentUri(mimeType, intentUri)
                else if (paymentRequest != null)
                    initStateFromPaymentRequest(mimeType, paymentRequest)
                else
                    throw IllegalArgumentException()
            } else if (intent.hasExtra(INTENT_EXTRA_PAYMENT_INTENT)) {
                initStateFromIntentExtras(intent.extras)
            } else {
                updateStateFrom(PaymentIntent.blank())
            }
        }

        viewModel.priceList.observe(this, Observer {
            textview_price.text = "$".plus(it["USD"]?.price.toString()).plus(" 1 BTC")
        })
        viewModel.priceNetworkState.observe(this, Observer {

        })
        viewModel.wallet.observe(this, Observer {
            updateView()
        })
        viewModel.addressBook.observe(this, Observer {
            updateView()
        })
        viewModel.exchangeRate.observe(this, Observer {
            if (viewModel.state == null) {
                amountCalculatorLink?.exchangeRate = it.rate
            } else {
                viewModel.state?.let { state ->
                    if (state <= SendViewModel.State.INPUT)
                        amountCalculatorLink?.exchangeRate = it.rate
                }
            }
        })
        viewModel.dynamicFees.observe(this, Observer {
            updateView()
            handler.post(dryrunRunnable)
        })
        viewModel.blockchainState.observe(this, Observer {
            updateView()
        })
        viewModel.balance.observe(this, Observer {
            invalidateOptionsMenu()
        })
        viewModel.progress.observe(this, ProgressDialogFragment.Observer(supportFragmentManager))

        viewModel.onGetPrice("BTC")
        receivingAddressView.setAdapter<ReceivingAddressViewAdapter>(receivingAddressViewAdapter)
        receivingAddressView.onFocusChangeListener = receivingAddressListener
        receivingAddressView.addTextChangedListener(receivingAddressListener)
        receivingAddressView.onItemClickListener = receivingAddressListener
        btcAmountView.setColor(
            if (isDarkMode) ContextCompat.getColor(this, R.color.colorPrimaryDark) else ContextCompat.getColor(
                this,
                R.color.colorPrimaryDarkTheme
            ),
            if (isDarkMode) ContextCompat.getColor(this, R.color.colorPrimaryDark) else ContextCompat.getColor(
                this,
                R.color.colorPrimaryDarkTheme
            )
        )
        localAmountView.setColor(
            if (isDarkMode) ContextCompat.getColor(this, R.color.colorPrimaryDark) else ContextCompat.getColor(
                this,
                R.color.colorPrimaryDarkTheme
            ),
            if (isDarkMode) ContextCompat.getColor(this, R.color.colorPrimaryDark) else ContextCompat.getColor(
                this,
                R.color.colorPrimaryDarkTheme
            )
        )
        btcAmountView.setCurrencySymbol(config.format.code())
        btcAmountView.setInputFormat(config.maxPrecisionFormat)
        btcAmountView.setHintFormat(config.format)

        localAmountView.setInputFormat(Constants.LOCAL_FORMAT)
        localAmountView.setHintFormat(Constants.LOCAL_FORMAT)
        amountCalculatorLink = CurrencyCalculatorLink(btcAmountView, localAmountView)
        amountCalculatorLink?.exchangeDirection = config.lastExchangeDirection

        transaction_row.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.transaction_layout_anim)
        sentTransactionViewHolder = TransactionsWalletAdapter.TransactionViewHolder(container)

        viewGo.setOnClickListener {
            validateReceivingAddress()

            if (everythingPlausible())
                handleGo()
            else
                requestFocusFirst()

            updateView()
        }
        viewCancel.setOnClickListener {
            handleCancel()
        }

        viewFee()
        btnMore.invisible()
    }

    private fun viewFee() {
        scanQR.setColorFilter(
            if (isDarkMode) ContextCompat.getColor(
                this,
                R.color.colorPrimary
            ) else ContextCompat.getColor(this, R.color.color_eth)
        )
        scanQR.setOnClickListener {
            ScanActivity.startForResult(this, REQUEST_CODE_SCAN)
        }
        val arr = ArrayList<String>()
        arr.add(getString(R.string.fee_economic_))
        arr.add(getString(R.string.fee_category_normal))
        arr.add(getString(R.string.fee_category_priority))

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arr
        )
        spinnerPriority.adapter = adapter
        spinnerPriority.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                when (i) {
                    0 -> {
                        handleFeeCategory(FeeCategory.ECONOMIC)
                        upDateViewFee()
                    }
                    1 -> {
                        handleFeeCategory(FeeCategory.NORMAL)
                        upDateViewFee()
                    }
                    else -> {
                        handleFeeCategory(FeeCategory.PRIORITY)
                        upDateViewFee()
                    }
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }
        textviewFeeAbsolute.setOnClickListener {
            spinnerPriority.performClick()
        }
    }

    override fun onResume() {
        super.onResume()
        amountCalculatorLink?.setListener(amountsListener)
        privateKeyPasswordView.addTextChangedListener(privateKeyPasswordListener)
        updateView()
        handler.post(dryrunRunnable)
    }

    override fun onPause() {
        privateKeyPasswordView.removeTextChangedListener(privateKeyPasswordListener)
        amountCalculatorLink?.setListener(null)
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        amountCalculatorLink?.exchangeDirection?.let {
            config.lastExchangeDirection = it
        }
        handler.removeCallbacksAndMessages(null)
        backgroundThread?.looper?.quit()
        viewModel.sentTransaction?.confidence?.removeEventListener(sentTransactionConfidenceListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handler.post { onActivityResultResumed(requestCode, resultCode, data) }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_send_coin, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val emptyAction = menu?.findItem(R.id.send_coins_options_empty)
        emptyAction?.isEnabled = (viewModel.state == SendViewModel.State.INPUT
                && viewModel.paymentIntent?.mayEditAmount() == true && viewModel.balance.value != null)

        val feeCategoryAction = menu?.findItem(R.id.send_coins_options_fee_category)
        feeCategoryAction?.isEnabled = viewModel.state == SendViewModel.State.INPUT
        when {
            viewModel.feeCategory == FeeCategory.ECONOMIC -> menu?.findItem(R.id.send_coins_options_fee_category_economic)
                ?.isChecked = true
            viewModel.feeCategory == FeeCategory.NORMAL -> menu?.findItem(R.id.send_coins_options_fee_category_normal)
                ?.isChecked = true
            viewModel.feeCategory == FeeCategory.PRIORITY -> menu?.findItem(R.id.send_coins_options_fee_category_priority)
                ?.isChecked = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.send_coins_options_scan -> {
                ScanActivity.startForResult(this, REQUEST_CODE_SCAN)
                return true
            }
            R.id.send_coins_options_fee_category_economic -> {
                handleFeeCategory(FeeCategory.ECONOMIC)
                return true
            }
            R.id.send_coins_options_fee_category_normal -> {
                handleFeeCategory(FeeCategory.NORMAL)
                return true
            }
            R.id.send_coins_options_fee_category_priority -> {
                handleFeeCategory(FeeCategory.PRIORITY)
                return true
            }
            R.id.send_coins_options_empty -> {
                handleEmpty()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleFeeCategory(feeCategory: FeeCategory) {
        viewModel.feeCategory = feeCategory

        updateView()
        upDateViewFee()
        handler.post(dryrunRunnable)
    }

    private fun handleEmpty() {
        val available = viewModel.balance.value
        amountCalculatorLink?.setBtcAmount(available)

        updateView()
        handler.post(dryrunRunnable)
    }

    private fun handleGo() {
        privateKeyBadPasswordView.visibility = View.INVISIBLE

        val wallet = viewModel.wallet.value
        if (wallet?.isEncrypted == true) {
            object : DeriveKeyTask(backgroundHandler, application.scryptIterationsTarget()) {
                override fun onSuccess(encryptionKey: KeyParameter, changed: Boolean) {
                    if (changed)
                        WalletUtils.autoBackupWallet(this@SendCoinActivity, wallet)
                    signAndSendPayment(encryptionKey)
                }
            }.deriveKey(wallet, privateKeyPasswordView.text.toString().trim())

            setState(SendViewModel.State.DECRYPTING)
        } else {
            signAndSendPayment(null)
        }
    }

    private fun signAndSendPayment(encryptionKey: KeyParameter?) {
        setState(SendViewModel.State.SIGNING)

        // final payment intent
        viewModel.paymentIntent?.let {
            val finalPaymentIntent = it.mergeWithEditedValues(
                amountCalculatorLink?.amount,
                if (viewModel.validatedAddress != null) viewModel.validatedAddress?.address else null
            )
            val finalAmount = finalPaymentIntent.amount

            // prepare send request
            val fees = viewModel.dynamicFees.value
            val wallet = viewModel.wallet.value
            val sendRequest = finalPaymentIntent.toSendRequest()
            sendRequest.emptyWallet =
                it.mayEditAmount() && finalAmount == wallet?.getBalance(Wallet.BalanceType.AVAILABLE)
            sendRequest.feePerKb = fees!![viewModel.feeCategory]
            sendRequest.memo = it.memo
            sendRequest.exchangeRate = amountCalculatorLink?.exchangeRate
            sendRequest.aesKey = encryptionKey

            val fee = viewModel.dryrunTransaction?.fee
            if (fee?.isGreaterThan(finalAmount) == true) {
                setState(SendViewModel.State.INPUT)

                val btcFormat = config.format
                val dialog = DialogBuilder.warn(
                    this,
                    R.string.significant_fee
                )
                dialog.setMessage(
                    getString(
                        R.string.significant_fee_mes, btcFormat.format(fee),
                        btcFormat.format(finalAmount)
                    )
                )
                dialog.setPositiveButton(
                    R.string.btn_send
                ) { _, _ -> sendPayment(sendRequest, finalAmount) }
                dialog.setNegativeButton(R.string.btn_cancel, null)
                dialog.show()
            } else {
                sendPayment(sendRequest, finalAmount)
            }
        }
    }

    private fun sendPayment(sendRequest: SendRequest, finalAmount: Coin) {
        val wallet = viewModel.wallet.value
        object : SendCoinsOfflineTask(wallet, backgroundHandler) {
            override fun onSuccess(transaction: Transaction?) {
                viewModel.sentTransaction = transaction
                setState(SendViewModel.State.SENDING)

                viewModel.sentTransaction?.confidence?.addEventListener(sentTransactionConfidenceListener)
                viewModel.paymentIntent?.let {
                    val refundAddress = if (it.standard == PaymentIntent.Standard.BIP70)
                        wallet?.freshAddress(KeyChain.KeyPurpose.REFUND)
                    else
                        null
                    viewModel.sentTransaction?.let { transaction ->
                        val payment = PaymentProtocol.createPaymentMessage(
                            Arrays.asList(transaction), finalAmount, refundAddress,
                            null, it.payeeData
                        )

                        BlockchainService.broadcastTransaction(this@SendCoinActivity, viewModel.sentTransaction)

                        val callingActivity = callingActivity
                        if (callingActivity != null) {

                            val result = Intent()
                            BitcoinIntegration.transactionHashToResult(
                                result,
                                transaction.txId.toString()
                            )
                            if (it.standard == PaymentIntent.Standard.BIP70)
                                BitcoinIntegration.paymentToResult(result, payment.toByteArray())
                            setResult(Activity.RESULT_OK, result)
                        }
                    }
                }
            }

            override fun onInsufficientMoney(missing: Coin?) {
                setState(SendViewModel.State.INPUT)

                val estimated = wallet?.getBalance(Wallet.BalanceType.ESTIMATED)
                val available = wallet?.getBalance(Wallet.BalanceType.AVAILABLE)
                val pending = estimated?.subtract(available)

                val btcFormat = config.format

                val dialog = DialogBuilder.warn(
                    this@SendCoinActivity,
                    R.string.insufficient_money_tit
                )
                val msg = StringBuilder()
                msg.append(getString(R.string.insufficient_money_msg1, btcFormat.format(missing)))

                if (pending != null && pending.signum() > 0)
                    msg.append("\n\n")
                        .append(getString(R.string.send_coins_pending, btcFormat.format(pending)))
                viewModel.paymentIntent?.let {
                    if (it.mayEditAmount())
                        msg.append("\n\n").append(getString(R.string.insufficient_money_msg2))
                    dialog.setMessage(msg)
                    if (it.mayEditAmount()) {
                        dialog.setPositiveButton(
                            R.string.option_empty
                        ) { _, _ -> handleEmpty() }
                        dialog.setNegativeButton(R.string.btn_cancel, null)
                    } else {
                        dialog.setNeutralButton(R.string.btn_dismiss, null)
                    }
                }
                dialog.show()
            }

            override fun onInvalidEncryptionKey() {
                setState(SendViewModel.State.INPUT)
                privateKeyBadPasswordView.visibility = View.VISIBLE
                privateKeyPasswordView.requestFocus()
            }

            override fun onEmptyWalletFailed() {
                setState(SendViewModel.State.INPUT)

                val dialog = DialogBuilder.warn(
                    this@SendCoinActivity,
                    R.string.wallet_failed_title
                )
                dialog.setMessage(R.string.wallet_failed)
                dialog.setNeutralButton(R.string.btn_dismiss, null)
                dialog.show()
            }

            override fun onFailure(exception: java.lang.Exception) {
                setState(SendViewModel.State.FAILED)

                val dialog = DialogBuilder.warn(this@SendCoinActivity, R.string.error_msg)
                dialog.setMessage(exception.toString())
                dialog.setNeutralButton(R.string.btn_dismiss, null)
                dialog.show()
            }

        }.sendCoinsOffline(sendRequest) // send asynchronously
    }


    private fun onActivityResultResumed(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                val bundle = intent?.extras ?: return
                val input = bundle.getString(ScanActivity.INTENT_EXTRA_RESULT) ?: return

                object : InputParser.StringInputParser(input) {
                    override fun handlePaymentIntent(paymentIntent: PaymentIntent) {
                        setState(null)
                        updateStateFrom(paymentIntent)
                    }

                    @Throws(VerificationException::class)
                    override fun handleDirectTransaction(transaction: Transaction) {
                        cannotClassify(input)
                    }

                    override fun error(messageResId: Int, vararg messageArgs: Any) {
                        dialog(this@SendCoinActivity, null, R.string.btn_scan, messageResId, messageArgs)
                    }
                }.parse()
            }
        }
    }

    private fun initStateFromIntentExtras(extras: Bundle?) {
        val paymentIntent = extras?.getParcelable<Parcelable>(INTENT_EXTRA_PAYMENT_INTENT)
        val feeCategory = extras
            ?.getSerializable(INTENT_EXTRA_FEE_CATEGORY) as FeeCategory?
        feeCategory?.let {
            viewModel.feeCategory = it
        }
        updateStateFrom(paymentIntent as PaymentIntent?)
    }

    private fun initStateFromBitcoinUri(bitcoinUri: Uri) {
        val input = bitcoinUri.toString()

        object : InputParser.StringInputParser(input) {
            override fun handlePaymentIntent(paymentIntent: PaymentIntent) {
                updateStateFrom(paymentIntent)
            }

            override fun handlePrivateKey(key: PrefixedChecksummedBytes) {
                throw UnsupportedOperationException()
            }

            @Throws(VerificationException::class)
            override fun handleDirectTransaction(transaction: Transaction) {
                throw UnsupportedOperationException()
            }

            override fun error(messageResId: Int, vararg messageArgs: Any) {
                dialog(this@SendCoinActivity, activityDismissListener, 0, messageResId, messageArgs)
            }
        }.parse()
    }

    private fun initStateFromPaymentRequest(mimeType: String, input: ByteArray) {
        object : InputParser.BinaryInputParser(mimeType, input) {
            override fun handlePaymentIntent(paymentIntent: PaymentIntent) {
                updateStateFrom(paymentIntent)
            }

            override fun error(messageResId: Int, vararg messageArgs: Any) {
                dialog(this@SendCoinActivity, activityDismissListener, 0, messageResId, messageArgs)
            }
        }.parse()
    }

    private fun initStateFromIntentUri(mimeType: String, bitcoinUri: Uri) {
        try {
            val `is` = contentResolvers.openInputStream(bitcoinUri)

            object : InputParser.StreamInputParser(mimeType, `is`) {
                override fun handlePaymentIntent(paymentIntent: PaymentIntent) {
                    updateStateFrom(paymentIntent)
                }

                override fun error(messageResId: Int, vararg messageArgs: Any) {
                    dialog(this@SendCoinActivity, activityDismissListener, 0, messageResId, messageArgs)
                }
            }.parse()
        } catch (x: FileNotFoundException) {
            throw RuntimeException(x)
        }

    }

    private fun updateStateFrom(paymentIntent: PaymentIntent?) {
        viewModel.paymentIntent = paymentIntent
        viewModel.validatedAddress = null
        paymentIntent?.let {
            handler.post {
                if (paymentIntent.hasPaymentRequestUrl() && paymentIntent.isHttpPaymentRequestUrl) {
                    requestPaymentRequest()
                } else {
                    setState(SendViewModel.State.INPUT)
                    receivingAddressView.text = null
                    amountCalculatorLink?.setBtcAmount(paymentIntent.amount)

                    updateView()
                    handler.post(dryrunRunnable)
                }
            }
        }
    }

    private fun requestPaymentRequest() {
        val paymentRequestHost: String?
        viewModel.paymentIntent?.let {
            paymentRequestHost =
                Uri.parse(it.paymentRequestUrl).host
            viewModel.progress.value =
                getString(R.string.send_coin_progress, paymentRequestHost)
            setState(SendViewModel.State.REQUEST_PAYMENT_REQUEST)

            val callback = object : RequestPaymentTask.ResultCallback {
                override fun onPaymentIntent(paymentIntent: PaymentIntent) {
                    viewModel.progress.value = null
                    viewModel.paymentIntent?.let { intent ->
                        if (intent.isExtendedBy(paymentIntent)) {
                            // success
                            setState(SendViewModel.State.INPUT)
                            updateStateFrom(paymentIntent)
                            updateView()
                            handler.post(dryrunRunnable)
                        } else {
                            val reasons = LinkedList<String>()
                            if (!intent.equalsAddress(paymentIntent))
                                reasons.add("address")
                            if (!intent.equalsAmount(paymentIntent))
                                reasons.add("amount")
                            if (reasons.isEmpty())
                                reasons.add("unknown")

                            val dialog = DialogBuilder.warn(
                                this@SendCoinActivity,
                                R.string.send_coins_failed
                            )
                            dialog.setMessage(
                                getString(
                                    R.string.send_coins_failed_mes,
                                    paymentRequestHost, Joiner.on(", ").join(reasons)
                                )
                            )
                            dialog.singleDismissButton({ _, which -> handleCancel() })
                            dialog.show()

                        }
                    }
                }

                override fun onFail(messageResId: Int, vararg messageArgs: Any) {
                    viewModel.progress.value = null

                    val dialog = DialogBuilder.warn(
                        this@SendCoinActivity,
                        R.string.send_coins_failed
                    )
                    dialog.setMessage(getString(messageResId, *messageArgs))
                    dialog.setPositiveButton(
                        R.string.btn_retry
                    ) { _, _ -> requestPaymentRequest() }
                    dialog.setNegativeButton(R.string.btn_dismiss) { _, _ ->
                        if (!it.hasOutputs())
                            handleCancel()
                        else
                            setState(SendViewModel.State.INPUT)
                    }
                    dialog.show()
                }
            }

            RequestPaymentTask.HttpRequestTask(backgroundHandler, callback, application.httpUserAgent())
                .requestPaymentRequest(it.paymentRequestUrl)
        }
    }

    private fun isPayeePlausible(): Boolean {
        if (viewModel.paymentIntent?.hasOutputs() == true)
            return true

        return viewModel.validatedAddress != null

    }

    private fun isAmountPlausible(): Boolean {
        return when {
            viewModel.dryrunTransaction != null -> viewModel.dryrunException == null
            viewModel.paymentIntent?.mayEditAmount() == true -> amountCalculatorLink?.hasAmount() ?: false
            else -> viewModel.paymentIntent?.hasAmount() ?: false
        }
    }

    private fun isPasswordPlausible(): Boolean {
        val wallet = viewModel.wallet.value ?: return false
        return if (!wallet.isEncrypted) true else !privateKeyPasswordView.text.toString().trim().isEmpty()
    }

    private fun everythingPlausible(): Boolean {
        return (viewModel.state === SendViewModel.State.INPUT && isPayeePlausible() && isAmountPlausible()
                && isPasswordPlausible())
    }

    private fun requestFocusFirst() {
        if (!isPayeePlausible())
            receivingAddressView.requestFocus()
        else if (!isAmountPlausible())
            amountCalculatorLink?.requestFocus()
        else if (!isPasswordPlausible())
            privateKeyPasswordView.requestFocus()
        else if (everythingPlausible())
            viewGo.requestFocus()
    }

    private fun handleCancel() {
        viewModel.state?.let {
            if (it <= SendViewModel.State.INPUT)
                setResult(Activity.RESULT_CANCELED)
            finish()
        }
        if (viewModel.state == null)
            setResult(Activity.RESULT_CANCELED)

        finish()
    }

    private val activityDismissListener = DialogInterface.OnClickListener { _, _ -> finish() }

    private inner class ReceivingAddressListener : View.OnFocusChangeListener, TextWatcher,
        AdapterView.OnItemClickListener {
        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (!hasFocus) {
                validateReceivingAddress()
                updateView()
            }
        }

        override fun afterTextChanged(s: Editable) {
            val constraint = s.toString().trim()
            if (!constraint.isEmpty())
                validateReceivingAddress()
            else
                updateView()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val entry = receivingAddressViewAdapter?.getItem(position)
            try {
                viewModel.validatedAddress = AddressAndLabel(
                    Constants.NETWORK_PARAMETERS, entry?.address,
                    entry?.label
                )
                receivingAddressView.text = null
            } catch (x: AddressFormatException) {
                // swallow
            }

        }
    }

    private val receivingAddressListener = ReceivingAddressListener()

    private val amountsListener = object : CurrencyAmountView.Listener {
        override fun changed() {
            updateView()
            handler.post(dryrunRunnable)
        }

        override fun focusChanged(hasFocus: Boolean) {}
    }

    private val privateKeyPasswordListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            privateKeyBadPasswordView.visibility = View.INVISIBLE
            updateView()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable) {}
    }

    private val sentTransactionConfidenceListener =
        TransactionConfidence.Listener { _, reason ->
            runOnUiThread {
                val mConfidence = viewModel.sentTransaction?.confidence
                val confidenceType = mConfidence?.confidenceType
                val numBroadcastPeers = mConfidence?.numBroadcastPeers()

                if (viewModel.state == SendViewModel.State.SENDING) {
                    if (confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
                        setState(SendViewModel.State.FAILED)
                    } else if (numBroadcastPeers != null) {
                        if (numBroadcastPeers > 1 || confidenceType == TransactionConfidence.ConfidenceType.BUILDING) {
                            setState(SendViewModel.State.SENT)

                            // Auto-close the dialog after a short delay
                            if (config.sendCoinsAutoclose) {
                                handler.postDelayed({ finish() }, 1000)
                            }
                        }
                    }
                }

                if (reason == TransactionConfidence.Listener.ChangeReason.SEEN_PEERS && confidenceType == TransactionConfidence.ConfidenceType.PENDING) {
                    // play sound effect
                    val soundResId = resources.getIdentifier(
                        "send_coins_broadcast_$numBroadcastPeers",
                        "raw", packageName
                    )
                    if (soundResId > 0)
                        RingtoneManager
                            .getRingtone(
                                this, Uri.parse(
                                    "android.resource://$packageName/$soundResId"
                                )
                            )
                            .play()
                }

                updateView()
            }
        }

    private fun validateReceivingAddress() {
        try {
            val addressStr = receivingAddressView.text.toString().trim()
            if (addressStr.isNotEmpty()) {
                val address = Address.fromString(Constants.NETWORK_PARAMETERS, addressStr)
                val label = addressBookDao.resolveLabel(address.toString())
                viewModel.validatedAddress = AddressAndLabel(
                    Constants.NETWORK_PARAMETERS, address.toString(),
                    label
                )
                receivingAddressView.text = null
            }
        } catch (x: AddressFormatException) {
            // swallow
        }

    }

    private fun upDateViewFee() {
        when (viewModel.feeCategory) {
            FeeCategory.NORMAL -> {
                textviewFeeType.text = getString(R.string.fee_category_normal)
                textviewFeeTime.text = "~15 - 60+ minutes."

            }
            FeeCategory.ECONOMIC -> {
                textviewFeeType.text = getString(R.string.fee_economic_)
                textviewFeeTime.text = "Can be days or weeks."
            }
            else -> {
                textviewFeeType.text = getString(R.string.fee_category_priority)
                textviewFeeTime.text = "~ 15+ minutes."
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateView() {
        val wallet = viewModel.wallet.value
        val fees = viewModel.dynamicFees.value
        val blockchainState = viewModel.blockchainState.value
        val addressBook = AddressBookEntry.asMap(viewModel.addressBook.value)
        val btcFormat = config.format
        viewModel.paymentIntent?.let { payment ->
            if (payment.hasPayee()) {
                payeeNameView.visible()
                payeeNameView.text = payment.payeeName

                payeeVerifiedByView.visible()
                val verifiedBy =
                    payment.payeeVerifiedBy ?: getString(R.string.verified_by_unknown)
                payeeVerifiedByView.text = Constants.CHAR_CHECKMARK + String.format(
                    getString(R.string.verified_by),
                    verifiedBy
                )
            } else {
                payeeNameView.visibility = View.GONE
                payeeVerifiedByView.visibility = View.GONE
            }

            if (payment.hasOutputs()) {
                payeeGroup.visible()
                receivingAddressView.gone()
                receivingStaticLabelView.visibility =
                    if (!payment.hasPayee() || payment.payeeVerifiedBy == null)
                        View.VISIBLE
                    else
                        View.GONE
                receivingStaticAddressView.visibility =
                    if (!payment.hasPayee() || payment.payeeVerifiedBy == null)
                        View.VISIBLE
                    else
                        View.GONE

                receivingStaticLabelView.text = payment.memo

                if (payment.hasAddress())
                    receivingStaticAddressView.text = WalletUtils.formatAddress(
                        payment.address,
                        Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE, true
                    )
                else
                    receivingStaticAddressView.setText(R.string.send_coins_address_complex)
            } else if (viewModel.validatedAddress != null) {
                payeeGroup.visibility = View.VISIBLE
                receivingAddressView.visibility = View.GONE
                receivingStaticLabelView.visible()
                receivingStaticAddressView.visible()

                receivingStaticAddressView.text = WalletUtils.formatAddress(
                    viewModel.validatedAddress!!.address,
                    Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE, true
                )
                val addressBookLabel = addressBookDao
                    .resolveLabel(viewModel.validatedAddress!!.address.toString())
                val staticLabel: String
                staticLabel = when {
                    addressBookLabel != null -> addressBookLabel
                    viewModel.validatedAddress!!.label != null -> viewModel.validatedAddress!!.label
                    else -> getString(R.string.address_unlabeled)
                }
                receivingStaticLabelView.text = staticLabel
                receivingStaticLabelView.setTextColor(
                    ContextCompat.getColor(
                        this,
                        if (viewModel.validatedAddress?.label != null) R.color.colorAccent else if (isDarkMode) R.color.white else R.color.colorInvertedBlackThemeAlternate2
                    )
                )
            } else if (payment.standard == null) {
                payeeGroup.visible()
                receivingStaticLabelView.gone()
                receivingStaticAddressView.gone()
                receivingAddressView.visible()
            } else {
                payeeGroup.gone()
            }

            receivingAddressView.isEnabled = viewModel.state == SendViewModel.State.INPUT

            amountGroup.visibility =
                if (payment.hasAmount() || viewModel.state != null && viewModel.state!!.compareTo(
                        SendViewModel.State.INPUT
                    ) >= 0
                )
                    View.VISIBLE
                else
                    View.GONE
            amountCalculatorLink?.setEnabled(
                viewModel.state == SendViewModel.State.INPUT && payment.mayEditAmount()
            )

            hintView.visibility = View.GONE
            viewModel.state?.let { state ->
                if (state == SendViewModel.State.INPUT) {
                    if (blockchainState != null && blockchainState.replaying) {
                        hintView.setTextColor(resources.getColor(R.color.fg_error))
                        hintView.visibility = View.VISIBLE
                        hintView.setText(R.string.replaying)
                    } else if (payment.mayEditAddress() && viewModel.validatedAddress == null
                        && !receivingAddressView.text.toString().trim().isEmpty()
                    ) {
                        hintView.setTextColor(resources.getColor(R.color.fg_error))
                        hintView.visibility = View.VISIBLE
                        hintView.setText(R.string.send_coins_error)
                    } else if (viewModel.dryrunException != null) {
                        hintView.setTextColor(resources.getColor(R.color.fg_error))
                        hintView.visibility = View.VISIBLE
                        when {
                            viewModel.dryrunException is Wallet.DustySendRequested -> hintView.text =
                                getString(R.string.dusty_send)
                            viewModel.dryrunException is InsufficientMoneyException -> hintView.text = getString(
                                R.string.insufficient_money,
                                btcFormat.format((viewModel.dryrunException as InsufficientMoneyException).missing!!)
                            )
                            viewModel.dryrunException is Wallet.CouldNotAdjustDownwards -> hintView.text =
                                getString(R.string.wallet_failed)
                            else -> hintView.text = viewModel.dryrunException.toString()
                        }
                    } else if (viewModel.dryrunTransaction != null && viewModel.dryrunTransaction?.fee != null) {
                        hintView.visibility = View.VISIBLE
                        val hintResId: Int
                        val colorResId: Int
                        when {
                            viewModel.feeCategory == FeeCategory.ECONOMIC -> {
                                hintResId = R.string.fee_economic
                                colorResId = R.color.fg_less_significant
                            }
                            viewModel.feeCategory == FeeCategory.PRIORITY -> {
                                hintResId = R.string.fee_priority
                                colorResId = R.color.fg_less_significant
                            }
                            else -> {
                                hintResId = R.string.hint_fee
                                colorResId = R.color.fg_insignificant
                            }
                        }
                        hintView.setTextColor(resources.getColor(colorResId))
                        hintView.text = getString(hintResId, btcFormat.format(viewModel.dryrunTransaction?.fee))
                    } else if (payment.mayEditAddress() && viewModel.validatedAddress != null
                        && wallet != null && wallet.isAddressMine(viewModel.validatedAddress!!.address)
                    ) {
                        hintView.setTextColor(resources.getColor(R.color.fg_insignificant))
                        hintView.visibility = View.VISIBLE
                        hintView.setText(R.string.send_coins_address_own)
                    }
                }
            }
            if (viewModel.sentTransaction != null && wallet != null) {
                transaction_row.visible()
                sentTransactionViewHolder
                    ?.bind(
                        ListItem.TransactionItem(
                            this, viewModel.sentTransaction!!,
                            wallet, addressBook, btcFormat, application.maxConnectedPeers(), false
                        )
                    )

            } else {
                transaction_row.gone()
            }

            viewCancel.isEnabled = (viewModel.state != SendViewModel.State.REQUEST_PAYMENT_REQUEST
                    && viewModel.state != SendViewModel.State.DECRYPTING
                    && viewModel.state != SendViewModel.State.SIGNING)
            viewGo.isEnabled = (everythingPlausible() && viewModel.dryrunTransaction != null && wallet != null
                    && fees != null && (blockchainState == null || !blockchainState.replaying))

            if (viewModel.state == null || viewModel.state == SendViewModel.State.REQUEST_PAYMENT_REQUEST) {
                viewCancel.setText(R.string.btn_cancel)
                viewGo.text = null
            } else if (viewModel.state == SendViewModel.State.INPUT) {
                viewCancel.setText(R.string.btn_cancel)
                viewGo.setText(R.string.btn_send)
            } else if (viewModel.state == SendViewModel.State.DECRYPTING) {
                viewCancel.setText(R.string.btn_cancel)
                viewGo.setText(R.string.state_decrypting)
            } else if (viewModel.state == SendViewModel.State.SIGNING) {
                viewCancel.setText(R.string.btn_cancel)
                viewGo.setText(R.string.preparation_msg)
            } else if (viewModel.state == SendViewModel.State.SENDING) {
                viewCancel.setText(R.string.btn_back)
                viewGo.setText(R.string.sending_msg)
            } else if (viewModel.state == SendViewModel.State.SENT) {
                viewCancel.setText(R.string.btn_back)
                viewGo.setText(R.string.sent_msg)
            } else if (viewModel.state == SendViewModel.State.FAILED) {
                viewCancel.setText(R.string.btn_back)
                viewGo.setText(R.string.failed_msg)
            }

            val privateKeyPasswordViewVisible =
                ((viewModel.state == SendViewModel.State.INPUT || viewModel.state == SendViewModel.State.DECRYPTING) && wallet != null
                        && wallet.isEncrypted)
            privateKeyPasswordViewGroup.visibility = if (privateKeyPasswordViewVisible) View.VISIBLE else View.GONE
            privateKeyPasswordView.isEnabled = viewModel.state == SendViewModel.State.INPUT

            // focus linking
            val activeAmountViewId = amountCalculatorLink?.activeTextView()?.id
            activeAmountViewId?.let {
                receivingAddressView.nextFocusDownId = it
                receivingAddressView.nextFocusForwardId = it
                amountCalculatorLink?.setNextFocusId(
                    if (privateKeyPasswordViewVisible) R.id.privateKeyPasswordView else R.id.viewGo
                )
                privateKeyPasswordView.nextFocusUpId = it
                privateKeyPasswordView.nextFocusDownId = R.id.viewGo
                privateKeyPasswordView.nextFocusForwardId = R.id.viewGo
                viewGo.nextFocusUpId =
                    if (privateKeyPasswordViewVisible) R.id.privateKeyPasswordView else it
            }
            if (fees != null) {
                textviewFeeAbsolute.text = btcFormat.format(fees[viewModel.feeCategory])
                textviewFeeAbsolute.visibility = View.VISIBLE
            }
        }
    }

    private fun setState(state: SendViewModel.State?) {
        viewModel.state = state
        invalidateOptionsMenu()
        updateView()
    }

    private val dryrunRunnable = object : Runnable {
        override fun run() {
            if (viewModel.state == SendViewModel.State.INPUT)
                executeDryrun()

            updateView()
        }

        private fun executeDryrun() {
            viewModel.dryrunTransaction = null
            viewModel.dryrunException = null

            val wallet = viewModel.wallet.value
            val fees = viewModel.dynamicFees.value
            val amount = amountCalculatorLink?.amount
            if (amount != null && fees != null) {
                try {
                    val dummy = wallet?.currentReceiveAddress() // won't be used, tx is never
                    // committed
                    val sendRequest = viewModel.paymentIntent?.mergeWithEditedValues(amount, dummy)
                        ?.toSendRequest()
                    sendRequest?.let { request ->
                        request.signInputs = false
                        viewModel.paymentIntent?.mayEditAmount()?.let {
                            request.emptyWallet =
                                it && amount == wallet?.getBalance(Wallet.BalanceType.AVAILABLE)
                        }
                        request.feePerKb = fees[viewModel.feeCategory]
                        wallet?.completeTx(request)
                        viewModel.dryrunTransaction = request.tx
                    }

                } catch (x: Exception) {
                    viewModel.dryrunException = x
                }

            }
        }
    }

    companion object {
        const val INTENT_EXTRA_PAYMENT_INTENT = "payment_intent"
        const val INTENT_EXTRA_FEE_CATEGORY = "fee_category"
        const val INTENT_EXTRA_KEY = "sweep_key"

        fun start(
            context: Context, paymentIntent: PaymentIntent,
            feeCategory: FeeCategory?, intentFlags: Int
        ) {
            val intent = Intent(context, SendCoinActivity::class.java)
            intent.putExtra(INTENT_EXTRA_PAYMENT_INTENT, paymentIntent)
            if (feeCategory != null)
                intent.putExtra(INTENT_EXTRA_FEE_CATEGORY, feeCategory)
            if (intentFlags != 0)
                intent.flags = intentFlags
            context.startActivity(intent)
        }

        fun start(context: Context, paymentIntent: PaymentIntent) {
            start(context, paymentIntent, null, 0)
        }

        fun start(context: Context, key: PrefixedChecksummedBytes) {
            /*val intent = Intent(context, SweepWalletActivity::class.java)todo
            intent.putExtra(INTENT_EXTRA_KEY, key)
            context.startActivity(intent)*/
        }
    }
}