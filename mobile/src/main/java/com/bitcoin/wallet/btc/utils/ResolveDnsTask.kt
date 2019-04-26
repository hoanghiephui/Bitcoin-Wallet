package com.bitcoin.wallet.btc.utils

import android.os.Handler
import android.os.Looper
import java.net.InetAddress
import java.net.UnknownHostException

abstract class ResolveDnsTask(private val backgroundHandler: Handler?) {
    private val callbackHandler: Handler = Handler(Looper.myLooper())

    fun resolve(hostname: String) {
        backgroundHandler?.post {
            try {
                val address = InetAddress.getByName(hostname) // blocks on network

                callbackHandler.post { onSuccess(address) }
            } catch (x: UnknownHostException) {
                callbackHandler.post { onUnknownHost() }
            }
        }
    }

    protected abstract fun onSuccess(address: InetAddress)

    protected abstract fun onUnknownHost()
}