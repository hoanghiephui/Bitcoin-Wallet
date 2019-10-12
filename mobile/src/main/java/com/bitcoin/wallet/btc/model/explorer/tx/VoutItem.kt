package com.bitcoin.wallet.btc.model.explorer.tx

import com.google.gson.annotations.SerializedName

data class VoutItem(

    @field:SerializedName("scriptPubKey")
    val scriptPubKey: ScriptPubKey? = null,

    @field:SerializedName("spentIndex")
    val spentIndex: Int? = null,

    @field:SerializedName("spentHeight")
    val spentHeight: Int? = null,

    @field:SerializedName("spentTxId")
    val spentTxId: String? = null,

    @field:SerializedName("value")
    val value: String? = null,

    @field:SerializedName("n")
    val N: Int? = null
)