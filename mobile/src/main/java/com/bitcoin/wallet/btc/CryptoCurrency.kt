package com.bitcoin.wallet.btc

import android.content.Context
import android.util.TypedValue

enum class CryptoCurrency(
    val symbol: String,
    val unit: String,
    val dp: Int,           // max decimal places
    val userDp: Int,       // user decimal places
    val requiredConfirmations: Int,
    private val featureFlags: Long
) {
    BTC(
        symbol = "BTC",
        unit = "Bitcoin",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3,
        featureFlags =
        CryptoCurrency.PRICE_CHARTING or
                CryptoCurrency.MULTI_WALLET

    ),
    ETHER(
        symbol = "ETH",
        unit = "Ether",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12,
        featureFlags =
        CryptoCurrency.PRICE_CHARTING
    ),
    BCH(
        symbol = "BCH",
        unit = "Bitcoin Cash",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3,
        featureFlags =
        CryptoCurrency.PRICE_CHARTING or
                CryptoCurrency.MULTI_WALLET
    ),
    XLM(
        symbol = "XLM",
        unit = "Stellar",
        dp = 7,
        userDp = 7,
        requiredConfirmations = 1,
        featureFlags =
        CryptoCurrency.PRICE_CHARTING
    ),
    PAX(
        symbol = "PAX",
        unit = "USD PAX",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 3, // Same as ETHER
        featureFlags =
        0L
    );

    fun hasFeature(feature: Long): Boolean = (0L != (featureFlags and feature))

    companion object {

        fun fromSymbol(symbol: String?): CryptoCurrency? =
            values().firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }

        fun fromSymbolOrThrow(symbol: String?): CryptoCurrency =
            fromSymbol(symbol) ?: throw IllegalArgumentException("Bad currency symbol \"$symbol\"")

        const val PRICE_CHARTING = 0x00000001L
        const val MULTI_WALLET = 0x00000002L

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