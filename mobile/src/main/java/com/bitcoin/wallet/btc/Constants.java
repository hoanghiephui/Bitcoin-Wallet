package com.bitcoin.wallet.btc;

import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import com.google.common.io.BaseEncoding;
import okhttp3.OkHttpClient;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.MonetaryFormat;

import java.util.concurrent.TimeUnit;

public class Constants {
    public static final boolean TEST = false;
    public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();
    public static final Context CONTEXT = new Context(NETWORK_PARAMETERS);
    public static final Script.ScriptType DEFAULT_OUTPUT_SCRIPT_TYPE = Script.ScriptType.P2WPKH;
    public static final Script.ScriptType UPGRADE_OUTPUT_SCRIPT_TYPE = Script.ScriptType.P2WPKH;
    public static final String USER_AGENT = "Bitcoin Wallet";

    public static final char CHAR_HAIR_SPACE = '\u200a';
    public static final char CHAR_THIN_SPACE = '\u2009';
    public static final char CHAR_BITCOIN = '\u20bf';
    public static final char CHAR_ALMOST_EQUAL_TO = '\u2248';
    public static final char CHAR_CHECKMARK = '\u2713';
    public static final char CURRENCY_PLUS_SIGN = '\uff0b';
    public static final char CURRENCY_MINUS_SIGN = '\uff0d';
    public static final int ADDRESS_FORMAT_GROUP_SIZE = 4;
    public static final int ADDRESS_FORMAT_LINE_SIZE = 12;
    public static final long LAST_USAGE_THRESHOLD_JUST_MS = DateUtils.HOUR_IN_MILLIS;
    public static final long LAST_USAGE_THRESHOLD_RECENTLY_MS = 2 * DateUtils.DAY_IN_MILLIS;
    public static final int NOTIFICATION_ID_CONNECTED = 1;
    public static final int NOTIFICATION_ID_COINS_RECEIVED = 2;
    public static final int NOTIFICATION_ID_MAINTENANCE = 3;
    public static final int NOTIFICATION_ID_INACTIVITY = 4;
    public static final String NOTIFICATION_GROUP_KEY_RECEIVED = "group-received";
    public static final String NOTIFICATION_CHANNEL_ID_RECEIVED = "received";
    public static final String NOTIFICATION_CHANNEL_ID_ONGOING = "ongoing";
    public static final String NOTIFICATION_CHANNEL_ID_IMPORTANT = "important";
    public static final int SCRYPT_ITERATIONS_TARGET = 65536;
    public static final int SCRYPT_ITERATIONS_TARGET_LOWRAM = 32768;
    public static final OkHttpClient HTTP_CLIENT;
    public static final String URL_BLOCKCHAIN = "https://api.blockchain.info/";
    public static final String URL_COINBASE = "https://api.coinbase.com/";
    public static final String URL_BITCOIN = "https://explorer.bitcoin.com/api/btc/";
    public static final String DEFAULT_EXCHANGE_CURRENCY = "USD";
    public static final int PEER_DISCOVERY_TIMEOUT_MS = 10 * (int) DateUtils.SECOND_IN_MILLIS;
    public static final int PEER_TIMEOUT_MS = 15 * (int) DateUtils.SECOND_IN_MILLIS;
    public static final MonetaryFormat LOCAL_FORMAT = new MonetaryFormat().noCode().minDecimals(2).optionalDecimals();
    public static final String PREFIX_ALMOST_EQUAL_TO = Character.toString(CHAR_ALMOST_EQUAL_TO) + CHAR_THIN_SPACE;
    public static final Coin TOO_MUCH_BALANCE_THRESHOLD = Coin.COIN.divide(4);
    public static final int MAX_NUM_CONFIRMATIONS = 7;
    public static final long DELAYED_TRANSACTION_THRESHOLD_MS = 2 * DateUtils.HOUR_IN_MILLIS;
    public static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();
    public static final String MIMETYPE_TRANSACTION = "application/x-btctx";
    public static final long LAST_USAGE_THRESHOLD_INACTIVE_MS = 4 * DateUtils.WEEK_IN_MILLIS;
    public static final String MIMETYPE_WALLET_BACKUP = "application/x-bitcoin-wallet-backup";
    public static final int ELECTRUM_SERVER_DEFAULT_PORT_TCP = NETWORK_PARAMETERS.getId()
            .equals(NetworkParameters.ID_MAINNET) ? 50001 : 51001;
    public static final int ELECTRUM_SERVER_DEFAULT_PORT_TLS = NETWORK_PARAMETERS.getId()
            .equals(NetworkParameters.ID_MAINNET) ? 50002 : 51002;
    public static final int PAGE_SIZE = 20;
    public static final String API_KEY = "089cf013-64e7-44c2-bc17-7b9746da0291";

    static {
        final OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.followRedirects(false);
        httpClientBuilder.followSslRedirects(true);
        httpClientBuilder.retryOnConnectionFailure(true);
        httpClientBuilder.connectTimeout(15, TimeUnit.SECONDS);
        httpClientBuilder.writeTimeout(15, TimeUnit.SECONDS);
        httpClientBuilder.readTimeout(15, TimeUnit.SECONDS);
        HTTP_CLIENT = httpClientBuilder.build();
    }

    public static void setMargin(View v, int margin) {
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams)v.getLayoutParams();
        params.setMargins(margin, margin,
                margin, margin);
    }

    public static void setMarginStartEnd(View v, int margin) {
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams)v.getLayoutParams();
        params.setMargins(margin, params.topMargin,
                margin, params.bottomMargin);
    }
}
