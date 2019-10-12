package com.bitcoin.wallet.btc.utils

import android.os.Handler
import android.os.Looper
import com.bitcoin.wallet.btc.Constants
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.Transaction
import org.bitcoinj.crypto.KeyCrypterException
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet

abstract class SendCoinsOfflineTask(
    private val wallet: Wallet?,
    private val backgroundHandler: Handler?
) {
    private val callbackHandler: Handler = Handler(Looper.myLooper())

    fun sendCoinsOffline(sendRequest: SendRequest) {
        backgroundHandler?.post {
            org.bitcoinj.core.Context.propagate(Constants.CONTEXT)

            try {
                //log.info("sending: {}", sendRequest)
                val transaction = wallet?.sendCoinsOffline(sendRequest) // can take long
                //log.info("send successful, transaction committed: {}", transaction?.txId)

                callbackHandler.post { onSuccess(transaction) }
            } catch (x: InsufficientMoneyException) {
                val missing = x.missing
                if (missing != null)
                //log.info("send failed, {} missing", missing.toFriendlyString())
                else
                //log.info("send failed, insufficient coins")

                    callbackHandler.post { onInsufficientMoney(x.missing) }
            } catch (x: ECKey.KeyIsEncryptedException) {
                //log.info("send failed, key is encrypted: {}", x.message)

                callbackHandler.post { onFailure(x) }
            } catch (x: KeyCrypterException) {
                //log.info("send failed, key crypter exception: {}", x.message)

                val isEncrypted = wallet?.isEncrypted
                callbackHandler.post {
                    if (isEncrypted == true)
                        onInvalidEncryptionKey()
                    else
                        onFailure(x)
                }
            } catch (x: Wallet.CouldNotAdjustDownwards) {
                //log.info("send failed, could not adjust downwards: {}", x.message)

                callbackHandler.post { onEmptyWalletFailed() }
            } catch (x: Wallet.CompletionException) {
                //log.info("send failed, cannot complete: {}", x.message)

                callbackHandler.post { onFailure(x) }
            }
        }
    }

    protected abstract fun onSuccess(transaction: Transaction?)

    protected abstract fun onInsufficientMoney(missing: Coin?)

    protected abstract fun onInvalidEncryptionKey()

    protected open fun onEmptyWalletFailed() {
        onFailure(Wallet.CouldNotAdjustDownwards())
    }

    protected abstract fun onFailure(exception: Exception)

    /*companion object {

        private val log = LoggerFactory.getLogger(SendCoinsOfflineTask::class.java)
    }*/
}