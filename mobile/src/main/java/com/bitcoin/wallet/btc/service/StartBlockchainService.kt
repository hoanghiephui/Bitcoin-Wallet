package com.bitcoin.wallet.btc.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.text.format.DateUtils.*
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants.*


class StartBlockchainService : JobService() {

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val storageLow =
            registerReceiver(null, IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)) != null
        val batteryLow = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_LOW)) != null
        if (!storageLow && !batteryLow)
            BlockchainService.start(this, false)
        return false
    }

    companion object {
        @JvmStatic
        fun schedule(application: BitcoinApplication, expectLargeData: Boolean) {
            val config = application.config
            val lastUsedAgo = config.lastUsedAgo

            // apply some backoff
            val interval: Long
            interval = when {
                lastUsedAgo < LAST_USAGE_THRESHOLD_JUST_MS -> MINUTE_IN_MILLIS * 15
                lastUsedAgo < LAST_USAGE_THRESHOLD_TODAY_MS -> HOUR_IN_MILLIS
                lastUsedAgo < LAST_USAGE_THRESHOLD_RECENTLY_MS -> DAY_IN_MILLIS / 2
                else -> DAY_IN_MILLIS
            }

            val jobScheduler =
                application.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfo = JobInfo.Builder(
                0, ComponentName(
                    application,
                    StartBlockchainService::class.java
                )
            )
            jobInfo.setMinimumLatency(interval)
            jobInfo.setOverrideDeadline(WEEK_IN_MILLIS)
            jobInfo.setRequiredNetworkType(if (expectLargeData) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
            jobInfo.setRequiresDeviceIdle(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                jobInfo.setRequiresBatteryNotLow(true)
                jobInfo.setRequiresStorageNotLow(true)
            }
            jobScheduler.schedule(jobInfo.build())
        }
    }

}