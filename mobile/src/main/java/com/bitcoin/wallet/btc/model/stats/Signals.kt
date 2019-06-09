package com.bitcoin.wallet.btc.model.stats

import com.google.gson.annotations.SerializedName

data class Signals(

    @field:SerializedName("percent_holding")
    val percentHolding: PercentHolding? = null,

    @field:SerializedName("price_correlations")
    val priceCorrelations: List<PriceCorrelationsItem>? = null
)