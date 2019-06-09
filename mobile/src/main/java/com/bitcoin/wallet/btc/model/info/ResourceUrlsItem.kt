package com.bitcoin.wallet.btc.model.info

import com.google.gson.annotations.SerializedName

data class ResourceUrlsItem(

    @field:SerializedName("icon_url")
    val iconUrl: String? = null,

    @field:SerializedName("link")
    val link: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("title")
    val title: String? = null
)