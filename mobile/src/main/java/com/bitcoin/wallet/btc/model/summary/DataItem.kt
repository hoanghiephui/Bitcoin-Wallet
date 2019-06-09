package com.bitcoin.wallet.btc.model.summary

import com.google.gson.annotations.SerializedName

data class DataItem(

    @field:SerializedName("market_cap")
    val marketCap: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("currency")
    val currency: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("unit_price_scale")
    val unitPriceScale: Int? = null,

    @field:SerializedName("percent_change")
    val percentChange: Double? = null,

    @field:SerializedName("base")
    val base: String? = null,

    @field:SerializedName("latest")
    val latest: String? = null
)