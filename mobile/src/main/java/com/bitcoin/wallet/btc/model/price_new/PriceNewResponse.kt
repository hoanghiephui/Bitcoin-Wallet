package com.bitcoin.wallet.btc.model.price_new

import com.google.gson.annotations.SerializedName

data class PriceNewResponse(

    @field:SerializedName("data")
    val data: Data? = null
)