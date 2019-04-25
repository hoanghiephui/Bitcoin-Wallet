package com.bitcoin.wallet.mobile.model

import com.google.gson.annotations.SerializedName

data class PriceDatum(
    @field:SerializedName("timestamp")
    val timestamp: Long,
    @field:SerializedName("price")
    val price: Double,
    @field:SerializedName("volume24h")
    val volume24h: Double? = null
)