package com.bitcoin.wallet.btc.ui.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.extension.visible
import com.bitcoin.wallet.btc.ui.activitys.MainActivity
import com.bitcoin.wallet.btc.utils.WalletUtils
import com.google.common.base.Strings
import kotlinx.android.synthetic.main.dialog_encrypt.*
import org.bitcoinj.crypto.KeyCrypterException
import org.bitcoinj.crypto.KeyCrypterScrypt

class EncryptKeysDialogFragment : BaseBottomSheetDialogFragment() {
    private val handler = Handler()
    private val backgroundThread: HandlerThread by lazy {
        HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND)
    }
    private var backgroundHandler: Handler? = null
    private val wallet by lazy {
        (requireActivity().application as BitcoinApplication).getWallet()
    }

    private enum class State {
        INPUT, CRYPTING, DONE
    }

    private var state = State.INPUT
    override fun layoutRes(): Int {
        return R.layout.dialog_encrypt
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)

        passwordAgainView.addTextChangedListener(textWatcher)
        passwordView.addTextChangedListener(textWatcher)
        updateView()
        viewGo.setOnClickListener {
            handleGo()
        }
        viewCancel.setOnClickListener {
            dismissDialog()
        }
    }

    override fun onResume() {
        updateView()
        super.onResume()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        passwordAgainView.removeTextChangedListener(textWatcher)
        passwordView.removeTextChangedListener(textWatcher)
        wipePasswords()
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        backgroundThread.looper.quit()
        super.onDestroy()
    }

    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            passwordStrengthView.gone()
            updateView()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable) {}
    }

    private fun handleGo() {
        val oldPassword = Strings.emptyToNull(passwordView.text.toString().trim { it <= ' ' })
        val newPassword = Strings.emptyToNull(passwordAgainView.text.toString().trim { it <= ' ' })
        state = State.CRYPTING
        updateView()
        backgroundHandler?.post {
            val oldKey = if (oldPassword != null) wallet.keyCrypter?.deriveKey(oldPassword) else null
            val keyCrypter =
                KeyCrypterScrypt((requireActivity().application as BitcoinApplication).scryptIterationsTarget())
            val newKey = if (newPassword != null) keyCrypter.deriveKey(newPassword) else null
            handler.post {
                if (wallet.isEncrypted) {
                    if (oldKey == null) {
                        state = State.INPUT
                        passwordView.requestFocus()
                    } else {
                        try {
                            wallet.decrypt(oldKey)
                            state = State.DONE
                        } catch (x: KeyCrypterException) {
                            passwordStrengthView.visible()
                            state = State.INPUT
                            passwordView.requestFocus()
                        }

                    }
                }

                if (newKey != null && !wallet.isEncrypted) {
                    wallet.encrypt(keyCrypter, newKey)
                    state = State.DONE
                }

                updateView()

                if (state == State.DONE) {
                    WalletUtils.autoBackupWallet(activity, wallet)
                    // trigger load manually because of missing callbacks for encryption state
                    (requireActivity() as MainActivity).viewModel.walletEncrypted.load()
                    handler.postDelayed({ dismiss() }, 2000)
                }
            }
        }
    }

    private fun wipePasswords() {
        passwordAgainView.text = null
        passwordView.text = null
    }

    private fun updateView() {
        if (passwordView == null) {
            return
        }
        val hasOldPassword = passwordView.text.toString().trim { it <= ' ' }.isNotEmpty()
        val hasPassword = passwordAgainView.text.toString().trim { it <= ' ' }.isNotEmpty()
        pass.visibility = if (wallet.isEncrypted) View.VISIBLE else View.GONE
        passwordView.isEnabled = state == State.INPUT

        passwordAgainView.isEnabled = state == State.INPUT

        val passwordLength = passwordAgainView.text?.length ?: 0
        passwordMismatchView.visibility =
            if (state == State.INPUT && passwordLength > 0) View.VISIBLE else View.INVISIBLE
        if (passwordLength < 4) {
            passwordMismatchView.setText(R.string.encrypt_strength_weak)
            passwordMismatchView.setTextColor(ContextCompat.getColor(requireContext(), R.color.fg_error))
        } else if (passwordLength < 6) {
            passwordMismatchView.setText(R.string.encrypt_keys_dialog_password_strength_fair)
            passwordMismatchView.setTextColor(ContextCompat.getColor(requireContext(), R.color.scan_dot))
        } else if (passwordLength < 8) {
            passwordMismatchView.setText(R.string.encrypt_kstrength_good)
            passwordMismatchView.setTextColor(ContextCompat.getColor(requireContext(), R.color.fg_less_significant))
        } else {
            passwordMismatchView.setText(R.string.encrypt_strength_strong)
            passwordMismatchView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_coin))
        }

        if (state == State.INPUT) {
            if (wallet.isEncrypted) {
                viewGo.setText(if (hasPassword) R.string.btn_edit else R.string.btn_remove)
                viewGo.isEnabled = hasOldPassword
            } else {
                viewGo.setText(R.string.btn_set)
                viewGo.isEnabled = hasPassword
            }

            viewCancel.isEnabled = true
        } else if (state == State.CRYPTING) {
            viewGo.setText(
                if (passwordAgainView.text.toString().trim { it <= ' ' }.isEmpty())
                    R.string.encrypt_decrypting
                else
                    R.string.state_encrypting
            )
            viewGo.isEnabled = false
            viewCancel.isEnabled = false
        } else if (state == State.DONE) {
            viewGo.setText(R.string.encrypt_done)
            viewGo.isEnabled = false
            viewCancel.isEnabled = false
        }
    }

    companion object {
        fun show(activity: AppCompatActivity) {
            val fragment = EncryptKeysDialogFragment()
            fragment.show(activity.supportFragmentManager, EncryptKeysDialogFragment::class.java.simpleName)
        }
    }
}