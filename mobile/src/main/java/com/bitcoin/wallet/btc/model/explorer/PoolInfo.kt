package com.bitcoin.wallet.btc.model.explorer

import com.google.gson.annotations.SerializedName

data class PoolInfo(

    @field:SerializedName("url")
    val url: String? = null,

    @field:SerializedName("poolName")
    val poolName: String? = null
)