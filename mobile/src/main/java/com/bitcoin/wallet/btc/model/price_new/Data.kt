package com.bitcoin.wallet.btc.model.price_new

import com.google.gson.annotations.SerializedName

data class Data(

    @field:SerializedName("currency")
    val currency: String? = null,

    @field:SerializedName("prices")
    val prices: List<PricesItem>? = null,

    @field:SerializedName("base")
    val base: String? = null
)