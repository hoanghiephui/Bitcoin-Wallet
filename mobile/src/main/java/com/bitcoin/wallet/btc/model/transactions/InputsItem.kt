package com.bitcoin.wallet.btc.model.transactions

import com.google.gson.annotations.SerializedName

data class InputsItem(

    @field:SerializedName("sequence")
    val sequence: Long? = null,

    @field:SerializedName("witness")
    val witness: String? = null,

    @field:SerializedName("prev_out")
    val prevOut: PrevOut? = null,

    @field:SerializedName("script")
    val script: String? = null
)