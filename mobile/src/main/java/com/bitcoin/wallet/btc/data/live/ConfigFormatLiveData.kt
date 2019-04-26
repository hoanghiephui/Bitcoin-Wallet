package com.bitcoin.wallet.btc.data.live

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.utils.Configuration
import org.bitcoinj.utils.MonetaryFormat

class ConfigFormatLiveData(application: BitcoinApplication) : LiveData<MonetaryFormat>(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val config: Configuration = application.config

    override fun onActive() {
        config.registerOnSharedPreferenceChangeListener(this)
        value = config.format
    }

    override fun onInactive() {
        config.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (Configuration.PREFS_KEY_BTC_PRECISION == key)
            value = config.format
    }
}