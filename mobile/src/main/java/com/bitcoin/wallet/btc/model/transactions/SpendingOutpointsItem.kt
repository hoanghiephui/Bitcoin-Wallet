package com.bitcoin.wallet.btc.model.transactions

import com.google.gson.annotations.SerializedName

data class SpendingOutpointsItem(

    @field:SerializedName("tx_index")
    val txIndex: Int? = null,

    @field:SerializedName("n")
    val N: Int? = null
)