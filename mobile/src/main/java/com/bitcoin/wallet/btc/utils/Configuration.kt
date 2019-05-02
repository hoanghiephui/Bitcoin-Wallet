package com.bitcoin.wallet.btc.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import com.bitcoin.wallet.btc.data.ExchangeRate
import com.google.common.base.Strings
import org.bitcoinj.core.Coin
import org.bitcoinj.utils.Fiat
import org.bitcoinj.utils.MonetaryFormat

class Configuration constructor(private val prefs: SharedPreferences) {
    private val lastVersionCode: Long

    private val btcPrecision: Int
        get() {
            val precision = prefs.getString(PREFS_KEY_BTC_PRECISION, null)
            return if (precision != null)
                precision[0] - '0'
            else
                PREFS_DEFAULT_BTC_PRECISION
        }

    val btcShift: Int
        get() {
            val precision = prefs.getString(PREFS_KEY_BTC_PRECISION, null)
            return if (precision != null)
                if (precision.length == 3) precision[2] - '0' else 0
            else
                PREFS_DEFAULT_BTC_SHIFT
        }

    val btcBase: Coin
        get() {
            val shift = btcShift
            return if (shift == 0)
                Coin.COIN
            else if (shift == 3)
                Coin.MILLICOIN
            else if (shift == 6)
                Coin.MICROCOIN
            else
                throw IllegalStateException("cannot handle shift: $shift")
        }

    val format: MonetaryFormat
        get() {
            val shift = btcShift
            val minPrecision = if (shift <= 3) 2 else 0
            val decimalRepetitions = (btcPrecision - minPrecision) / 2
            return MonetaryFormat().shift(shift).minDecimals(minPrecision).repeatOptionalDecimals(
                2,
                decimalRepetitions
            )
        }

    val maxPrecisionFormat: MonetaryFormat
        get() {
            val shift = btcShift
            return if (shift == 0)
                MonetaryFormat().shift(0).minDecimals(2).optionalDecimals(2, 2, 2)
            else if (shift == 3)
                MonetaryFormat().shift(3).minDecimals(2).optionalDecimals(2, 1)
            else
                MonetaryFormat().shift(6).minDecimals(0).optionalDecimals(2)
        }

    val ownName: String?
        get() = Strings.emptyToNull(prefs.getString(PREFS_KEY_OWN_NAME, "")!!.trim { it <= ' ' })

    val sendCoinsAutoclose: Boolean
        get() = prefs.getBoolean(PREFS_KEY_SEND_COINS_AUTOCLOSE, true)

    val nofity: Boolean
        get() = prefs.getBoolean(PREFS_KEY_NOTIFY, true)

    val connectivityNotificationEnabled: Boolean
        get() = prefs.getBoolean(PREFS_KEY_CONNECTIVITY_NOTIFICATION, false)

    val trustedPeerHost: String?
        get() = Strings.emptyToNull(prefs.getString(PREFS_KEY_TRUSTED_PEER, "")!!.trim { it <= ' ' })

    val trustedPeerOnly: Boolean
        get() = prefs.getBoolean(PREFS_KEY_TRUSTED_PEER_ONLY, false)

    val disclaimerEnabled: Boolean
        get() = prefs.getBoolean(PREFS_KEY_DISCLAIMER, true)

    var exchangeCurrencyCode: String
        get() = prefs.getString(PREFS_KEY_EXCHANGE_CURRENCY, "") ?: "usd"
        set(exchangeCurrencyCode) = prefs.edit().putString(PREFS_KEY_EXCHANGE_CURRENCY, exchangeCurrencyCode).apply()

    val lastUsedAgo: Long
        get() {
            val now = System.currentTimeMillis()

            return now - prefs.getLong(PREFS_KEY_LAST_USED, 0)
        }

