package com.bitcoin.wallet.btc.di.modules

import com.bitcoin.wallet.btc.BuildConfig
import com.bitcoin.wallet.btc.Constants.*
import com.bitcoin.wallet.btc.api.BitcoinEndpoints
import com.bitcoin.wallet.btc.api.BlockchainEndpoint
import com.bitcoin.wallet.btc.api.CoinbaseEndpoint
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import dagger.Module
import dagger.Provides
import okhttp3.*
import okhttp3.internal.platform.Platform
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
class NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()
        val builder = OkHttpClient.Builder()
            .connectionSpecs(listOf(spec))
            .connectTimeout(API_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(API_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(API_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .pingInterval(PING_INTERVAL.toLong(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // Add logging for debugging purposes
            .addInterceptor(
                LoggingInterceptor.Builder()
                    .loggable(BuildConfig.DEBUG)
                    .tag("LoggingI")
                    .setLevel(Level.BASIC)
                    .log(Platform.INFO)
                    .request("Request")
                    .response("Response").build()
            )
        return builder.build()
    }

    @Singleton
    @Provides
    fun provideGson(): Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    @Singleton
    @Provides
    @Named("blockchain")
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(URL_BLOCKCHAIN)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    @Named("coinbase")
    fun provideRetrofitCoinbase(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(URL_COINBASE)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    @Named("bitcoin")
    fun provideRetrofitBitcoin(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(URL_BITCOIN)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    fun provideBlockchainEndpoint(@Named("blockchain") retrofit: Retrofit): BlockchainEndpoint =
        retrofit.create(BlockchainEndpoint::class.java)

    @Singleton
    @Provides
    fun provideCoinbaseEndpoint(@Named("coinbase") retrofit: Retrofit): CoinbaseEndpoint =
        retrofit.create(CoinbaseEndpoint::class.java)

    @Singleton
    @Provides
    fun provideBitcoinEndpoints(@Named("bitcoin") retrofit: Retrofit): BitcoinEndpoints =
        retrofit.create(BitcoinEndpoints::class.java)

    companion object {
        private const val API_TIMEOUT = 30
        private const val PING_INTERVAL = 10
    }
}