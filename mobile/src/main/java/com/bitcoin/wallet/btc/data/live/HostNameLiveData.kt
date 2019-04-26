package com.bitcoin.wallet.btc.data.live

import android.os.AsyncTask
import android.os.Handler
import androidx.lifecycle.LiveData
import java.net.InetAddress
import java.util.HashMap

class HostNameLiveData : LiveData<HashMap<InetAddress, String>>() {
    private val handler = Handler()

    init {
        value = HashMap()
    }

    fun reverseLookup(address: InetAddress) {
        val hostName = value
        if (hostName != null && !hostName.containsKey(address)) {
            AsyncTask.execute {
                val hostname = address.canonicalHostName
                handler.post {
                    hostName[address] = hostname
                    value = hostName
                }
            }
        }
    }
}