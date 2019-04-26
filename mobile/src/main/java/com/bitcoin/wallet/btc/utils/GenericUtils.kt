package com.bitcoin.wallet.btc.utils

import java.util.Currency

object GenericUtils {
    @JvmStatic
    fun startsWithIgnoreCase(string: String, prefix: String): Boolean {
        return string.regionMatches(0, prefix, 0, prefix.length, ignoreCase = true)
    }

    @JvmStatic
    fun currencySymbol(currencyCode: String): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            currency.symbol
        } catch (x: IllegalArgumentException) {
            currencyCode
        }

    }
}
