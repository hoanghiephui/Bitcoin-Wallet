package com.bitcoin.wallet.btc.model.explorer.tx

import com.google.gson.annotations.SerializedName

data class TxResponse(

    @field:SerializedName("fees")
    val fees: Double? = null,

    @field:SerializedName("locktime")
    val locktime: Int? = null,

    @field:SerializedName("txid")
    val txid: String? = null,

    @field:SerializedName("confirmations")
    val confirmations: Int? = null,

    @field:SerializedName("version")
    val version: Int? = null,

    @field:SerializedName("vout")
    val vout: List<VoutItem>? = null,

    @field:SerializedName("blockheight")
    val blockheight: Int? = null,

    @field:SerializedName("valueOut")
    val valueOut: Double? = null,

    @field:SerializedName("blockhash")
    val blockhash: String? = null,

    @field:SerializedName("size")
    val size: Int? = null,

    @field:SerializedName("blocktime")
    val blocktime: Long? = null,

    @field:SerializedName("valueIn")
    val valueIn: Double? = null,

    @field:SerializedName("vin")
    val vin: List<VinItem>? = null,

    @field:SerializedName("time")
    val time: Long? = null,

    @field:SerializedName("isCoinBase")
    val isCoinBase: Boolean? = null
)