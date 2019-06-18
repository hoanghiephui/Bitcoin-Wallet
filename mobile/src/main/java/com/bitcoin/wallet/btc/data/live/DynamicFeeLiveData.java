package com.bitcoin.wallet.btc.data.live;

import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;
import com.bitcoin.wallet.btc.BitcoinApplication;
import com.bitcoin.wallet.btc.Constants;
import com.bitcoin.wallet.btc.FilesWallet;
import com.bitcoin.wallet.btc.data.FeeCategory;
import com.google.common.base.Stopwatch;
import com.google.common.io.ByteStreams;
import okhttp3.*;
import okhttp3.internal.http.HttpDate;
import org.bitcoinj.core.Coin;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DynamicFeeLiveData extends LiveData<Map<FeeCategory, Coin>> {
    private final HttpUrl dynamicFeesUrl;
    private final String userAgent;
    private final AssetManager assets;
    private final File dynamicFeesFile;
    private final File tempFile;

    //private static final Logger log = LoggerFactory.getLogger(DynamicFeeLiveData.class);todo log

    public DynamicFeeLiveData(final BitcoinApplication application) {
        final PackageInfo packageInfo = application.getPackageInfo();
        final int versionNameSplit = packageInfo.versionName.indexOf('-');
        this.dynamicFeesUrl = HttpUrl.parse(HttpUrl.parse("https://wallet.schildbach.de/fees")
                + (versionNameSplit >= 0 ? packageInfo.versionName.substring(versionNameSplit) : ""));
        this.userAgent = BitcoinApplication.Companion.httpUserAgent(packageInfo.versionName);
        this.assets = application.getAssets();
        this.dynamicFeesFile = new File(application.getFilesDir(), FilesWallet.FEES_FILENAME);
        this.tempFile = new File(application.getCacheDir(), FilesWallet.FEES_FILENAME + ".temp");
    }

    @Override
    protected void onActive() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final Map<FeeCategory, Coin> dynamicFees = loadInBackground();
                postValue(dynamicFees);
            }
        });
    }

    private static void fetchDynamicFees(final HttpUrl url, final File tempFile, final File targetFile,
                                         final String userAgent) {
        final Stopwatch watch = Stopwatch.createStarted();

        final Request.Builder request = new Request.Builder();
        request.url(url);
        request.header("User-Agent", userAgent);
        if (targetFile.exists())
            request.header("If-Modified-Since", HttpDate.format(new Date(targetFile.lastModified())));

        final OkHttpClient.Builder httpClientBuilder = Constants.HTTP_CLIENT.newBuilder();
        httpClientBuilder.connectionSpecs(Collections.singletonList(ConnectionSpec.RESTRICTED_TLS));
        httpClientBuilder.connectTimeout(5, TimeUnit.SECONDS);
        httpClientBuilder.writeTimeout(5, TimeUnit.SECONDS);
        httpClientBuilder.readTimeout(5, TimeUnit.SECONDS);
        final OkHttpClient httpClient = httpClientBuilder.build();
        final Call call = httpClient.newCall(request.build());
        try {
            final Response response = call.execute();
            final int status = response.code();
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
                //log.info("Dynamic fees not modified at {}, took {}", url, watch);
            } else if (status == HttpURLConnection.HTTP_OK) {
                final ResponseBody body = response.body();
                final FileOutputStream os = new FileOutputStream(tempFile);
                ByteStreams.copy(body.byteStream(), os);
                os.close();
                final Date lastModified = response.headers().getDate("Last-Modified");
                if (lastModified != null)
                    tempFile.setLastModified(lastModified.getTime());
                body.close();
                if (!tempFile.renameTo(targetFile))
                    throw new IllegalStateException("Cannot rename " + tempFile + " to " + targetFile);
                watch.stop();
                //log.info("Dynamic fees fetched from {}, took {}", url, watch);
            } else {
                //log.warn("HTTP status {} when fetching dynamic fees from {}", response.code(), url);
            }
        } catch (final Exception x) {
            //log.warn("Problem when fetching dynamic fees rates from " + url, x);
        }
    }

    private static Map<FeeCategory, Coin> parseFees(final InputStream is) throws IOException {
        final Map<FeeCategory, Coin> dynamicFees = new HashMap<FeeCategory, Coin>();
        String line = null;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
            while (true) {
                line = reader.readLine();
                if (line == null)
                    break;
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;

                final String[] fields = line.split("=");
                try {
                    final FeeCategory category = FeeCategory.valueOf(fields[0]);
                    final Coin rate = Coin.valueOf(Long.parseLong(fields[1]));
                    dynamicFees.put(category, rate);
                } catch (IllegalArgumentException x) {
                    //log.warn("Cannot parse line, ignoring: '" + line + "'", x);
                }
            }
        } catch (final Exception x) {
            throw new RuntimeException("Error while parsing: '" + line + "'", x);
        } finally {
            is.close();
        }
        return dynamicFees;
    }

    private Map<FeeCategory, Coin> loadInBackground() {
        try {
            final Map<FeeCategory, Coin> staticFees = parseFees(assets.open(FilesWallet.FEES_FILENAME));
            fetchDynamicFees(dynamicFeesUrl, tempFile, dynamicFeesFile, userAgent);
            if (!dynamicFeesFile.exists())
                return staticFees;

            // Check dynamic fees for sanity, based on the hardcoded fees.
            // The bounds are as follows (h is the respective hardcoded fee):
            // ECONOMIC: h/8 to h*4
            // NORMAL: h/4 to h*4
            // PRIORITY: h/4 to h*8
            final Map<FeeCategory, Coin> dynamicFees = parseFees(new FileInputStream(dynamicFeesFile));
            for (final FeeCategory category : FeeCategory.values()) {
                final Coin staticFee = staticFees.get(category);
                final Coin dynamicFee = dynamicFees.get(category);
                if (dynamicFee == null) {
                    dynamicFees.put(category, staticFee);
                    /*log.warn("Dynamic fee category missing, using static: category {}, {}/kB", category,
                            staticFee.toFriendlyString());*/
                    continue;
                }
                final Coin upperBound = staticFee.shiftLeft(category == FeeCategory.PRIORITY ? 3 : 2);
                if (dynamicFee.isGreaterThan(upperBound)) {
                    dynamicFees.put(category, upperBound);
                    /*log.warn("Down-adjusting dynamic fee: category {} from {}/kB to {}/kB", category,
                            dynamicFee.toFriendlyString(), upperBound.toFriendlyString());*/
                    continue;
                }
                final Coin lowerBound = staticFee.shiftRight(category == FeeCategory.ECONOMIC ? 3 : 2);
                if (dynamicFee.isLessThan(lowerBound)) {
                    dynamicFees.put(category, lowerBound);
                    /*log.warn("Up-adjusting dynamic fee: category {} from {}/kB to {}/kB", category,
                            dynamicFee.toFriendlyString(), lowerBound.toFriendlyString());*/
                }
            }
            return dynamicFees;
        } catch (final IOException x) {
            // Should not happen
            throw new RuntimeException(x);
        }
    }
}
