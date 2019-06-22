package com.bitcoin.wallet.btc.model.index

import com.google.gson.annotations.SerializedName

data class Time(

    @field:SerializedName("iso")
    val iso: String? = null,

    @field:SerializedName("unix")
    val unix: Int? = null
)