    val bestChainHeightEver: Int
        get() = prefs.getInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, 0)

    var cachedExchangeRate: ExchangeRate?
        get() {
            if (prefs.contains(PREFS_KEY_CACHED_EXCHANGE_CURRENCY) && prefs.contains(PREFS_KEY_CACHED_EXCHANGE_RATE_COIN)
                && prefs.contains(PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT)
            ) {
                val cachedExchangeCurrency = prefs.getString(PREFS_KEY_CACHED_EXCHANGE_CURRENCY, null)
                val cachedExchangeRateCoin = Coin.valueOf(prefs.getLong(PREFS_KEY_CACHED_EXCHANGE_RATE_COIN, 0))
                val cachedExchangeRateFiat = Fiat.valueOf(
                    cachedExchangeCurrency,
                    prefs.getLong(PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT, 0)
                )
                return ExchangeRate(
                    org.bitcoinj.utils.ExchangeRate(cachedExchangeRateCoin, cachedExchangeRateFiat),
                    cachedExchangeCurrency ?: "USD"
                )
            } else {
                return null
            }
        }
        set(cachedExchangeRate) {
            val edit = prefs.edit()
            edit.putString(PREFS_KEY_CACHED_EXCHANGE_CURRENCY, cachedExchangeRate?.currencyCode)
            edit.putLong(PREFS_KEY_CACHED_EXCHANGE_RATE_COIN, cachedExchangeRate?.rate?.coin?.value ?: 0)
            edit.putLong(PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT, cachedExchangeRate?.rate?.fiat?.value ?: 0)
            edit.apply()
        }

    var lastExchangeDirection: Boolean
        get() = prefs.getBoolean(PREFS_KEY_LAST_EXCHANGE_DIRECTION, true)
        set(exchangeDirection) = prefs.edit().putBoolean(PREFS_KEY_LAST_EXCHANGE_DIRECTION, exchangeDirection).apply()

    init {

        this.lastVersionCode = prefs.getLong(PREFS_KEY_LAST_VERSION, 0)
    }

    fun remindBalance(): Boolean {
        return prefs.getBoolean(PREFS_KEY_REMIND_BALANCE, true)
    }

    fun setRemindBalance(remindBalance: Boolean) {
        prefs.edit().putBoolean(PREFS_KEY_REMIND_BALANCE, remindBalance).apply()
    }

    fun remindBackup(): Boolean {
        return prefs.getBoolean(PREFS_KEY_REMIND_BACKUP, true)
    }

    fun armBackupReminder() {
        prefs.edit().putBoolean(PREFS_KEY_REMIND_BACKUP, true).apply()
    }

    fun disarmBackupReminder() {
        prefs.edit {
            putBoolean(PREFS_KEY_REMIND_BACKUP, false)
            putLong(PREFS_KEY_LAST_BACKUP, System.currentTimeMillis())
        }
    }

    fun updateLastVersionCode(currentVersionCode: Long) {
        prefs.edit().putLong(PREFS_KEY_LAST_VERSION, currentVersionCode).apply()
    }

    fun hasBeenUsed(): Boolean {
        return prefs.contains(PREFS_KEY_LAST_USED)
    }

    fun touchLastUsed() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(PREFS_KEY_LAST_USED, now).apply()
    }

    fun maybeIncrementBestChainHeightEver(bestChainHeightEver: Int) {
        if (bestChainHeightEver > onGetBestChainHeightEver())
            prefs.edit().putInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, bestChainHeightEver).apply()
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun onGetBestChainHeightEver(): Int {
        return prefs.getInt(PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, 0)
    }

    companion object {
        const val PREFS_KEY_BTC_PRECISION = "btc_precision"
        const val PREFS_KEY_OWN_NAME = "own_name"
        const val PREFS_KEY_SEND_COINS_AUTOCLOSE = "send_coins_autoclose"
        const val PREFS_KEY_CONNECTIVITY_NOTIFICATION = "connectivity_notification"
        const val PREFS_KEY_EXCHANGE_CURRENCY = "exchange_currency"
        const val PREFS_KEY_TRUSTED_PEER = "trusted_peer"
        const val PREFS_KEY_TRUSTED_PEER_ONLY = "trusted_peer_only"
        const val PREFS_KEY_REMIND_BALANCE = "remind_balance"
        const val PREFS_KEY_DISCLAIMER = "disclaimer"
        const val PREFS_KEY_NOTIFY = "notify_coin"
        const val PREFS_KEY_REMIND_BACKUP = "remind_backup"
        private const val PREFS_KEY_LAST_VERSION = "last_version"
        private const val PREFS_KEY_LAST_USED = "last_used"
        private const val PREFS_KEY_BEST_CHAIN_HEIGHT_EVER = "best_chain_height_ever"
        private const val PREFS_KEY_CACHED_EXCHANGE_CURRENCY = "cached_exchange_currency"
        private const val PREFS_KEY_CACHED_EXCHANGE_RATE_COIN = "cached_exchange_rate_coin"
        private const val PREFS_KEY_CACHED_EXCHANGE_RATE_FIAT = "cached_exchange_rate_fiat"
        private const val PREFS_KEY_LAST_EXCHANGE_DIRECTION = "last_exchange_direction"
        private const val PREFS_KEY_LAST_BACKUP = "last_backup"
        private const val PREFS_DEFAULT_BTC_SHIFT = 3
        private const val PREFS_DEFAULT_BTC_PRECISION = 2
    }
}
