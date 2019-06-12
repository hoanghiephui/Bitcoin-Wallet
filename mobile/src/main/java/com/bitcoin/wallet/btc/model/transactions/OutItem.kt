package com.bitcoin.wallet.btc.model.transactions

import com.google.gson.annotations.SerializedName

data class OutItem(

    @field:SerializedName("spent")
    val spent: Boolean? = null,

    @field:SerializedName("type")
    val type: Int? = null,

    @field:SerializedName("tx_index")
    val txIndex: Int? = null,

    @field:SerializedName("addr")
    val addr: String? = null,

    @field:SerializedName("value")
    val value: Long? = null,

    @field:SerializedName("script")
    val script: String? = null,

    @field:SerializedName("n")
    val N: Int? = null
)