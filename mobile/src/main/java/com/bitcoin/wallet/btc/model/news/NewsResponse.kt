package com.bitcoin.wallet.btc.model.news

import com.google.gson.annotations.SerializedName

data class NewsResponse(

    @field:SerializedName("pagination")
    val pagination: Pagination? = null,

    @field:SerializedName("data")
    val data: List<DataItem>? = null
)