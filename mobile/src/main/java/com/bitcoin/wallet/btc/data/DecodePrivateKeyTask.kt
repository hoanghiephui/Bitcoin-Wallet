package com.bitcoin.wallet.btc.data

import android.os.Handler
import android.os.Looper
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.BIP38PrivateKey

abstract class DecodePrivateKeyTask(private val backgroundHandler: Handler) {
    private val callbackHandler: Handler = Handler(Looper.myLooper())

    fun decodePrivateKey(encryptedKey: BIP38PrivateKey, passphrase: String) {
        backgroundHandler.post {
            try {
                val decryptedKey = encryptedKey.decrypt(passphrase) // takes time

                callbackHandler.post { onSuccess(decryptedKey) }
            } catch (x: BIP38PrivateKey.BadPassphraseException) {
                callbackHandler.post { onBadPassphrase() }
            }
        }
    }

    protected abstract fun onSuccess(decryptedKey: ECKey)

    protected abstract fun onBadPassphrase()
}