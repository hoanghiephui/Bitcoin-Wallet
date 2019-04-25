package com.bitcoin.wallet.mobile.model

import com.google.gson.annotations.SerializedName

data class StatsResponse(

    @field:SerializedName("market_price_usd")
    val marketPriceUsd: Double? = null,

    @field:SerializedName("hash_rate")
    val hashRate: Double? = null,

    @field:SerializedName("total_fees_btc")
    val totalFeesBtc: Long? = null,

    @field:SerializedName("n_btc_mined")
    val nBtcMined: Long? = null,

    @field:SerializedName("n_tx")
    val nTx: Int? = null,

    @field:SerializedName("n_blocks_mined")
    val nBlocksMined: Int? = null,

    @field:SerializedName("minutes_between_blocks")
    val minutesBetweenBlocks: Double? = null,

    @field:SerializedName("totalbc")
    val totalbc: Long? = null,

    @field:SerializedName("n_blocks_total")
    val nBlocksTotal: Int? = null,

    @field:SerializedName("trade_volume_usd")
    val tradeVolumeUsd: Double? = null,

    @field:SerializedName("estimated_transaction_volume_usd")
    val estimatedTransactionVolumeUsd: Double? = null,

    @field:SerializedName("blocks_size")
    val blocksSize: Int? = null,

    @field:SerializedName("miners_revenue_usd")
    val minersRevenueUsd: Double? = null,

    @field:SerializedName("nextretarget")
    val nextretarget: Int? = null,

    @field:SerializedName("difficulty")
    val difficulty: Long? = null,

    @field:SerializedName("trade_volume_btc")
    val tradeVolumeBtc: Double? = null,

    @field:SerializedName("estimated_btc_sent")
    val estimatedBtcSent: Long? = null,

    @field:SerializedName("miners_revenue_btc")
    val minersRevenueBtc: Long? = null,

    @field:SerializedName("total_btc_sent")
    val totalBtcSent: Long? = null,

    @field:SerializedName("timestamp")
    val timestamp: Long? = null
)