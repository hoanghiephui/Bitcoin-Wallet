package com.bitcoin.wallet.btc.model.news

import com.google.gson.annotations.SerializedName

data class Pagination(

    @field:SerializedName("ending_before")
    val endingBefore: Any? = null,

    @field:SerializedName("previous_ending_before")
    val previousEndingBefore: Any? = null,

    @field:SerializedName("limit")
    val limit: Int? = null,

    @field:SerializedName("previous_uri")
    val previousUri: Any? = null,

    @field:SerializedName("next_starting_after")
    val nextStartingAfter: String? = null,

    @field:SerializedName("starting_after")
    val startingAfter: Any? = null,

    @field:SerializedName("next_uri")
    val nextUri: String? = null,

    @field:SerializedName("order")
    val order: String? = null
)