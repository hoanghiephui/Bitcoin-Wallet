package com.bitcoin.wallet.btc

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.work.WorkManager
import com.bitcoin.wallet.btc.di.components.AppComponent
import com.bitcoin.wallet.btc.service.BlockchainService
import com.bitcoin.wallet.btc.utils.Configuration
import com.bitcoin.wallet.btc.utils.WalletUtils
import com.google.common.base.Splitter
import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.SettableFuture
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.VerificationException
import org.bitcoinj.core.VersionMessage
import org.bitcoinj.crypto.LinuxSecureRandom
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.UnreadableWalletException
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.WalletFiles
import org.bitcoinj.wallet.WalletProtobufSerializer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BitcoinApplication : DaggerApplication() {
    private val appComponent by lazy { AppComponent.getComponent(this) }
    private val activityManager: ActivityManager by lazy {
        getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }
    private var walletFile: File? = null
    private var walletFiles: WalletFiles? = null
    val config: Configuration by lazy {
        Configuration(
            PreferenceManager.getDefaultSharedPreferences(this)
        )
    }
    val packageInfo: PackageInfo by lazy {
        packageManager.getPackageInfo(packageName, 0)
    }
    private val BIP39_WORDLIST_FILENAME = "bip39.txt"

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> = appComponent

    override fun onCreate() {
        super.onCreate()
        initConfigs()
    }

    private fun initConfigs() {
        LinuxSecureRandom()
        Threading.throwOnLockCycles()
        org.bitcoinj.core.Context.enableStrictMode()
        try {
            org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
            /*log.info(//todo log
                "=== starting app using configuration: {}, {}", "prod",
                Constants.NETWORK_PARAMETERS.id
            )*/
        } catch (e: NoClassDefFoundError) {
            e.printStackTrace()
            //log.info("bitcoinj uncaught exception", e) todo log
        }
        Threading.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, throwable ->
            //log.info("bitcoinj uncaught exception", throwable) todo log
        }
        try {
            walletFile = getFileStreamPath(Constants.Files.WALLET_FILENAME_PROTOBUF)
        } catch (ex: Exception) {
            //log.info("bitcoinj uncaught exception wallet file", ex) todo log
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.let { config.updateLastVersionCode(it) }
        } else {
            packageInfo.versionCode.toLong().let { config.updateLastVersionCode(it) }
        }

        cleanupFiles()
        initNotificationManager()
        WorkManager.initialize(
            this, androidx.work.Configuration.Builder()
                .setWorkerFactory(appComponent.daggerWorkerFactory())
                .build()
        )
    }

    private fun cleanupFiles() {
        for (filename in fileList()) {
            if (filename.startsWith(Constants.Files.WALLET_KEY_BACKUP_BASE58)
                || filename.startsWith(Constants.Files.WALLET_KEY_BACKUP_PROTOBUF + '.')
                || filename.endsWith(".tmp")
            ) {
                val file = File(filesDir, filename)
                //log.info("removing obsolete file: '{}'", file) todo log
                file.delete()
            }
        }
    }

    private fun initNotificationManager() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val watch = Stopwatch.createStarted()
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val received = NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID_RECEIVED,
                    getString(R.string.notifi_channel_received), NotificationManager.IMPORTANCE_HIGH
                )
                received.setSound(
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.coins_received),
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build()
                )
                nm.createNotificationChannel(received)

                val ongoing = NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID_ONGOING,
                    getString(R.string.notifi_ongoing_name), NotificationManager.IMPORTANCE_HIGH
                )
                nm.createNotificationChannel(ongoing)

                val important = NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID_IMPORTANT,
                    getString(R.string.notifi_important), NotificationManager.IMPORTANCE_HIGH
                )
                nm.createNotificationChannel(important)

                //log.info("created notification channels, took {}", watch) todo log
            }
        } catch (ex: Exception) {

        }
    }

    fun getWallet(): Wallet {
        val watch = Stopwatch.createStarted()
        val future = SettableFuture.create<Wallet>()
        getWalletAsync(object : OnWalletLoadedListener {
            override fun onWalletLoaded(wallet: Wallet?) {
                future.set(wallet)
            }
        })
        try {
            return future.get()
        } catch (x: InterruptedException) {
            throw RuntimeException(x)
        } catch (x: ExecutionException) {
            throw RuntimeException(x)
        } finally {
            watch.stop()
            //if (Looper.myLooper() == Looper.getMainLooper())
            //log.warn("UI thread blocked for $watch when using getWallet()", RuntimeException()) todo log
        }
    }

    private val getWalletExecutor = Executors.newSingleThreadExecutor()
    private val getWalletLock = Any()


    fun getWalletAsync(listener: OnWalletLoadedListener) {
        getWalletExecutor.execute(object : Runnable {
            override fun run() {
                org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
                synchronized(getWalletLock) {
                    initMnemonicCode()
                    if (walletFiles == null)
                        loadWalletFromProtobuf()
                }
                listener.onWalletLoaded(walletFiles?.wallet)
            }

            private fun loadWalletFromProtobuf() {
                walletFile?.let { file ->
                    var wallet: Wallet? = null
                    if (file.exists()) {
                        try {
                            FileInputStream(walletFile).use { walletStream ->
                                val watch = Stopwatch.createStarted()
                                wallet = WalletProtobufSerializer().readWallet(walletStream)
                                watch.stop()

                                if (wallet?.params != Constants.NETWORK_PARAMETERS)
                                    throw UnreadableWalletException(
                                        "bad wallet network parameters: " + wallet?.params?.id
                                    )

                                //log.info("wallet loaded from: '{}', took {}", walletFile, watch) todo log
                            }
                        } catch (x: IOException) {
                            //log.warn("problem loading wallet, auto-restoring: $walletFile", x) todo log
                            wallet = WalletUtils.restoreWalletFromAutoBackup(this@BitcoinApplication)
                            if (wallet != null)
                                Toast.makeText(
                                    this@BitcoinApplication,
                                    getString(R.string.reset),
                                    Toast.LENGTH_LONG
                                ).show()
                        } catch (x: UnreadableWalletException) {
                            //log.warn("problem loading wallet, auto-restoring: $walletFile", x) todo log
                            wallet = WalletUtils.restoreWalletFromAutoBackup(this@BitcoinApplication)
                            if (wallet != null)
                                Toast.makeText(this@BitcoinApplication, R.string.reset, Toast.LENGTH_LONG).show()
                        }
                        wallet?.let {
                            if (!it.isConsistent) {
                                //log.warn("inconsistent wallet, auto-restoring: $walletFile") todo log
                                wallet = WalletUtils.restoreWalletFromAutoBackup(this@BitcoinApplication)
                                if (wallet != null)
                                    Toast.makeText(this@BitcoinApplication, R.string.reset, Toast.LENGTH_LONG).show()
                            }

                            if (it.params != Constants.NETWORK_PARAMETERS)
                                throw Error("bad wallet network parameters: " + it.params.id)

                            it.cleanup()
                            walletFiles = wallet?.autosaveToFile(
                                walletFile, Constants.Files.WALLET_AUTOSAVE_DELAY_MS,
                                TimeUnit.MILLISECONDS, null
                            )
                        }

                    } else {
                        val watch = Stopwatch.createStarted()
                        wallet = Wallet.createDeterministic(
                            Constants.NETWORK_PARAMETERS,
                            Constants.DEFAULT_OUTPUT_SCRIPT_TYPE
                        )
                        walletFiles = wallet?.autosaveToFile(
                            walletFile, Constants.Files.WALLET_AUTOSAVE_DELAY_MS,
                            TimeUnit.MILLISECONDS, null
                        )
                        autosaveWalletNow() // persist...
                        WalletUtils.autoBackupWallet(this@BitcoinApplication, wallet) // ...and backup asap
                        watch.stop()
                        //log.info("fresh {} wallet created, took {}", Constants.DEFAULT_OUTPUT_SCRIPT_TYPE, watch) todo log

                        config.armBackupReminder()
                    }
                }
            }

            private fun initMnemonicCode() {
                if (MnemonicCode.INSTANCE == null) {
                    try {
                        val watch = Stopwatch.createStarted()
                        MnemonicCode.INSTANCE = MnemonicCode(assets.open(BIP39_WORDLIST_FILENAME), null)
                        watch.stop()
                        //log.info("BIP39 wordlist loaded from: '{}', took {}", BIP39_WORDLIST_FILENAME, watch) todo log
                    } catch (x: IOException) {
                        throw Error(x)
                    }

                }
            }
        })
    }

    interface OnWalletLoadedListener {
        fun onWalletLoaded(wallet: Wallet?)
    }

    fun autosaveWalletNow() {
        val watch = Stopwatch.createStarted()
        synchronized(getWalletLock) {
            if (walletFiles != null) {
                watch.stop()
                //log.info("wallet saved to: '{}', took {}", walletFile, watch) todo log
                try {
                    walletFiles?.saveNow()
                } catch (x: IOException) {
                    //log.warn("problem with forced autosaving of wallet", x) todo log
                }

            }
        }
    }

    fun replaceWallet(newWallet: Wallet) {
        newWallet.cleanup()
        if (newWallet.isDeterministicUpgradeRequired(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE) && !newWallet.isEncrypted)
            newWallet.upgradeToDeterministic(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE, null)
        BlockchainService.resetBlockchain(this)

        val oldWallet = getWallet()
        synchronized(getWalletLock) {
            oldWallet.shutdownAutosaveAndWait() // this will also prevent BlockchainService to save
            walletFiles = newWallet.autosaveToFile(
                walletFile, Constants.Files.WALLET_AUTOSAVE_DELAY_MS,
                TimeUnit.MILLISECONDS, null
            )
        }
        config.maybeIncrementBestChainHeightEver(newWallet.lastBlockSeenHeight)
        WalletUtils.autoBackupWallet(this, newWallet)

        val broadcast = Intent(ACTION_WALLET_REFERENCE_CHANGED)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast)
    }

    fun applicationPackageFlavor(): String? {
        val packageName = packageName
        val index = packageName.lastIndexOf('_')

        return if (index != -1)
            packageName.substring(index + 1)
        else
            null
    }

    fun maxConnectedPeers(): Int {
        return if (activityManager.isLowRamDevice) 4 else 6
    }

    fun scryptIterationsTarget(): Int {
        return if (activityManager.isLowRamDevice)
            Constants.SCRYPT_ITERATIONS_TARGET_LOWRAM
        else
            Constants.SCRYPT_ITERATIONS_TARGET
    }

    fun httpUserAgent(): String {
        return httpUserAgent(packageInfo.versionName ?: BuildConfig.VERSION_NAME)
    }

    @Throws(VerificationException::class)
    fun processDirectTransaction(tx: Transaction) {
        val wallet = getWallet()
        if (wallet.isTransactionRelevant(tx)) {
            wallet.receivePending(tx, null)
            BlockchainService.broadcastTransaction(this, tx)
        }
    }

    companion object {
        const val ACTION_WALLET_REFERENCE_CHANGED = "com.bitcoin.wallet.wallet_reference_changed"
        fun httpUserAgent(versionName: String): String {
            return try {
                val versionMessage = VersionMessage(Constants.NETWORK_PARAMETERS, 0)
                versionMessage.appendToSubVer(Constants.USER_AGENT, versionName, null)
                versionMessage.subVer
            } catch (e: Exception) {
                e.printStackTrace()
                "/bitcoinj:0.15.1/Bitcoin Wallet:1.0.0/"
            } catch (e: ExceptionInInitializerError) {
                "/bitcoinj:0.15.1/Bitcoin Wallet:1.0.0/"
            } catch (e: IllegalArgumentException) {
                "/bitcoinj:0.15.1/Bitcoin Wallet:1.0.0/"
            }
        }

        fun versionLine(packageInfo: PackageInfo): String {
            return (ImmutableList.copyOf(Splitter.on('.').splitToList(packageInfo.packageName)).reverse()[0] + ' '.toString()
                    + packageInfo.versionName + if (BuildConfig.DEBUG) " (debuggable)" else "")
        }
    }
}