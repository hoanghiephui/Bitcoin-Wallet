package com.bitcoin.wallet.btc.data.live

import android.os.AsyncTask
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.listeners.KeyChainEventListener
import java.util.*
import kotlin.Comparator

class ImportedAddressLiveData(application: BitcoinApplication) :
    BaseWalletLiveData<List<Address>>(application), KeyChainEventListener {

    override fun onWalletActive(wallet: Wallet?) {
        wallet?.addKeyChainEventListener(Threading.SAME_THREAD, this)
        loadAddresses()
    }

    override fun onWalletInactive(wallet: Wallet?) {
        wallet?.removeKeyChainEventListener(this)
    }

    override fun onKeysAdded(keys: List<ECKey>) {
        loadAddresses()
    }

    private fun loadAddresses() {
        val wallet = wallet
        AsyncTask.execute {
            wallet?.let {
                val importedKeys = it.importedKeys
                Collections.sort(importedKeys, Comparator { lhs, rhs ->
                    val lhsRotating = it.isKeyRotating(lhs)
                    val rhsRotating = it.isKeyRotating(rhs)

                    if (lhsRotating != rhsRotating)
                        return@Comparator if (lhsRotating) 1 else -1
                    if (lhs.creationTimeSeconds != rhs.creationTimeSeconds) if (lhs.creationTimeSeconds > rhs.creationTimeSeconds) 1 else -1 else 0
                })
                val importedAddresses = ArrayList<Address>()
                for (key in importedKeys)
                    importedAddresses.add(LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, key))
                postValue(importedAddresses)
            }
        }
    }
}