package com.bitcoin.wallet.btc.model.stats

import com.google.gson.annotations.SerializedName

data class StatsResponse(

    @field:SerializedName("data")
    val data: Data? = null
)