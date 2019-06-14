package com.bitcoin.wallet.btc.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.service.BlockchainService
import com.bitcoin.wallet.btc.ui.activitys.SettingActivity
import com.bitcoin.wallet.btc.ui.widget.DialogBuilder
import com.bitcoin.wallet.btc.utils.Configuration
import com.bitcoin.wallet.btc.utils.Configuration.Companion.PREFS_KEY_TRUSTED_PEER
import com.bitcoin.wallet.btc.utils.Configuration.Companion.PREFS_KEY_TRUSTED_PEER_ONLY
import com.bitcoin.wallet.btc.utils.Qr
import com.bitcoin.wallet.btc.utils.ResolveDnsTask
import java.net.InetAddress
import java.util.*

class SettingFragment: PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    lateinit var sharedPreferences: SharedPreferences
    private var config: Configuration? = null

    private val handler = Handler()
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var trustedPeerPreference: Preference? = null
    private var trustedPeerOnlyPreference: Preference? = null
    private val PREFS_KEY_INITIATE_RESET = "initiate_reset"
    private val PREFS_KEY_EXTENDED_PUBLIC_KEY = "extended_public_key"

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (activity == null) {
            return
        }
        this.config = (activity?.application as BitcoinApplication).config
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val isDark = sharedPreferences.getBoolean("dark", false)
        if (isDark) {
            addPreferencesFromResource(R.xml.config_dark)
        } else {
            addPreferencesFromResource(R.xml.config)
        }

        val darkMode: SwitchPreference = findPreference("dark_preference") as SwitchPreference
        darkMode.setDefaultValue(isDark)
        darkMode.isChecked = isDark
        darkMode.onPreferenceChangeListener = this

        val currency = findPreference("notify_coin")
        currency.onPreferenceChangeListener = this

        backgroundThread = HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThread?.let {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
        trustedPeerPreference = findPreference(Configuration.PREFS_KEY_TRUSTED_PEER)
        trustedPeerPreference?.onPreferenceChangeListener = this

        trustedPeerOnlyPreference = findPreference(Configuration.PREFS_KEY_TRUSTED_PEER_ONLY)
        trustedPeerOnlyPreference?.onPreferenceChangeListener = this
        updateTrustedPeer()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        val key = preference?.key
        if (PREFS_KEY_INITIATE_RESET == key) {
            handleInitiateReset()
            return true
        } else if (PREFS_KEY_EXTENDED_PUBLIC_KEY == key) {
            handleExtendedPublicKey()
            return true
        }

        return false
    }

    override fun onDestroy() {
        trustedPeerOnlyPreference?.onPreferenceChangeListener = null
        trustedPeerPreference?.onPreferenceChangeListener = null
        backgroundThread?.looper?.quit()
        super.onDestroy()
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when (preference?.key) {
            "dark_preference" -> {
                if (::sharedPreferences.isInitialized) {
                    sharedPreferences.edit {
                        putBoolean("dark", newValue as Boolean)
                    }
                    onReStart()
                }
            }
            "notify_coin" -> {
                if (::sharedPreferences.isInitialized) {
                    sharedPreferences.edit {
                        putBoolean("notify_coin", newValue as Boolean)
                    }
                }
            }
            PREFS_KEY_TRUSTED_PEER -> {
                handler.post {
                    BlockchainService.stop(activity)
                    updateTrustedPeer()
                }
            }
            PREFS_KEY_TRUSTED_PEER_ONLY -> {
                BlockchainService.stop(activity)
            }
        }
        return true
    }

    private fun onReStart() {
        if (activity != null) {
            (activity as? SettingActivity)?.onRestartApp()
        }
    }

    private fun updateTrustedPeer() {
        if (activity == null || !isAdded) {
            return
        }
        val trustedPeer = config?.trustedPeerHost

        if (trustedPeer == null) {
            trustedPeerPreference?.setSummary(R.string.trusted_peer_summary)
            trustedPeerOnlyPreference?.isEnabled = false
        } else {
            trustedPeerPreference?.summary = trustedPeer + "\n[" + getString(R.string.trusted_peer_resolve_progress) + "]"
            trustedPeerOnlyPreference?.isEnabled = true

            object : ResolveDnsTask(backgroundHandler) {
                override fun onSuccess(address: InetAddress) {
                    trustedPeerPreference?.summary = trustedPeer
                }

                override fun onUnknownHost() {
                    if (activity == null || !isAdded) {
                        return
                    }
                    trustedPeerPreference?.summary = (trustedPeer + "\n["
                            + getString(R.string.trusted_peer_resolve_unknown_host) + "]")
                }
            }.resolve(trustedPeer)
        }
    }

    private fun handleInitiateReset() {
        val dialog = DialogBuilder(requireContext())
        dialog.setTitle(R.string.initiate_reset)
        dialog.setMessage(R.string.initiate_reset_message)
        dialog.setPositiveButton(R.string.initiate_reset_btn
        ) { _, _ ->
            BlockchainService.resetBlockchain(activity)
            activity?.finish()
        }
        dialog.setNegativeButton(R.string.btn_dismiss, null)
        dialog.show()
    }

    private fun handleExtendedPublicKey() {
        if (activity == null) {
            return
        }
        val activeKeyChain = (activity?.application as BitcoinApplication).getWallet().activeKeyChain
        val extendedKey = activeKeyChain.watchingKey
        val outputScriptType = activeKeyChain.outputScriptType
        val creationTimeSeconds = extendedKey.creationTimeSeconds
        val base58 = String.format(
            Locale.US, "%s?c=%d&h=bip32",
            extendedKey.serializePubB58(Constants.NETWORK_PARAMETERS, outputScriptType), creationTimeSeconds
        )
        activity?.let {
            BitmapBottomDialog.show(it, Qr.bitmap(base58))
        }
    }
}