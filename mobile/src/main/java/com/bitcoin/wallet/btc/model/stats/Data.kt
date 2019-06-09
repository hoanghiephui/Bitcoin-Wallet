package com.bitcoin.wallet.btc.model.stats

import com.google.gson.annotations.SerializedName

data class Data(

    @field:SerializedName("circulating_supply")
    val circulatingSupply: String? = null,

    @field:SerializedName("base_id")
    val baseId: String? = null,

    @field:SerializedName("total_supply")
    val totalSupply: String? = null,

    @field:SerializedName("all_time_high")
    val allTimeHigh: String? = null,

    @field:SerializedName("percent_change_1h")
    val percentChange1h: Double? = null,

    @field:SerializedName("market_cap")
    val marketCap: String? = null,

    @field:SerializedName("percent_change_24h")
    val percentChange24h: Double? = null,

    @field:SerializedName("signals")
    val signals: Signals? = null,

    @field:SerializedName("max_supply")
    val maxSupply: String? = null,

    @field:SerializedName("volume_24h")
    val volume24h: String? = null,

    @field:SerializedName("currency")
    val currency: String? = null,

    @field:SerializedName("percent_change_7d")
    val percentChange7d: Double? = null,

    @field:SerializedName("base")
    val base: String? = null
)