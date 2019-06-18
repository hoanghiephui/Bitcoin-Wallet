package com.bitcoin.wallet.btc.model.explorer.transaction

import com.google.gson.annotations.SerializedName

data class ScriptPubKey(

    @field:SerializedName("addresses")
    val addresses: List<String>? = null,

    @field:SerializedName("hex")
    val hex: String? = null,

    @field:SerializedName("asm")
    val asm: String? = null,

    @field:SerializedName("type")
    val type: String? = null
)