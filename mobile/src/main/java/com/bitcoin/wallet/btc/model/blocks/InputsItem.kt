package com.bitcoin.wallet.btc.model.blocks

import com.google.gson.annotations.SerializedName

data class InputsItem(

    @field:SerializedName("sequence")
    val sequence: Long? = null,

    @field:SerializedName("witness")
    val witness: String? = null,

    @field:SerializedName("script")
    val script: String? = null
)