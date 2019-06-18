package com.bitcoin.wallet.btc.model.explorer

import com.google.gson.annotations.SerializedName

data class BlocksResponse(

    @field:SerializedName("pagination")
    val pagination: Pagination? = null,

    @field:SerializedName("blocks")
    val blocks: List<BlocksItem>? = null,

    @field:SerializedName("length")
    val length: Int? = null
)