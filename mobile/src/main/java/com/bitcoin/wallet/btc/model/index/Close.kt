package com.bitcoin.wallet.btc.model.index

import com.google.gson.annotations.SerializedName

data class Close(

    @field:SerializedName("price")
    val price: Int? = null,

    @field:SerializedName("time")
    val time: Time? = null
)