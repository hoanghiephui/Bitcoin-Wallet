package com.bitcoin.wallet.btc.service

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.ui.activitys.MainActivity
import com.bitcoin.wallet.btc.utils.Configuration
import org.bitcoinj.wallet.Wallet

class NotificationService : IntentService(NotificationService::class.java.name) {

    private var nm: NotificationManager? = null
    private var application: BitcoinApplication? = null
    private var config: Configuration? = null

    init {
        setIntentRedelivery(true)
    }

    override fun onCreate() {
        super.onCreate()

        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        application = getApplication() as BitcoinApplication
        config = application?.config

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(
                this,
                Constants.NOTIFICATION_CHANNEL_ID_ONGOING
            ).apply {
                setSmallIcon(R.drawable.ic_notify)
                setWhen(System.currentTimeMillis())
                setOngoing(true)
                startForeground(Constants.NOTIFICATION_ID_MAINTENANCE, this.build())
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
        val wallet = application?.getWallet()
        intent?.let {
            when {
                ACTION_DISMISS == it.action -> handleDismiss()
                ACTION_DISMISS_FOREVER == it.action -> handleDismissForever()
                else -> wallet?.let { dat -> handleMaybeShowNotification(dat) }
            }
        }
    }

    private fun handleMaybeShowNotification(wallet: Wallet) {
        val estimatedBalance = wallet.getBalance(Wallet.BalanceType.ESTIMATED_SPENDABLE)

        if (estimatedBalance.isPositive) {
            val btcFormat = config?.format
            val title = getString(R.string.notification_title)
            val text = StringBuilder(
                getString(R.string.notification_message, btcFormat?.format(estimatedBalance))
            )
            val dismissIntent = Intent(this, NotificationService::class.java)
            dismissIntent.action = ACTION_DISMISS
            val dismissForeverIntent = Intent(this, NotificationService::class.java)
            dismissForeverIntent.action = ACTION_DISMISS_FOREVER

            val notification = NotificationCompat.Builder(
                this,
                Constants.NOTIFICATION_CHANNEL_ID_IMPORTANT
            )
            notification.setStyle(NotificationCompat.BigTextStyle().bigText(text))
            notification.setSmallIcon(R.drawable.ic_notify)
            notification.setContentTitle(title)
            notification.setContentText(text)
            notification
                .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0, Intent(this, MainActivity::class.java).setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        ), 0
                    )
                )
            notification.setAutoCancel(true)
            notification.addAction(
                NotificationCompat.Action.Builder(
                    0,
                    getString(R.string.notification_dismiss),
                    PendingIntent.getService(this, 0, dismissIntent, 0)
                ).build()
            )
            notification.addAction(
                NotificationCompat.Action.Builder(
                    0,
                    getString(R.string.notification_dismiss_forever),
                    PendingIntent.getService(this, 0, dismissForeverIntent, 0)
                ).build()
            )
            nm?.notify(Constants.NOTIFICATION_ID_INACTIVITY, notification.build())
        }
    }

    private fun handleDismiss() {
        nm?.cancel(Constants.NOTIFICATION_ID_INACTIVITY)
    }

    private fun handleDismissForever() {
        config?.setRemindBalance(false)
        nm?.cancel(Constants.NOTIFICATION_ID_INACTIVITY)
    }

    companion object {
        fun startMaybeShowNotification(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, NotificationService::class.java)
            )
        }

        private val ACTION_DISMISS = NotificationService::class.java.getPackage()?.name + ".dismiss"
        private val ACTION_DISMISS_FOREVER =
            NotificationService::class.java.getPackage()?.name + ".dismiss_forever"
    }
}