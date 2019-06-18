package com.bitcoin.wallet.btc.model.explorer.address

import com.google.gson.annotations.SerializedName

data class AddressResponse(

    @field:SerializedName("unconfirmedBalance")
    val unconfirmedBalance: Int? = null,

    @field:SerializedName("totalReceivedSat")
    val totalReceivedSat: Long? = null,

    @field:SerializedName("balance")
    val balance: Double? = null,

    @field:SerializedName("balanceSat")
    val balanceSat: Long? = null,

    @field:SerializedName("totalSentSat")
    val totalSentSat: Long? = null,

    @field:SerializedName("unconfirmedTxApperances")
    val unconfirmedTxApperances: Int? = null,

    @field:SerializedName("addrStr")
    val addrStr: String? = null,

    @field:SerializedName("totalReceived")
    val totalReceived: Double? = null,

    @field:SerializedName("totalSent")
    val totalSent: Double? = null,

    @field:SerializedName("unconfirmedBalanceSat")
    val unconfirmedBalanceSat: Long? = null,

    @field:SerializedName("transactions")
    val transactions: List<String>? = null,

    @field:SerializedName("txApperances")
    val txApperances: Int? = null
)