package com.bitcoin.wallet.btc.model.price_new

import com.google.gson.annotations.SerializedName

data class PricesItem(

    @field:SerializedName("price")
    val price: String? = null,

    @field:SerializedName("time")
    val time: String? = null
)