package com.bitcoin.wallet.btc.ui.fragments

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.data.FeeCategory
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.service.BlockchainService
import com.bitcoin.wallet.btc.ui.widget.DialogBuilder
import com.bitcoin.wallet.btc.utils.Configuration
import com.bitcoin.wallet.btc.utils.DeriveKeyTask
import com.bitcoin.wallet.btc.utils.WalletUtils
import com.bitcoin.wallet.btc.viewmodel.RaiseFeeViewModel
import dagger.android.support.DaggerDialogFragment
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.crypto.KeyCrypterException
import org.bitcoinj.wallet.KeyChain
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.bouncycastle.crypto.params.KeyParameter
import javax.inject.Inject

class RaiseFeeDialogFragment : DaggerDialogFragment() {

    private val activity: BaseActivity by lazy {
        context as BaseActivity
    }
    private var application: BitcoinApplication? = null
    private var config: Configuration? = null
    private var wallet: Wallet? = null

    private var feeRaise: Coin? = null
    private var transaction: Transaction? = null

    private var dialog: AlertDialog? = null

    private var messageView: TextView? = null
    private var passwordGroup: View? = null
    private var passwordView: EditText? = null
    private var badPasswordView: View? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: RaiseFeeViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(RaiseFeeViewModel::class.java)
    }

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var state = State.INPUT

    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            badPasswordView?.visibility = View.INVISIBLE
            updateView()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable) {}
    }

    private enum class State {
        INPUT, DECRYPTING, DONE
    }

    //private static final Logger log = LoggerFactory.getLogger(RaiseFeeDialogFragment.class);todo log

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.application = activity.application
        this.config = application!!.config
        this.wallet = application!!.getWallet()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.dynamicFees.observeNotNull(this) {
            val size = transaction?.messageSize?.plus(192)
            feeRaise = size?.toLong()?.let { it1 -> it[FeeCategory.PRIORITY]?.multiply(it1)?.divide(1000) }
            updateView()
        }

        val args = arguments
        if (args?.getByteArray(KEY_TRANSACTION) == null) {
            return
        }
        transaction = wallet?.getTransaction(Sha256Hash.wrap(args.getByteArray(KEY_TRANSACTION)!!))

        backgroundThread = HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_raise_fee, null)

        messageView = view.findViewById(R.id.raise_fee_dialog_message)

        passwordGroup = view.findViewById(R.id.raise_fee_dialog_password_group)

        passwordView = view.findViewById(R.id.raise_fee_dialog_password)
        passwordView!!.text = null

        badPasswordView = view.findViewById(R.id.raise_fee_dialog_bad_password)

        val builder = DialogBuilder(activity)
        builder.setTitle(R.string.raise_fee)
        builder.setView(view)
        // dummies, just to make buttons show
        builder.setPositiveButton(R.string.raise, null)
        builder.setNegativeButton(R.string.btn_dismiss, null)
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener { d ->
            positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)

            positiveButton?.typeface = Typeface.DEFAULT_BOLD
            positiveButton?.setOnClickListener { v -> handleGo() }
            negativeButton?.setOnClickListener { v -> dismissAllowingStateLoss() }

            passwordView?.addTextChangedListener(textWatcher)

            this@RaiseFeeDialogFragment.dialog = dialog
            updateView()
        }

        //log.info("showing raise fee dialog");

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface?) {
        this.dialog = null

        wipePasswords()

        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        backgroundThread?.looper?.quit()

        super.onDestroy()
    }

    private fun handleGo() {
        state = State.DECRYPTING
        updateView()

        if (wallet?.isEncrypted == true) {
            object : DeriveKeyTask(backgroundHandler, application!!.scryptIterationsTarget()) {
                override fun onSuccess(encryptionKey: KeyParameter, wasChanged: Boolean) {
                    if (wasChanged)
                        WalletUtils.autoBackupWallet(activity!!, wallet)
                    doRaiseFee(encryptionKey)
                }
            }.deriveKey(wallet!!, passwordView!!.text.toString().trim { it <= ' ' })

            updateView()
        } else {
            doRaiseFee(null)
        }
    }

    private fun doRaiseFee(encryptionKey: KeyParameter?) {
        // construct child-pays-for-parent
        if (findSpendableOutput(wallet, transaction!!, feeRaise) == null) {
            return
        }
        val outputToSpend = findSpendableOutput(wallet, transaction!!, feeRaise) ?: return
        val transactionToSend = Transaction(Constants.NETWORK_PARAMETERS)
        transactionToSend.addInput(outputToSpend)
        transactionToSend.addOutput(
            outputToSpend.value.subtract(feeRaise!!),
            wallet!!.freshAddress(KeyChain.KeyPurpose.CHANGE)
        )
        transactionToSend.purpose = Transaction.Purpose.RAISE_FEE

        val sendRequest = SendRequest.forTx(transactionToSend)
        sendRequest.aesKey = encryptionKey

        try {
            wallet!!.signTransaction(sendRequest)

            //log.info("raise fee: cpfp {}", transactionToSend);

            wallet!!.commitTx(transactionToSend)
            BlockchainService.broadcastTransaction(activity, transactionToSend)

            state = State.DONE
            updateView()

            dismiss()
        } catch (x: KeyCrypterException) {
            badPasswordView?.visibility = View.VISIBLE

            state = State.INPUT
            updateView()

            passwordView!!.requestFocus()

            //log.info("raise fee: bad spending password");
        }

    }

    private fun wipePasswords() {
        passwordView?.text = null
    }

    private fun updateView() {
        if (dialog == null)
            return

        val needsPassword = wallet!!.isEncrypted

        if (feeRaise == null) {
            messageView!!.setText(R.string.raise_fee_fee)
            passwordGroup!!.visibility = View.GONE
        } else if (findSpendableOutput(wallet, transaction!!, feeRaise) == null) {
            messageView!!.setText(R.string.raise_fee_cant_raise)
            passwordGroup!!.visibility = View.GONE
        } else {
            messageView!!.text = getString(R.string.raise_fee_meg, config!!.format.format(feeRaise!!))
            passwordGroup!!.visibility = if (needsPassword) View.VISIBLE else View.GONE
        }

        if (state == State.INPUT) {
            positiveButton!!.setText(R.string.raise)
            positiveButton!!.isEnabled =
                ((!needsPassword || passwordView!!.text.toString().trim { it <= ' ' }.length > 0)
                        && feeRaise != null && findSpendableOutput(wallet, transaction!!, feeRaise) != null)
            negativeButton!!.isEnabled = true
        } else if (state == State.DECRYPTING) {
            positiveButton!!.setText(R.string.state_decrypting_)
            positiveButton!!.isEnabled = false
            negativeButton!!.isEnabled = false
        } else if (state == State.DONE) {
            positiveButton!!.setText(R.string.state_done)
            positiveButton!!.isEnabled = false
            negativeButton!!.isEnabled = false
        }
    }

    companion object {
        private val FRAGMENT_TAG = RaiseFeeDialogFragment::class.java.name
        private val KEY_TRANSACTION = "transaction"

        fun show(fm: FragmentManager, tx: Transaction) {
            val newFragment = instance(tx)
            newFragment.show(fm, FRAGMENT_TAG)
        }

        private fun instance(tx: Transaction): RaiseFeeDialogFragment {
            val fragment = RaiseFeeDialogFragment()

            val args = Bundle()
            args.putByteArray(KEY_TRANSACTION, tx.txId.bytes)
            fragment.arguments = args

            return fragment
        }

        fun feeCanLikelyBeRaised(wallet: Wallet, transaction: Transaction): Boolean {
            if (transaction.confidence.depthInBlocks > 0)
                return false

            return if (WalletUtils.isPayToManyTransaction(transaction)) false else findSpendableOutput(
                wallet,
                transaction,
                Transaction.DEFAULT_TX_FEE
            ) != null

            // We don't know dynamic fees here, so we need to guess.
        }

        private fun findSpendableOutput(
            wallet: Wallet?, transaction: Transaction,
            minimumOutputValue: Coin?
        ): TransactionOutput? {
            for (output in transaction.outputs) {
                if (output.isMine(wallet) && output.isAvailableForSpending
                    && output.value.isGreaterThan(minimumOutputValue!!)
                )
                    return output
            }

            return null
        }
    }
}
