package com.bitcoin.wallet.btc.api

import com.bitcoin.wallet.btc.model.PriceDatum
import com.bitcoin.wallet.btc.model.info.InfoResponse
import com.bitcoin.wallet.btc.model.news.NewsResponse
import com.bitcoin.wallet.btc.model.price.PriceResponse
import com.bitcoin.wallet.btc.model.price_new.PriceNewResponse
import com.bitcoin.wallet.btc.model.stats.StatsResponse
import com.bitcoin.wallet.btc.model.summary.SummaryResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface CoinbaseEndpoint {
    /**
     * get stats coin base
     * https://api.coinbase.com/v2/assets/stats/5b71fc48-3dd3-540c-809b-f8c94d0e68b5?base=USD
     */
    @GET("v2/assets/stats/{baseId}")
    fun getStatsCoinbase(
        @Path("baseId") baseId: String,
        @Query("base") base: String
    ): Observable<StatsResponse>

    /**
     * get price coin base
     * https://api.coinbase.com/v2/assets/prices/5b71fc48-3dd3-540c-809b-f8c94d0e68b5?base=VND&resolution=hour
     */
    @GET("v2/assets/prices/{baseId}")
    fun getListPriceForChart(
        @Path("baseId") baseId: String,
        @Query("base") base: String,
        @Query("resolution") resolution: String
    ): Observable<PriceResponse>

    /**
     * get price coin base chart
     * https://api.coinbase.com/v2/prices/btc-usd/historic?period=day
     */
    @GET("v2/prices/{base_currency}-{fiat_currency}/historic")
    fun getListPriceForChartNew(
        @Path("base_currency") baseCurrency: String,
        @Path("fiat_currency") fiatCurrency: String,
        @Query("period") period: String
    ): Observable<PriceNewResponse>

    /**
     * get info coin
     * https://www.coinbase.com/api/v2/assets/info/5b71fc48-3dd3-540c-809b-f8c94d0e68b5
     */
    @GET
    fun getInfoCoinbase(
        @Url url: String
    ): Observable<InfoResponse>

    /**
     * get list news
     * https://www.coinbase.com/api/v2/news-articles?asset_id=5b71fc48-3dd3-540c-809b-f8c94d0e68b5&limit=6
     */
    @GET
    fun getListNews(
        @Url url: String
    ): Observable<NewsResponse>

    /**
     * get list summary coin
     * https://www.coinbase.com/api/v2/assets/summary?base=USD&resolution=day&limit=10
     */
    @GET
    fun getListSummaryCoin(
        @Url url: String
    ): Observable<SummaryResponse>
}

data class ZipHomeData(
    var priceResponse: List<PriceDatum>,
    var infoResponse: InfoResponse,
    var newsResponse: NewsResponse,
    var summaryResponse: SummaryResponse,
    var statsResponse: StatsResponse
)