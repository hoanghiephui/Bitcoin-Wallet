package com.bitcoin.wallet.mobile.di.modules

import com.bitcoin.wallet.mobile.BuildConfig
import com.bitcoin.wallet.mobile.Constants.URL_BLOCKCHAIN
import com.bitcoin.wallet.mobile.api.BlockchainEndpoint
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import dagger.Module
import dagger.Provides
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.internal.platform.Platform
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(URL_BLOCKCHAIN)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    fun provideBlockchainEndpoint(retrofit: Retrofit): BlockchainEndpoint =
        retrofit.create(BlockchainEndpoint::class.java)

    companion object {
        private val API_TIMEOUT = 30
        private val PING_INTERVAL = 10
    }
}

private class ResponseConverter(
    private val gson: Gson,
    private val creator: GsonConverterFactory = GsonConverterFactory.create(gson)
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return try {
            if (type === String::class.java) {
                StringResponseConverter()
            } else {
                creator.responseBodyConverter(type, annotations, retrofit)
            }
        } catch (ignored: OutOfMemoryError) {
            null
        }

    }

    override fun requestBodyConverter(
        type: Type, parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>, retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return creator.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }

    private class StringResponseConverter : Converter<ResponseBody, String> {
        @Throws(IOException::class)
        override fun convert(value: ResponseBody): String {
            return value.string()
        }
    }
}