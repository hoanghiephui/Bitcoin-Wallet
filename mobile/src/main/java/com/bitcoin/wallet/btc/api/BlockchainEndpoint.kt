package com.bitcoin.wallet.btc.api

import com.bitcoin.wallet.btc.model.PriceDatum
import com.bitcoin.wallet.btc.model.StatsResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface BlockchainEndpoint {
    /**
     * get list data chart blockchain.info
     */
    @GET("price/index-series")
    fun getHistoricPriceSeries(
        @Query("base") base: String,
        @Query("quote") quote: String,
        @Query("start") start: Long,
        @Query("scale") scale: Int
    ): Observable<List<PriceDatum>>

    /**
     * get list price blockchain.info
     */
    @GET("price/indexes")
    fun getPriceIndexes(
        @Query("base") base: String,
        @Query("api_key") apiKey: String
    ): Observable<Map<String, PriceDatum>>

    /**
     * This method can be used to get the data behind Blockchain.info's stats.
     */
    @GET("stats")
    fun getStats(
        @Query("cors") cors: Boolean
    ): Observable<StatsResponse>
}