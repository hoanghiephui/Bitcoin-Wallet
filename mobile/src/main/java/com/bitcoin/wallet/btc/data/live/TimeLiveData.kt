package com.bitcoin.wallet.btc.data.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import com.bitcoin.wallet.btc.BitcoinApplication
import java.util.*

class TimeLiveData(private val application: BitcoinApplication) : LiveData<Date>() {

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            value = Date()
        }
    }

    override fun onActive() {
        application.registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        value = Date()
    }

    override fun onInactive() {
        application.unregisterReceiver(tickReceiver)
    }
}