package com.bitcoin.wallet.btc.ui.activitys

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.data.DecodePrivateKeyTask
import com.bitcoin.wallet.btc.data.FeeCategory
import com.bitcoin.wallet.btc.data.PaymentIntent
import com.bitcoin.wallet.btc.data.RequestWalletBalanceTask
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.service.BlockchainService
import com.bitcoin.wallet.btc.ui.adapter.ListItem
import com.bitcoin.wallet.btc.ui.adapter.TransactionsWalletAdapter
import com.bitcoin.wallet.btc.ui.fragments.ProgressDialogFragment
import com.bitcoin.wallet.btc.ui.widget.DialogBuilder
import com.bitcoin.wallet.btc.utils.InputParser
import com.bitcoin.wallet.btc.utils.MonetarySpannable
import com.bitcoin.wallet.btc.utils.SendCoinsOfflineTask
import com.bitcoin.wallet.btc.viewmodel.SweepWalletViewModel
import com.google.common.collect.ComparisonChain
import kotlinx.android.synthetic.main.activity_sweep_wallet.*
import kotlinx.android.synthetic.main.item_transaction.*
import org.bitcoinj.core.*
import org.bitcoinj.crypto.BIP38PrivateKey
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.WalletTransaction
import java.util.*

class SweepWalletActivity : BaseActivity() {
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[SweepWalletViewModel::class.java]
    }
    private val config by lazy {
        application.config
    }
    private val handler = Handler()
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var sweepTransactionViewHolder: TransactionsWalletAdapter.TransactionViewHolder? = null
    private var reloadAction: MenuItem? = null
    private var scanAction: MenuItem? = null

    override fun layoutRes(): Int {
        return R.layout.activity_sweep_wallet
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar(getString(R.string.sweep_wallet))
        BlockchainService.start(this, false)
        viewModel.dynamicFees.observeNotNull(this) {
            updateView()
        }
        viewModel.progress.observe(this, ProgressDialogFragment.Observer(supportFragmentManager))
        backgroundThread = HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThread?.apply {
            start()
            backgroundHandler = Handler(this.looper)
        }

        if (savedInstanceState == null) {
            if (intent.hasExtra(INTENT_EXTRA_KEY)) {
                viewModel.privateKeyToSweep = intent
                    .getSerializableExtra(INTENT_EXTRA_KEY) as PrefixedChecksummedBytes?

                // delay until fragment is resumed
                handler.post(maybeDecodeKeyRunnable)
            }
        }
        transaction_row.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.transaction_layout_anim)
        sweepTransactionViewHolder = TransactionsWalletAdapter.TransactionViewHolder(container)

        viewGo.setOnClickListener {
            if (viewModel.state == SweepWalletViewModel.State.DECODE_KEY)
                handleDecrypt()
            if (viewModel.state == SweepWalletViewModel.State.CONFIRM_SWEEP)
                handleSweep()
        }
        viewCancel.setOnClickListener { finish() }
    }

    public override fun onDestroy() {
        backgroundThread?.looper?.quit()

        if (viewModel.sentTransaction != null)
            viewModel.sentTransaction?.confidence?.removeEventListener(sentTransactionConfidenceListener)

        super.onDestroy()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                val input = intent!!.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT)

                object : InputParser.StringInputParser(input) {
                    override fun handlePrivateKey(key: PrefixedChecksummedBytes) {
                        viewModel.privateKeyToSweep = key
                        setState(SweepWalletViewModel.State.DECODE_KEY)
                        maybeDecodeKey()
                    }

                    override fun handlePaymentIntent(paymentIntent: PaymentIntent) {
                        cannotClassify(input)
                    }

                    @Throws(VerificationException::class)
                    override fun handleDirectTransaction(transaction: Transaction) {
                        cannotClassify(input)
                    }

                    override fun error(messageResId: Int, vararg messageArgs: Any) {
                        dialog(this@SweepWalletActivity, null, R.string.btn_scan, messageResId, messageArgs)
                    }
                }.parse()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sweep, menu)
        reloadAction = menu?.findItem(R.id.sweep_wallet_options_reload)
        scanAction = menu?.findItem(R.id.sweep_wallet_options_scan)

        val pm = packageManager
        scanAction?.isVisible =
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.sweep_wallet_options_reload -> {
                handleReload()
            }
            R.id.sweep_wallet_options_scan -> {
                ScanActivity.startForResult(this, REQUEST_CODE_SCAN)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleReload() {
        if (viewModel.walletToSweep == null)
            return

        requestWalletBalance()
    }

    private fun setState(state: SweepWalletViewModel.State) {
        viewModel.state = state
        updateView()
    }

    private val sentTransactionConfidenceListener =
        TransactionConfidence.Listener { confidence, reason ->
            runOnUiThread {

                val confidence = viewModel.sentTransaction?.confidence
                val confidenceType = confidence?.confidenceType
                val numBroadcastPeers = confidence?.numBroadcastPeers()

                if (numBroadcastPeers != null && viewModel.state == SweepWalletViewModel.State.SENDING) {
                    if (confidenceType == TransactionConfidence.ConfidenceType.DEAD)
                        setState(SweepWalletViewModel.State.FAILED)
                    else if (numBroadcastPeers > 1 || confidenceType == TransactionConfidence.ConfidenceType.BUILDING)
                        setState(SweepWalletViewModel.State.SENT)
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
                                    "android.resource://" + packageName + "/" + soundResId
                                )
                            )
                            .play()
                }

                updateView()
            }
        }

    private val maybeDecodeKeyRunnable = Runnable { maybeDecodeKey() }

    private fun maybeDecodeKey() {
        if (viewModel.state != SweepWalletViewModel.State.DECODE_KEY || viewModel.privateKeyToSweep == null) {
            return
        }

        if (viewModel.privateKeyToSweep is DumpedPrivateKey) run {
            val key = (viewModel.privateKeyToSweep as DumpedPrivateKey).key
            askConfirmSweep(key)
        } else if (viewModel.privateKeyToSweep is BIP38PrivateKey) backgroundHandler?.let {
            run {
                badPasswordView.visibility =
                    View.INVISIBLE

                val password = passwordView.text.toString().trim { it <= ' ' }
                passwordView.text = null // get rid of it asap

                if (!password.isEmpty()) {
                    viewModel.progress.value = getString(R.string.sweep_wallet_decrypt_progress)

                    object : DecodePrivateKeyTask(it) {
                        override fun onSuccess(decryptedKey: ECKey) {
                            viewModel.progress.value = null

                            askConfirmSweep(decryptedKey)
                        }

                        override fun onBadPassphrase() {
                            viewModel.progress.value = null

                            badPasswordView.visibility = View.VISIBLE
                            passwordView.requestFocus()
                        }
                    }.decodePrivateKey(viewModel.privateKeyToSweep as BIP38PrivateKey, password)
                }
            }
        }
    }

    private fun askConfirmSweep(key: ECKey) {
        viewModel.walletToSweep = Wallet.createBasic(Constants.NETWORK_PARAMETERS)
        viewModel.walletToSweep?.importKey(key)

        setState(SweepWalletViewModel.State.CONFIRM_SWEEP)

        // delay until fragment is resumed
        handler.post(requestWalletBalanceRunnable)
    }

    private val requestWalletBalanceRunnable = Runnable { requestWalletBalance() }

    private val UTXO_COMPARATOR = Comparator<UTXO> { lhs, rhs ->
        ComparisonChain.start().compare(lhs.hash, rhs.hash).compare(lhs.index, rhs.index)
            .result()
    }

    private fun requestWalletBalance() {
        viewModel.progress.value = getString(R.string.sweep_wallet_balance_progress)

        val callback = object : RequestWalletBalanceTask.ResultCallback {
            override fun onResult(utxos: Set<UTXO>) {
                viewModel.progress.value = null

                // Filter UTXOs we've already spent and sort the rest.
                val walletTxns = application.getWallet().getTransactions(false)
                val sortedUtxos = TreeSet(UTXO_COMPARATOR)
                for (utxo in utxos)
                    if (!utxoSpentBy(walletTxns, utxo))
                        sortedUtxos.add(utxo)

                // Fake transaction funding the wallet to sweep.
                val fakeTxns = HashMap<Sha256Hash, Transaction>()
                for (utxo in sortedUtxos) {
                    var fakeTx: Transaction? = fakeTxns[utxo.hash]
                    if (fakeTx == null) {
                        fakeTx = FakeTransaction(Constants.NETWORK_PARAMETERS, utxo.hash, utxo.hash)
                        fakeTx.confidence.confidenceType = TransactionConfidence.ConfidenceType.BUILDING
                        fakeTxns[fakeTx.txId] = fakeTx
                    }
                    val fakeOutput = TransactionOutput(
                        Constants.NETWORK_PARAMETERS, fakeTx,
                        utxo.value, utxo.script.program
                    )
                    // Fill with output dummies as needed.
                    while (fakeTx.outputs.size < utxo.index)
                        fakeTx.addOutput(
                            TransactionOutput(
                                Constants.NETWORK_PARAMETERS, fakeTx,
                                Coin.NEGATIVE_SATOSHI, byteArrayOf()
                            )
                        )
                    // Add the actual output we will spend later.
                    fakeTx.addOutput(fakeOutput)
                }

                viewModel.walletToSweep?.clearTransactions(0)
                for (tx in fakeTxns.values)
                    viewModel.walletToSweep
                        ?.addWalletTransaction(WalletTransaction(WalletTransaction.Pool.UNSPENT, tx))

                updateView()
            }

            private fun utxoSpentBy(transactions: Set<Transaction>, utxo: UTXO): Boolean {
                for (tx in transactions) {
                    for (input in tx.inputs) {
                        val outpoint = input.outpoint
                        if (outpoint.hash == utxo.hash && outpoint.index == utxo.index)
                            return true
                    }
                }
                return false
            }

            override fun onFail(messageResId: Int, vararg messageArgs: Any) {
                viewModel.progress.value = null

                val dialog = DialogBuilder.warn(
                    this@SweepWalletActivity,
                    R.string.sweep_wallet_balance_faile
                )
                dialog.setMessage(getString(messageResId, *messageArgs))
                dialog.setPositiveButton(R.string.btn_retry,
                    { _, which -> requestWalletBalance() })
                dialog.setNegativeButton(R.string.btn_dismiss, null)
                dialog.show()
            }
        }

        val key = viewModel.walletToSweep?.importedKeys?.iterator()?.next()
        RequestWalletBalanceTask(backgroundHandler, callback).requestWalletBalance(assets, key)
    }

    private fun updateView() {
        val fees = viewModel.dynamicFees.value
        val btcFormat = config.format

        if (viewModel.walletToSweep != null) {
            balanceView.visibility = View.VISIBLE
            val balanceSpannable = MonetarySpannable(
                btcFormat,
                viewModel.walletToSweep?.getBalance(Wallet.BalanceType.ESTIMATED)
            )
            balanceSpannable.applyMarkup(null, null)
            val balance = SpannableStringBuilder(balanceSpannable)
            balance.insert(0, ": ")
            balance.insert(0, getString(R.string.sweep_wallet_balance))
            balanceView.text = balance
        } else {
            balanceView.visibility = View.GONE
        }

        if (viewModel.state === SweepWalletViewModel.State.DECODE_KEY && viewModel.privateKeyToSweep == null) {
            messageView1.visibility = View.VISIBLE
            messageView1.setText(R.string.sweep_wallet_unknown)
        } else if (viewModel.state == SweepWalletViewModel.State.DECODE_KEY && viewModel.privateKeyToSweep != null) {
            messageView1.visibility = View.VISIBLE
            messageView1.setText(R.string.encrypted_sweep_wallet)
        } else if (viewModel.privateKeyToSweep != null) {
            messageView1.visibility = View.GONE
        }

        passwordViewGroup.visibility =
            if (viewModel.state == SweepWalletViewModel.State.DECODE_KEY && viewModel.privateKeyToSweep != null)
                View.VISIBLE
            else
                View.GONE

        hintView.visibility =
            if (viewModel.state == SweepWalletViewModel.State.DECODE_KEY && viewModel.privateKeyToSweep == null)
                View.VISIBLE
            else
                View.GONE

        if (viewModel.sentTransaction != null) {
            transaction_row.visibility = View.VISIBLE
            sweepTransactionViewHolder
                ?.bind(
                    ListItem.TransactionItem(
                        this, viewModel.sentTransaction!!,
                        application.getWallet(), null, btcFormat, application.maxConnectedPeers(), false
                    )
                )
        } else {
            transaction_row.visibility = View.GONE
        }

        if (viewModel.state == SweepWalletViewModel.State.DECODE_KEY) {
            viewCancel.setText(R.string.btn_cancel)
            viewGo.setText(R.string.sweep_wallet_btn_decrypt)
            viewGo.isEnabled = viewModel.privateKeyToSweep != null
        } else if (viewModel.state == SweepWalletViewModel.State.CONFIRM_SWEEP) {
            viewCancel.setText(R.string.btn_cancel)
            viewGo.setText(R.string.sweep_wallet_btn_sweep)
            viewGo.isEnabled = (viewModel.walletToSweep != null
                    && viewModel.walletToSweep!!.getBalance(Wallet.BalanceType.ESTIMATED).signum() > 0 && fees != null)
        } else if (viewModel.state == SweepWalletViewModel.State.PREPARATION) {
            viewCancel.setText(R.string.btn_cancel)
            viewGo.setText(R.string.preparation_msg)
            viewGo.isEnabled = false
        } else if (viewModel.state == SweepWalletViewModel.State.SENDING) {
            viewCancel.setText(R.string.btn_back)
            viewGo.setText(R.string.sending_msg)
            viewGo.isEnabled = false
        } else if (viewModel.state == SweepWalletViewModel.State.SENT) {
            viewCancel.setText(R.string.btn_back)
            viewGo.setText(R.string.sent_msg)
            viewGo.isEnabled = false
        } else if (viewModel.state == SweepWalletViewModel.State.FAILED) {
            viewCancel.setText(R.string.btn_back)
            viewGo.setText(R.string.failed_msg)
            viewGo.isEnabled = false
        }

        viewCancel.isEnabled = viewModel.state != SweepWalletViewModel.State.PREPARATION

        // enable actions
        reloadAction?.isEnabled =
            viewModel.state == SweepWalletViewModel.State.CONFIRM_SWEEP && viewModel.walletToSweep != null

        scanAction?.isEnabled =
            viewModel.state == SweepWalletViewModel.State.DECODE_KEY || viewModel.state == SweepWalletViewModel.State.CONFIRM_SWEEP
    }

    private fun handleDecrypt() {
        handler.post(maybeDecodeKeyRunnable)
    }

    private fun handleSweep() {
        setState(SweepWalletViewModel.State.PREPARATION)

        val fees = viewModel.dynamicFees.value
        val sendRequest = SendRequest.emptyWallet(application.getWallet().freshReceiveAddress())
        sendRequest.feePerKb = fees?.get(FeeCategory.NORMAL)

        object : SendCoinsOfflineTask(viewModel.walletToSweep, backgroundHandler) {
            override fun onSuccess(transaction: Transaction?) {
                viewModel.sentTransaction = transaction

                setState(SweepWalletViewModel.State.SENDING)

                viewModel.sentTransaction?.confidence?.addEventListener(sentTransactionConfidenceListener)

                viewModel.sentTransaction?.let { application.processDirectTransaction(it) }
            }

            override fun onInsufficientMoney(missing: Coin?) {
                setState(SweepWalletViewModel.State.FAILED)

                showInsufficientMoneyDialog()
            }

            override fun onEmptyWalletFailed() {
                setState(SweepWalletViewModel.State.FAILED)

                showInsufficientMoneyDialog()
            }

            override fun onFailure(exception: Exception) {
                setState(SweepWalletViewModel.State.FAILED)

                val dialog = DialogBuilder.warn(this@SweepWalletActivity, R.string.error_msg)
                dialog.setMessage(exception.toString())
                dialog.setNeutralButton(R.string.btn_dismiss, null)
                dialog.show()
            }

            override fun onInvalidEncryptionKey() {
                throw RuntimeException() // cannot happen
            }

            private fun showInsufficientMoneyDialog() {
                val dialog = DialogBuilder.warn(
                    this@SweepWalletActivity,
                    R.string.sweep_wallet_money
                )
                dialog.setMessage(R.string.sweep_wallet_money_msg)
                dialog.setNeutralButton(R.string.btn_dismiss, null)
                dialog.show()
            }
        }.sendCoinsOffline(sendRequest) // send asynchronously
    }

    private class FakeTransaction(
        params: NetworkParameters,
        private val txId: Sha256Hash,
        private val wTxId: Sha256Hash
    ) : Transaction(params) {

        override fun getTxId(): Sha256Hash {
            return txId
        }

        override fun getWTxId(): Sha256Hash {
            return wTxId
        }
    }

    companion object {
        const val INTENT_EXTRA_KEY = "sweep_key"
        private const val REQUEST_CODE_SCAN = 0
        fun start(context: Context) {
            context.startActivity(Intent(context, SweepWalletActivity::class.java))
        }

        fun start(context: Context, key: PrefixedChecksummedBytes) {
            val intent = Intent(context, SweepWalletActivity::class.java)
            intent.putExtra(INTENT_EXTRA_KEY, key)
            context.startActivity(intent)
        }
    }

}