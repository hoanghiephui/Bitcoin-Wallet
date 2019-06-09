package com.bitcoin.wallet.btc.model.price

import com.google.gson.annotations.SerializedName

data class Data(

    @field:SerializedName("base_id")
    val baseId: String? = null,

    @field:SerializedName("currency")
    val currency: String? = null,

    @field:SerializedName("unit_price_scale")
    val unitPriceScale: Int? = null,

    @field:SerializedName("prices")
    val prices: Prices? = null,

    @field:SerializedName("base")
    val base: String? = null
)