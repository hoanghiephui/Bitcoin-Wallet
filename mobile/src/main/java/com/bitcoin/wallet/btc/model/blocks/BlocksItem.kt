package com.bitcoin.wallet.btc.model.blocks

import com.google.gson.annotations.SerializedName

data class BlocksItem(

    @field:SerializedName("tx_count")
    val txCount: Int? = null,

    @field:SerializedName("coinbase")
    val coinbase: Coinbase? = null,

    @field:SerializedName("size")
    val size: Int? = null,

    @field:SerializedName("total_sent")
    val totalSent: Long? = null,

    @field:SerializedName("weight")
    val weight: Int? = null,

    @field:SerializedName("time")
    val time: Int? = null,

    @field:SerializedName("hash")
    val hash: String? = null,

    @field:SerializedName("height")
    val height: Int? = null
)