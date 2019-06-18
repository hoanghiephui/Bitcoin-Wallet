package com.bitcoin.wallet.btc.model.explorer.transaction

import com.google.gson.annotations.SerializedName

data class VoutItem(

    @field:SerializedName("scriptPubKey")
    val scriptPubKey: ScriptPubKey? = null,

    @field:SerializedName("spentIndex")
    val spentIndex: Any? = null,

    @field:SerializedName("spentHeight")
    val spentHeight: Any? = null,

    @field:SerializedName("spentTxId")
    val spentTxId: Any? = null,

    @field:SerializedName("value")
    val value: String? = null,

    @field:SerializedName("n")
    val N: Int? = null
)