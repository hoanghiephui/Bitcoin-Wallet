package com.bitcoin.wallet.btc.model.blocks

import com.google.gson.annotations.SerializedName

data class Coinbase(

    @field:SerializedName("lock_time")
    val lockTime: Int? = null,

    @field:SerializedName("ver")
    val ver: Int? = null,

    @field:SerializedName("size")
    val size: Int? = null,

    @field:SerializedName("inputs")
    val inputs: List<InputsItem>? = null,

    @field:SerializedName("weight")
    val weight: Int? = null,

    @field:SerializedName("time")
    val time: Int? = null,

    @field:SerializedName("tx_index")
    val txIndex: Int? = null,

    @field:SerializedName("vin_sz")
    val vinSz: Int? = null,

    @field:SerializedName("hash")
    val hash: String? = null,

    @field:SerializedName("vout_sz")
    val voutSz: Int? = null,

    @field:SerializedName("relayed_by")
    val relayedBy: String? = null,

    @field:SerializedName("out")
    val out: List<OutItem>? = null,

    @field:SerializedName("rbf")
    val rbf: Boolean? = null
)