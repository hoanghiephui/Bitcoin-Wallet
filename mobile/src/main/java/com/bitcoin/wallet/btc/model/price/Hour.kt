package com.bitcoin.wallet.btc.model.price

import com.google.gson.annotations.SerializedName

data class Hour(

    @field:SerializedName("prices")
    val prices: List<List<Any>>? = null,

    @field:SerializedName("percent_change")
    val percentChange: Double? = null
)