package com.bitcoin.wallet.mobile.ui.widget

import android.view.View
import android.widget.TextView
import com.bitcoin.wallet.mobile.Constants
import org.bitcoinj.core.Coin
import org.bitcoinj.utils.ExchangeRate
import org.bitcoinj.utils.Fiat

class CurrencyCalculatorLink(
    private val btcAmountView: CurrencyAmountView,
    private val localAmountView: CurrencyAmountView
) {

    private var listener: CurrencyAmountView.Listener? = null
    private var enabled = true
    var exchangeRate: ExchangeRate? = null
        set(exchangeRate) {
            field = exchangeRate
            update()
        }
    var exchangeDirection = true
        set(exchangeDirection) {
            field = exchangeDirection
            update()
        }

    val amount: Coin?
        get() {
            if (this.exchangeDirection) {
                return btcAmountView.amount as Coin?
            } else if (this.exchangeRate != null) {
                val localAmount = localAmountView.amount as Fiat? ?: return null
                try {
                    val btcAmount = this.exchangeRate?.fiatToCoin(localAmount)
                    if (btcAmount?.isGreaterThan(Constants.NETWORK_PARAMETERS.maxMoney) == true)
                        throw ArithmeticException()
                    return btcAmount
                } catch (x: ArithmeticException) {
                    return null
                }

            } else {
                return null
            }
        }

    init {
        val btcAmountViewListener = object : CurrencyAmountView.Listener {
            override fun changed() {
                if (btcAmountView.amount != null)
                    exchangeDirection = true
                else
                    localAmountView.setHint(null)

                listener?.changed()
            }

            override fun focusChanged(hasFocus: Boolean) {
                listener?.focusChanged(hasFocus)
            }
        }
        this.btcAmountView.setListener(btcAmountViewListener)
        val localAmountViewListener = object : CurrencyAmountView.Listener {
            override fun changed() {
                if (localAmountView.amount != null)
                    exchangeDirection = false
                else
                    btcAmountView.setHint(null)
                listener?.changed()
            }

            override fun focusChanged(hasFocus: Boolean) {
                listener?.focusChanged(hasFocus)
            }
        }
        this.localAmountView.setListener(localAmountViewListener)

        update()
    }

    fun setListener(listener: CurrencyAmountView.Listener?) {
        this.listener = listener
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        update()
    }

    fun hasAmount(): Boolean {
        return amount != null
    }

    private fun update() {
        btcAmountView.isEnabled = enabled

        if (this.exchangeRate != null) {
            localAmountView.isEnabled = enabled
            localAmountView.setCurrencySymbol(this.exchangeRate?.fiat?.currencyCode)

            if (this.exchangeDirection) {
                val btcAmount = btcAmountView.amount as Coin?
                if (btcAmount != null) {
                    btcAmountView.setHint(null)
                    localAmountView.setAmount(null, false)
                    try {
                        val localAmount = this.exchangeRate?.coinToFiat(btcAmount)
                        localAmountView.setHint(localAmount)
                    } catch (x: ArithmeticException) {
                        localAmountView.setHint(null)
                    }

                }
            } else {
                val localAmount = localAmountView.amount as Fiat?
                if (localAmount != null) {
                    localAmountView.setHint(null)
                    btcAmountView.setAmount(null, false)
                    try {
                        val btcAmount = this.exchangeRate?.fiatToCoin(localAmount)
                        if (btcAmount?.isGreaterThan(Constants.NETWORK_PARAMETERS.maxMoney) == true)
                            throw ArithmeticException()
                        btcAmountView.setHint(btcAmount)
                    } catch (x: ArithmeticException) {
                        btcAmountView.setHint(null)
                    }

                }
            }
        } else {
            localAmountView.isEnabled = false
            localAmountView.setHint(null)
            btcAmountView.setHint(null)
        }
    }

    fun activeTextView(): TextView? {
        return if (this.exchangeDirection)
            btcAmountView.textView
        else
            localAmountView.textView
    }

    fun requestFocus() {
        activeTextView()?.requestFocus()
    }

    fun setBtcAmount(amount: Coin?) {
        val listener = this.listener
        this.listener = null
        btcAmountView.setAmount(amount, true)
        this.listener = listener
    }

    fun setNextFocusId(nextFocusId: Int) {
        btcAmountView.setNextFocusId(nextFocusId)
        localAmountView.setNextFocusId(nextFocusId)
    }
}