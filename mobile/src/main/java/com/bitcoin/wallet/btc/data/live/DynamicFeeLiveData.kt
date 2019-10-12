package com.bitcoin.wallet.btc.data.live

import android.content.res.AssetManager
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.FilesWallet
import com.bitcoin.wallet.btc.data.FeeCategory
import com.google.common.base.Stopwatch
import com.google.common.io.ByteStreams
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.internal.http.toHttpDateString
import org.bitcoinj.core.Coin
import java.io.*
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

class DynamicFeeLiveData(application: BitcoinApplication) : LiveData<Map<FeeCategory, Coin>>() {
    private val dynamicFeesUrl: HttpUrl?
    private val userAgent: String
    private val assets: AssetManager
    private val dynamicFeesFile: File
    private val tempFile: File

    init {
        val packageInfo = application.packageInfo
        val versionNameSplit = packageInfo.versionName.indexOf('-')
        this.dynamicFeesUrl =
            ("https://wallet.schildbach.de/fees".toHttpUrlOrNull().toString() + if (versionNameSplit >= 0) packageInfo.versionName.substring(
                versionNameSplit
            ) else ""
                    ).toHttpUrlOrNull()
        this.userAgent = BitcoinApplication.httpUserAgent(packageInfo.versionName)
        this.assets = application.assets
        this.dynamicFeesFile = File(application.filesDir, FilesWallet.FEES_FILENAME)
        this.tempFile = File(application.cacheDir, FilesWallet.FEES_FILENAME + ".temp")
    }

    override fun onActive() {
        AsyncTask.execute {
            val dynamicFees = loadInBackground()
            postValue(dynamicFees)
        }
    }

    private fun fetchDynamicFees(
        url: HttpUrl?, tempFile: File, targetFile: File,
        userAgent: String
    ) {
        val watch = Stopwatch.createStarted()

        val request = Request.Builder()
        request.url(url!!)
        request.header("User-Agent", userAgent)
        if (targetFile.exists())
            Date(targetFile.lastModified()).toHttpDateString()?.let {
                request.header(
                    "If-Modified-Since",
                    it
                )
            }

        val httpClientBuilder = Constants.HTTP_CLIENT.newBuilder()
        httpClientBuilder.connectionSpecs(listOf(ConnectionSpec.RESTRICTED_TLS))
        httpClientBuilder.connectTimeout(5, TimeUnit.SECONDS)
        httpClientBuilder.writeTimeout(5, TimeUnit.SECONDS)
        httpClientBuilder.readTimeout(5, TimeUnit.SECONDS)
        val httpClient = httpClientBuilder.build()
        val call = httpClient.newCall(request.build())
        try {
            val response = call.execute()
            val status = response.code
            if (status == HttpURLConnection.HTTP_OK) {
                val body = response.body
                val os = FileOutputStream(tempFile)
                ByteStreams.copy(body!!.byteStream(), os)
                os.close()
                val lastModified = response.headers.getDate("Last-Modified")
                if (lastModified != null)
                    tempFile.setLastModified(lastModified.time)
                body.close()
                if (!tempFile.renameTo(targetFile))
                    throw IllegalStateException("Cannot rename $tempFile to $targetFile")
                watch.stop()
            }
        } catch (x: Exception) {
            //log.warn("Problem when fetching dynamic fees rates from " + url, x);
        }

    }

    @Throws(IOException::class)
    private fun parseFees(`is`: InputStream): MutableMap<FeeCategory, Coin> {
        val dynamicFees = HashMap<FeeCategory, Coin>()
        var line: String? = null
        try {
            BufferedReader(InputStreamReader(`is`, StandardCharsets.US_ASCII)).use { reader ->
                while (true) {
                    line = reader.readLine()
                    if (line == null)
                        break
                    line = line!!.trim { it <= ' ' }
                    if (line!!.length == 0 || line!![0] == '#')
                        continue

                    val fields =
                        line!!.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    try {
                        val category = FeeCategory.valueOf(fields[0])
                        val rate = Coin.valueOf(java.lang.Long.parseLong(fields[1]))
                        dynamicFees[category] = rate
                    } catch (x: IllegalArgumentException) {
                        //log.warn("Cannot parse line, ignoring: '" + line + "'", x);
                    }

                }
            }
        } catch (x: Exception) {
            throw RuntimeException("Error while parsing: '$line'", x)
        } finally {
            `is`.close()
        }
        return dynamicFees
    }

    private fun loadInBackground(): Map<FeeCategory, Coin> {
        try {
            val staticFees = parseFees(assets.open(FilesWallet.FEES_FILENAME))
            fetchDynamicFees(dynamicFeesUrl, tempFile, dynamicFeesFile, userAgent)
            if (!dynamicFeesFile.exists())
                return staticFees

            // Check dynamic fees for sanity, based on the hardcoded fees.
            // The bounds are as follows (h is the respective hardcoded fee):
            // ECONOMIC: h/8 to h*4
            // NORMAL: h/4 to h*4
            // PRIORITY: h/4 to h*8
            val dynamicFees = parseFees(FileInputStream(dynamicFeesFile))
            for (category in FeeCategory.values()) {
                val staticFee = staticFees[category] ?: break
                val dynamicFee = dynamicFees[category]
                if (dynamicFee == null) {
                    dynamicFees[category] = staticFee
                    /*log.warn("Dynamic fee category missing, using static: category {}, {}/kB", category,
                            staticFee.toFriendlyString());*/
                    continue
                }
                val upperBound =
                    staticFee.shiftLeft(if (category === FeeCategory.PRIORITY) 3 else 2)
                if (dynamicFee.isGreaterThan(upperBound)) {
                    dynamicFees[category] = upperBound
                    /*log.warn("Down-adjusting dynamic fee: category {} from {}/kB to {}/kB", category,
                            dynamicFee.toFriendlyString(), upperBound.toFriendlyString());*/
                    continue
                }
                val lowerBound =
                    staticFee.shiftRight(if (category === FeeCategory.ECONOMIC) 3 else 2)
                if (dynamicFee.isLessThan(lowerBound)) {
                    dynamicFees[category] = lowerBound
                    /*log.warn("Up-adjusting dynamic fee: category {} from {}/kB to {}/kB", category,
                            dynamicFee.toFriendlyString(), lowerBound.toFriendlyString());*/
                }
            }
            return dynamicFees
        } catch (x: IOException) {
            // Should not happen
            throw RuntimeException(x)
        }

    }
}
