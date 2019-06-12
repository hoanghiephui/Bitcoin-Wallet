package com.bitcoin.wallet.btc.model.transactions

import com.google.gson.annotations.SerializedName

data class TxItem(

    @field:SerializedName("ver")
    val ver: Int? = null,

    @field:SerializedName("inputs")
    val inputs: List<InputsItem>? = null,

    @field:SerializedName("fee")
    val fee: Long? = null,

    @field:SerializedName("weight")
    val weight: Int? = null,

    @field:SerializedName("block_height")
    val blockHeight: Int? = null,

    @field:SerializedName("relayed_by")
    val relayedBy: String? = null,

    @field:SerializedName("out")
    val out: List<OutItem>? = null,

    @field:SerializedName("lock_time")
    val lockTime: Int? = null,

    @field:SerializedName("size")
    val size: Int? = null,

    @field:SerializedName("rbf")
    val rbf: Boolean? = null,

    @field:SerializedName("double_spend")
    val doubleSpend: Boolean? = null,

    @field:SerializedName("block_index")
    val blockIndex: Int? = null,

    @field:SerializedName("time")
    val time: Long? = null,

    @field:SerializedName("tx_index")
    val txIndex: Int? = null,

    @field:SerializedName("vin_sz")
    val vinSz: Int? = null,

    @field:SerializedName("hash")
    val hash: String? = null,

    @field:SerializedName("vout_sz")
    val voutSz: Int? = null
)