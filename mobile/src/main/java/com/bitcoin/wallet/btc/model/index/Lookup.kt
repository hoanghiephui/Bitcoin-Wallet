package com.bitcoin.wallet.btc.model.index

import com.google.gson.annotations.SerializedName

data class Lookup(

    @field:SerializedName("price")
    val price: Long? = null,

    @field:SerializedName("k")
    val K: Int? = null,

    @field:SerializedName("time")
    val time: Time? = null
)