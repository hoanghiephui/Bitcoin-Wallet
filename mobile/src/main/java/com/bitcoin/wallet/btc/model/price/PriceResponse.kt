package com.bitcoin.wallet.btc.model.price

import com.google.gson.annotations.SerializedName

data class PriceResponse(

    @field:SerializedName("data")
    val data: Data? = null
)