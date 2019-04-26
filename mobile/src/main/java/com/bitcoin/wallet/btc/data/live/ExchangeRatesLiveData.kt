package com.bitcoin.wallet.btc.data.live

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.loader.content.CursorLoader
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.data.ExchangeRatesProvider
import com.google.common.base.Strings

class ExchangeRatesLiveData(application: BitcoinApplication) : LiveData<Cursor>() {
    private val loader: CursorLoader

    init {
        this.loader = object : CursorLoader(
            application,
            ExchangeRatesProvider.contentUri(application.packageName, false), null,
            ExchangeRatesProvider.QUERY_PARAM_Q, arrayOf(""), null
        ) {
            override fun deliverResult(cursor: Cursor?) {
                if (cursor != null)
                    value = cursor
            }
        }
    }

    override fun onActive() {
        loader.startLoading()
    }

    override fun onInactive() {
        loader.stopLoading()
    }

    fun setQuery(query: String) {
        loader.selectionArgs = arrayOf(Strings.nullToEmpty(query))
        loader.forceLoad()
    }
}
