package com.bitcoin.wallet.btc.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import org.bitcoinj.wallet.Wallet

class UpgradeWalletService : IntentService(UpgradeWalletService::class.java.name) {

    private var application: BitcoinApplication? = null

    init {
        setIntentRedelivery(true)
    }

    override fun onCreate() {
        super.onCreate()
        application = getApplication() as BitcoinApplication

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(
                this,
                Constants.NOTIFICATION_CHANNEL_ID_ONGOING
            ).apply {
                setSmallIcon(R.drawable.ic_notify)
                setContentText("Bitcoin Wallet is Synchronized...")
                setWhen(System.currentTimeMillis())
                setOngoing(true)
                startForeground(Constants.NOTIFICATION_ID_MAINTENANCE, this.build())
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
        val wallet = application?.getWallet()
        if (wallet?.isDeterministicUpgradeRequired(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE) == true && !wallet.isEncrypted) {
            // upgrade wallet to a specific deterministic chain
            wallet.upgradeToDeterministic(Constants.UPGRADE_OUTPUT_SCRIPT_TYPE, null)
            // let other service pre-generate look-ahead keys
            BlockchainService.start(this, false)
        }
        maybeUpgradeToSecureChain(wallet)
    }

    private fun maybeUpgradeToSecureChain(wallet: Wallet?) {
        try {
            wallet?.doMaintenance(null, false)
        } catch (x: Exception) {
        }
        BlockchainService.start(this, false)
    }

    companion object {
        fun startUpgrade(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, UpgradeWalletService::class.java)
            )
        }
    }
}