package com.bitcoin.wallet.btc.utils

import android.os.Handler
import android.os.Looper
import com.bitcoin.wallet.btc.Constants
import org.bitcoinj.crypto.KeyCrypterException
import org.bitcoinj.crypto.KeyCrypterScrypt
import org.bitcoinj.wallet.Wallet
import org.bouncycastle.crypto.params.KeyParameter

abstract class DeriveKeyTask(
    private val backgroundHandler: Handler?,
    private val scryptIterationsTarget: Int
) {
    private val callbackHandler: Handler = Handler(Looper.myLooper())

    fun deriveKey(wallet: Wallet, password: String) {
        if (!wallet.isEncrypted) {
            return
        }
        val keyCrypter = wallet.keyCrypter
        keyCrypter?.let {
            backgroundHandler?.post {
                org.bitcoinj.core.Context.propagate(Constants.CONTEXT)

                // Key derivation takes time.
                var key = it.deriveKey(password)
                var wasChanged = false

                // If the key isn't derived using the desired parameters, derive a new key.
                if (it is KeyCrypterScrypt) {
                    val scryptIterations = it.scryptParameters.n

                    if (scryptIterations != scryptIterationsTarget.toLong()) {
                        /*log.info(
                            "upgrading scrypt iterations from {} to {}; re-encrypting wallet", scryptIterations,
                            scryptIterationsTarget
                        )*/

                        val newKeyCrypter = KeyCrypterScrypt(scryptIterationsTarget)
                        val newKey = newKeyCrypter.deriveKey(password)

                        // Re-encrypt wallet with new key.
                        try {
                            wallet.changeEncryptionKey(newKeyCrypter, key, newKey)
                            key = newKey
                            wasChanged = true
                            //log.info("scrypt upgrade succeeded")
                        } catch (x: KeyCrypterException) {
                            //log.info("scrypt upgrade failed: {}", x.message)
                        }

                    }
                }

                // Hand back the (possibly changed) encryption key.
                val keyToReturn = key
                val keyToReturnWasChanged = wasChanged
                callbackHandler.post { onSuccess(keyToReturn, keyToReturnWasChanged) }
            }
        }
    }

    protected abstract fun onSuccess(encryptionKey: KeyParameter, changed: Boolean)

    companion object {
        //private val log = LoggerFactory.getLogger(DeriveKeyTask::class.java)todo log
    }
}