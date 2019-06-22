package com.bitcoin.wallet.btc.model.index

import com.google.gson.annotations.SerializedName

data class LookupResponse(

    @field:SerializedName("lookup")
    val lookup: Lookup? = null,

    @field:SerializedName("close")
    val close: Close? = null,

    @field:SerializedName("open")
    val open: Open? = null
)