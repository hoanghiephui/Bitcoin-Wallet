package com.bitcoin.wallet.mobile.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.Constants

class BitcoinReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as BitcoinApplication

        val bootCompleted = Intent.ACTION_BOOT_COMPLETED == intent.action
        val packageReplaced = Intent.ACTION_MY_PACKAGE_REPLACED == intent.action

        if (packageReplaced || bootCompleted) {
            // make sure wallet is upgraded to HD
            if (packageReplaced)
                UpgradeWalletService.startUpgrade(context)

            // make sure there is always an alarm scheduled
            BlockchainService.scheduleStart(application)

            // if the app hasn't been used for a while and contains coins, maybe show reminder
            val config = application.config
            if (config.remindBalance() && config.hasBeenUsed()
                && config.lastUsedAgo > Constants.LAST_USAGE_THRESHOLD_INACTIVE_MS
            )
                NotificationService.startMaybeShowNotification(context)
        }
    }
}