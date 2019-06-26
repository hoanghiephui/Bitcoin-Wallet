package com.bitcoin.wallet.btc.ui.fragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.FilesWallet
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.btc.crypto.Crypto
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.ui.activitys.MainActivity
import com.bitcoin.wallet.btc.ui.activitys.WalletTransactionsActivity
import com.bitcoin.wallet.btc.utils.Event
import com.bitcoin.wallet.btc.utils.Iso8601Format
import com.bitcoin.wallet.btc.viewmodel.BackupWalletViewModel
import com.google.common.io.CharStreams
import kotlinx.android.synthetic.main.dialog_backup.*
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.WalletProtobufSerializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

class BackupDialog : BaseBottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: BackupWalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[BackupWalletViewModel::class.java]
    }

    override fun layoutRes(): Int {
        return R.layout.dialog_backup
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        passwordView.addTextChangedListener(textWatcher)
        passwordAgainView.addTextChangedListener(textWatcher)

        viewModel.wallet.observeNotNull(viewLifecycleOwner) {
            warningView.visibility = if (it.isEncrypted) View.VISIBLE else View.GONE
        }

        viewModel.password.observeNotNull(viewLifecycleOwner) {
            passwordMismatchView.visibility = View.INVISIBLE

            val passwordLength = it.length
            passwordStrengthView.visibility = if (passwordLength > 0) View.VISIBLE else View.INVISIBLE
            when {
                passwordLength < 6 -> {
                    passwordStrengthView.setText(R.string.encrypt_strength_weak)
                    context?.let { ContextCompat.getColor(it, R.color.fg_error) }?.let {
                        passwordStrengthView
                            .setTextColor(it)
                    }
                }
                passwordLength < 8 -> {
                    passwordStrengthView.setText(R.string.encrypt_keys_dialog_password_strength_fair)
                    context?.let { ContextCompat.getColor(it, R.color.scan_dot) }?.let {
                        passwordStrengthView
                            .setTextColor(it)
                    }
                }
                passwordLength < 10 -> {
                    passwordStrengthView.setText(R.string.encrypt_kstrength_good)
                    context?.let { ContextCompat.getColor(it, R.color.fg_less_significant) }?.let {
                        passwordStrengthView.setTextColor(
                            it
                        )
                    }
                }
                else -> {
                    passwordStrengthView.setText(R.string.encrypt_strength_strong)
                    context?.let { ContextCompat.getColor(it, R.color.color_coin) }?.let {
                        passwordStrengthView
                            .setTextColor(it)
                    }
                }
            }

            val hasPassword = it.isNotEmpty()
            val hasPasswordAgain = passwordAgainView?.text.toString().trim { it <= ' ' }.isNotEmpty()
            viewGo.isEnabled = viewModel.wallet.value != null && hasPassword && hasPasswordAgain
        }
        if (requireActivity() is MainActivity) {
            (requireActivity() as MainActivity).viewModel.backupStatus.observeNotNull(viewLifecycleOwner) {
                onBackup(it.contentOrThrow)
            }
        }


        viewCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        viewGo?.setOnClickListener {
            handleGo()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        passwordView?.removeTextChangedListener(textWatcher)
        passwordAgainView?.removeTextChangedListener(textWatcher)

        super.onDismiss(dialog)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT) {
            if (resultCode == Activity.RESULT_OK) {
                onBackup(data?.data)
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onBackup(data: Uri?) {
        viewModel.wallet.observe(this, object : Observer<Wallet> {
            override fun onChanged(wallet: Wallet?) {
                viewModel.wallet.removeObserver(this)
                val targetUri = data
                val target = targetUri?.let { uriToTarget(it) }
                val password = passwordView?.text.toString().trim { it <= ' ' }
                if (password.isEmpty()) {
                    return
                }
                dismissAllowingStateLoss()
                var plainBytes: ByteArray? = null
                targetUri?.let { url ->
                    try {
                        OutputStreamWriter(
                            activity?.contentResolver?.openOutputStream(url), StandardCharsets.UTF_8
                        ).use { cipherOut ->
                            val walletProto = WalletProtobufSerializer().walletToProto(wallet)
                            val baos = ByteArrayOutputStream()
                            walletProto.writeTo(baos)
                            baos.close()
                            plainBytes = baos.toByteArray()
                            val cipherText = Crypto.encrypt(plainBytes, password.toCharArray())
                            cipherOut.write(cipherText)
                            cipherOut.flush()
                        }
                    } catch (x: IOException) {
                        activity?.let {
                            if (it is MainActivity) {
                                it.viewModel.backupWalletStatus.value =
                                    Event(BackUpStatus(false, x.toString()))
                            } else if (it is WalletTransactionsActivity) {
                                it.viewModel.backupWalletStatus.value =
                                    Event(BackUpStatus(false, x.toString()))
                            }
                        }
                    }

                    try {
                        InputStreamReader(
                            activity?.contentResolver?.openInputStream(url), StandardCharsets.UTF_8
                        ).use { cipherIn ->
                            val cipherText = StringBuilder()
                            CharStreams.copy(cipherIn, cipherText)
                            cipherIn.close()

                            val plainBytes2 = Crypto.decryptBytes(
                                cipherText.toString(),
                                password.toCharArray()
                            )
                            if (!Arrays.equals(plainBytes, plainBytes2))
                                throw IOException("verification failed")

                            activity?.let {
                                (it.application as BitcoinApplication).config
                                    .disarmBackupReminder()
                                if (it is MainActivity) {
                                    it.viewModel.backupWalletStatus.value =
                                        Event(BackUpStatus(true, target ?: url.toString()))
                                } else if (it is WalletTransactionsActivity) {
                                    it.viewModel.backupWalletStatus.value =
                                        Event(BackUpStatus(true, target ?: url.toString()))
                                }
                            }
                        }
                    } catch (x: IOException) {
                        activity?.let {
                            if (it is MainActivity) {
                                it.viewModel.backupWalletStatus.value =
                                    Event(BackUpStatus(false, x.toString()))
                            } else if (it is WalletTransactionsActivity) {
                                it.viewModel.backupWalletStatus.value =
                                    Event(BackUpStatus(false, x.toString()))
                            }
                        }
                    }
                }


            }

        })
    }

    private fun uriToTarget(uri: Uri): String? {
        if (uri.scheme != "content")
            return null
        val host = uri.host
        if ("com.google.android.apps.docs.storage" == host)
            return "Google Drive"
        if ("com.box.android.documents" == host)
            return "Box"
        return if ("com.android.providers.downloads.documents" == host) "internal storage" else null
    }

    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            viewModel.password.postValue(s.toString().trim { it <= ' ' })
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable) {}
    }

    private fun handleGo() {
        val password = passwordView?.text.toString().trim { it <= ' ' }
        val passwordAgain = passwordAgainView?.text.toString().trim { it <= ' ' }

        if (passwordAgain == password) {
            backupWallet()
        } else {
            passwordMismatchView?.visibility = View.VISIBLE
        }
    }

    private fun backupWallet() {
        passwordView?.isEnabled = false
        passwordAgainView?.isEnabled = false

        val dateFormat = Iso8601Format("yyyy-MM-dd-HH-mm")
        dateFormat.timeZone = TimeZone.getDefault()

        val filename = StringBuilder(FilesWallet.EXTERNAL_WALLET_BACKUP)
        filename.append('-')
        filename.append(dateFormat.format(Date()))

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIMETYPE_WALLET_BACKUP
        intent.putExtra(Intent.EXTRA_TITLE, filename.toString())
        startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT)
    }

    companion object {
        fun show(activity: AppCompatActivity) {
            val fragment = BackupDialog()
            fragment.isCancelable = false
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }

        const val REQUEST_CODE_CREATE_DOCUMENT = 123
    }

    data class BackUpStatus(
        val isStatus: Boolean,
        val mes: String
    )
}