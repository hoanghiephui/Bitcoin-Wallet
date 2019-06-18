package com.bitcoin.wallet.btc.api

import com.bitcoin.wallet.btc.model.explorer.BlocksResponse
import com.bitcoin.wallet.btc.model.explorer.address.AddressResponse
import com.bitcoin.wallet.btc.model.explorer.details.BlockDetailResponse
import com.bitcoin.wallet.btc.model.explorer.transaction.TransactionsResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface BitcoinEndpoints {
    @GET("blocks/")
    fun getLatestBlocks(@Query("limit") limit: String): Observable<BlocksResponse>

    @GET("blocks/")
    fun getLatestBlocksByDate(@Query("blockDate") blockDate: String): Observable<BlocksResponse>

    @GET("block/{hash}")
    fun getBlockDetailsByHash(@Path("hash") hash: String): Observable<BlockDetailResponse>

    //https://explorer.bitcoin.com/api/btc/txs/?block=0000000000000000031c10163a0f088ff59064f31afae48510a9171b96e03deb&pageNum=0
    @GET
    fun getListTransaction(@Url url: String): Observable<TransactionsResponse>

    //https://explorer.bitcoin.com/api/btc/addr/1LuZmXJfzf73ooUcC7BHKB92gjLEGv7eCh
    @GET
    fun getAddress(@Url url: String): Observable<AddressResponse>

    //https://explorer.bitcoin.com/api/btc/txs/?address=1LuZmXJfzf73ooUcC7BHKB92gjLEGv7eCh&pageNum=0
    @GET
    fun getListTransactionByAddress(@Url url: String): Observable<TransactionsResponse>
}