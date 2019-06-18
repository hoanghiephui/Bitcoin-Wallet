package com.bitcoin.wallet.btc.model.explorer.transaction

import com.google.gson.annotations.SerializedName

data class TransactionsResponse(

    @field:SerializedName("txs")
    val txs: List<TxsItem>? = null,

    @field:SerializedName("pagesTotal")
    val pagesTotal: Int? = null
)