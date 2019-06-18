package com.bitcoin.wallet.btc.model.explorer.transaction

import com.google.gson.annotations.SerializedName

data class ScriptSig(

    @field:SerializedName("hex")
    val hex: String? = null,

    @field:SerializedName("asm")
    val asm: String? = null
)