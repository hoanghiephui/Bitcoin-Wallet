package com.bitcoin.wallet.btc.data.live

import android.content.SharedPreferences
import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.loader.content.CursorLoader
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.BuildConfig
import com.bitcoin.wallet.btc.data.ExchangeRate
import com.bitcoin.wallet.btc.data.ExchangeRatesProvider
import com.bitcoin.wallet.btc.utils.Configuration

class SelectedExchangeRateLiveData(application: BitcoinApplication) : LiveData<ExchangeRate>(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val config: Configuration = application.config
    private val loader: CursorLoader

    init {
        this.loader = object : CursorLoader(
            application,
            ExchangeRatesProvider.contentUri(BuildConfig.APPLICATION_ID, false), null,
            ExchangeRatesProvider.KEY_CURRENCY_CODE, arrayOf<String>(), null
        ) {
            override fun deliverResult(cursor: Cursor?) {
                if (cursor != null && cursor.count > 0) {
                    cursor.moveToFirst()
                    value = ExchangeRatesProvider.getExchangeRate(cursor)
                }
            }
        }
    }

    override fun onActive() {
        loader.startLoading()
        config.registerOnSharedPreferenceChangeListener(this)
        onCurrencyChange()
    }

    override fun onInactive() {
        config.unregisterOnSharedPreferenceChangeListener(this)
        loader.stopLoading()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (Configuration.PREFS_KEY_EXCHANGE_CURRENCY == key)
            onCurrencyChange()
    }

    private fun onCurrencyChange() {
        val exchangeCurrency = config.exchangeCurrencyCode
        loader.selectionArgs = arrayOf(exchangeCurrency)
        loader.forceLoad()
    }
}