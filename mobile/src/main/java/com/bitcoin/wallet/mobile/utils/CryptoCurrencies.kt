package com.bitcoin.wallet.mobile.utils

import android.content.Context
import android.util.TypedValue

enum class CryptoCurrencies(val symbol: String, val unit: String) {
    BTC("BTC", "Bitcoin"),
    ETHER("ETH", "Ether"),
    BCH("BCH", "Bitcoin Cash");

    companion object {

        fun fromString(text: String): CryptoCurrencies? =
            CryptoCurrencies.values().firstOrNull { it.symbol.equals(text, ignoreCase = true) }

        @JvmStatic
        fun getTextColor(context: Context, attrId: Int): Int {
            val tV = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(attrId, tV, true)
            return tV.data
        }

    }
}

enum class TimeSpan {
    YEAR,
    MONTH,
    WEEK,
    DAY,
    ALL_TIME
}

enum class TimeSpans {
    YEAR,
    MONTH,
    WEEK,
    DAY,
    HR
}

/**
 * All time start times in epoch-seconds
 */

// 2010-08-18 00:00:00 UTC
const val FIRST_BTC_ENTRY_TIME = 1282089600L
// 2015-08-08 00:00:00 UTC
const val FIRST_ETH_ENTRY_TIME = 1438992000L
// 2017-07-24 00:00:00 UTC
const val FIRST_BCH_ENTRY_TIME = 1500854400L