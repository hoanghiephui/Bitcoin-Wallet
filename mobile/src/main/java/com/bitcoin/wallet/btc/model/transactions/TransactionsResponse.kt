package com.bitcoin.wallet.btc.model.transactions

import com.google.gson.annotations.SerializedName

data class TransactionsResponse(

    @field:SerializedName("ver")
    val ver: Int? = null,

    @field:SerializedName("next_block")
    val nextBlock: List<String>? = null,

    @field:SerializedName("prev_block")
    val prevBlock: String? = null,

    @field:SerializedName("mrkl_root")
    val mrklRoot: String? = null,

    @field:SerializedName("tx")
    val tx: List<TxItem>? = null,

    @field:SerializedName("n_tx")
    val nTx: Int? = null,

    @field:SerializedName("fee")
    val fee: Int? = null,

    @field:SerializedName("main_chain")
    val mainChain: Boolean? = null,

    @field:SerializedName("bits")
    val bits: Int? = null,

    @field:SerializedName("nonce")
    val nonce: Long? = null,

    @field:SerializedName("size")
    val size: Int? = null,

    @field:SerializedName("block_index")
    val blockIndex: Int? = null,

    @field:SerializedName("time")
    val time: Int? = null,

    @field:SerializedName("hash")
    val hash: String? = null,

    @field:SerializedName("height")
    val height: Int? = null
)