package com.bitcoin.wallet.mobile.data.live

import android.content.*
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.service.BlockchainService
import org.bitcoinj.core.StoredBlock

class BlocksLiveData constructor(private val application: BitcoinApplication) :
    LiveData<List<StoredBlock>>(), ServiceConnection {
    private val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(application)
    private var blockchainService: BlockchainService? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (blockchainService != null)
                value = blockchainService!!.getRecentBlocks(MAX_BLOCKS)
        }
    }

    override fun onActive() {
        broadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(BlockchainService.ACTION_BLOCKCHAIN_STATE)
        )
        application.bindService(Intent(application, BlockchainService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onInactive() {
        application.unbindService(this)
        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        blockchainService = (service as BlockchainService.LocalBinder).service
        value = blockchainService!!.getRecentBlocks(MAX_BLOCKS)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        blockchainService = null
    }

    companion object {
        private val MAX_BLOCKS = 100
    }
}