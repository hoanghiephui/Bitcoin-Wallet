package com.bitcoin.wallet.btc.model.explorer

import com.google.gson.annotations.SerializedName

data class Pagination(

    @field:SerializedName("next")
    val next: String? = null,

    @field:SerializedName("current")
    val current: String? = null,

    @field:SerializedName("moreTs")
    val moreTs: Int? = null,

    @field:SerializedName("more")
    val more: Boolean? = null,

    @field:SerializedName("prev")
    val prev: String? = null,

    @field:SerializedName("isToday")
    val isToday: Boolean? = null,

    @field:SerializedName("currentTs")
    val currentTs: Int? = null
)