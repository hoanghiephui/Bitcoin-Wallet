package com.bitcoin.wallet.mobile.data.live

import android.os.AsyncTask
import com.bitcoin.wallet.mobile.BitcoinApplication
import com.bitcoin.wallet.mobile.Constants
import org.bitcoinj.core.Address
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.Wallet

class FreshReceiveAddressLiveData(application: BitcoinApplication) : BaseWalletLiveData<Address>(application) {
    private var outputScriptType: Script.ScriptType? = null

    fun overrideOutputScriptType(outputScriptType: Script.ScriptType) {
        this.outputScriptType = outputScriptType
    }

    public override fun setValue(address: Address) {
        super.setValue(address)
    }

    override fun onWalletActive(wallet: Wallet?) {
        maybeLoad()
    }

    private fun maybeLoad() {
        if (value == null) {
            val wallet = wallet
            val outputScriptType = this.outputScriptType
            AsyncTask.execute {
                org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
                postValue(
                    if (outputScriptType != null)
                        wallet?.freshReceiveAddress(outputScriptType)
                    else
                        wallet?.freshReceiveAddress()
                )
            }
        }
    }
}