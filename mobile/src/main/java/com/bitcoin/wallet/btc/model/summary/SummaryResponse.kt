package com.bitcoin.wallet.btc.model.summary

import com.google.gson.annotations.SerializedName

data class SummaryResponse(

    @field:SerializedName("data")
    val data: List<DataItem>? = null
)