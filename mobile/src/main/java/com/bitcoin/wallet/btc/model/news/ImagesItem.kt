package com.bitcoin.wallet.btc.model.news

import com.google.gson.annotations.SerializedName

data class ImagesItem(

    @field:SerializedName("description")
    val description: Any? = null,

    @field:SerializedName("url")
    val url: String? = null
)