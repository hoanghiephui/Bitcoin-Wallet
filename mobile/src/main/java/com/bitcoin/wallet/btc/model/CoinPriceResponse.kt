package com.bitcoin.wallet.btc.model

import com.google.gson.annotations.SerializedName

data class CoinPriceResponse(
    @field:SerializedName("data")
    val data: Data? = null
)

data class Data(
    @field:SerializedName("BTC")
    val bTC: Double? = null,

    @field:SerializedName("BCH")
    val bCH: Double? = null
)