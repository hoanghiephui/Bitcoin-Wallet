package com.bitcoin.wallet.btc.model.blocks

import com.google.gson.annotations.SerializedName

data class BlocksResponse(

    @field:SerializedName("blocks")
    val blocks: List<BlocksItem>? = null
)