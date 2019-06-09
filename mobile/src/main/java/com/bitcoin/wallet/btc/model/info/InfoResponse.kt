package com.bitcoin.wallet.btc.model.info

import com.google.gson.annotations.SerializedName

data class InfoResponse(

    @field:SerializedName("data")
    val data: List<Data>? = null
)