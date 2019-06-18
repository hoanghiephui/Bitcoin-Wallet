package com.bitcoin.wallet.btc.model.explorer

import com.google.gson.annotations.SerializedName

data class BlocksItem(

    @field:SerializedName("size")
    val size: Int? = null,

    @field:SerializedName("txlength")
    val txlength: Int? = null,

    @field:SerializedName("time")
    val time: Long? = null,

    @field:SerializedName("poolInfo")
    val poolInfo: PoolInfo? = null,

    @field:SerializedName("hash")
    val hash: String? = null,

    @field:SerializedName("height")
    val height: Int? = null
)