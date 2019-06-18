package com.bitcoin.wallet.btc.model.explorer.details

import com.google.gson.annotations.SerializedName

data class BlockDetailResponse(

    @field:SerializedName("reward")
    val reward: Double? = null,

    @field:SerializedName("tx")
    val tx: List<String>? = null,

    @field:SerializedName("previousblockhash")
    val previousblockhash: String? = null,

    @field:SerializedName("nextblockhash")
    val nextblockhash: String? = null,

    @field:SerializedName("bits")
    val bits: String? = null,

    @field:SerializedName("isMainChain")
    val isMainChain: Boolean? = null,

    @field:SerializedName("confirmations")
    val confirmations: Int? = null,

    @field:SerializedName("version")
    val version: Int? = null,

    @field:SerializedName("nonce")
    val nonce: Long? = null,

    @field:SerializedName("difficulty")
    val difficulty: Double? = null,

    @field:SerializedName("chainwork")
    val chainwork: String? = null,

    @field:SerializedName("size")
    val size: Int? = null,

    @field:SerializedName("merkleroot")
    val merkleroot: String? = null,

    @field:SerializedName("time")
    val time: Long? = null,

    @field:SerializedName("poolInfo")
    val poolInfo: PoolInfo? = null,

    @field:SerializedName("hash")
    val hash: String? = null,

    @field:SerializedName("height")
    val height: Int? = null
)