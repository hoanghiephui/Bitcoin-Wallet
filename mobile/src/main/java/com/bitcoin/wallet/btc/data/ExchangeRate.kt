package com.bitcoin.wallet.btc.data

import androidx.core.util.Preconditions.checkNotNull

class ExchangeRate(val rate: org.bitcoinj.utils.ExchangeRate, val source: String) {

    val currencyCode: String
        get() = rate.fiat.currencyCode

    init {
        checkNotNull(rate.fiat.currencyCode)
    }

    override fun toString(): String {
        return javaClass.simpleName + '['.toString() + rate.fiat + ']'.toString()
    }
}
