package com.bitcoin.wallet.btc.model.stats

import com.google.gson.annotations.SerializedName

data class PriceCorrelationsItem(

    @field:SerializedName("correlation")
    val correlation: Double? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("id")
    val id: String? = null
)