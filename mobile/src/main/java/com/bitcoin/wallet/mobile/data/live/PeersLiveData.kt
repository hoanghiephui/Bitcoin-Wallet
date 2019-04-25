package com.bitcoin.wallet.mobile.data.live

import android.content.*
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.service.BlockchainService
import org.bitcoinj.core.Peer

class PeersLiveData constructor(private val application: BitcoinApplication) : LiveData<List<Peer>>(),
    ServiceConnection {
    private val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(application)
    private var blockchainService: BlockchainService? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (blockchainService != null)
                value = blockchainService?.connectedPeers
        }
    }

    override fun onActive() {
        broadcastManager.registerReceiver(broadcastReceiver, IntentFilter(BlockchainService.ACTION_PEER_STATE))
        application.bindService(Intent(application, BlockchainService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onInactive() {
        application.unbindService(this)
        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        blockchainService = (service as BlockchainService.LocalBinder).service
        value = blockchainService?.connectedPeers
    }

    override fun onServiceDisconnected(name: ComponentName) {
        blockchainService = null
    }
}
