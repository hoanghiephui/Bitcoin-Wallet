package com.bitcoin.wallet.btc.data.live

import android.os.AsyncTask
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import com.google.common.collect.Iterables
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.wallet.Wallet
import java.util.*

class AddressToExcludeLiveData(application: BitcoinApplication) :
    BaseWalletLiveData<Set<String>>(application) {

    override fun onWalletActive(wallet: Wallet?) {
        loadAddressesToExclude()
    }

    private fun loadAddressesToExclude() {
        val wallet = wallet
        AsyncTask.execute {
            wallet?.let {
                val derivedKeys = it.issuedReceiveKeys
                Collections.sort(derivedKeys, DeterministicKey.CHILDNUM_ORDER)
                val randomKeys = it.importedKeys

                val addresses = HashSet<String>(derivedKeys.size + randomKeys.size)
                for (key in Iterables.concat(derivedKeys, randomKeys))
                    addresses.add(LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, key).toString())
                postValue(addresses)
            }
        }
    }
}