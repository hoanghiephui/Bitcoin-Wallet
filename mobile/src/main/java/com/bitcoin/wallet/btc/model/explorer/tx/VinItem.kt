package com.bitcoin.wallet.btc.model.explorer.tx

import com.google.gson.annotations.SerializedName

data class VinItem(

    @field:SerializedName("sequence")
    val sequence: Long? = null,

    @field:SerializedName("scriptSig")
    val scriptSig: ScriptSig? = null,

    @field:SerializedName("valueSat")
    val valueSat: Int? = null,

    @field:SerializedName("txid")
    val txid: String? = null,

    @field:SerializedName("addr")
    val addr: String? = null,

    @field:SerializedName("doubleSpentTxID")
    val doubleSpentTxID: Any? = null,

    @field:SerializedName("value")
    val value: Double? = null,

    @field:SerializedName("n")
    val N: Int? = null,

    @field:SerializedName("vout")
    val vout: Int? = null
)