package com.bitcoin.wallet.btc.ui.widget

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.utils.GenericUtils
import com.bitcoin.wallet.btc.utils.MonetarySpannable
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Monetary
import org.bitcoinj.utils.MonetaryFormat

class CurrencyAmountView : FrameLayout {
    private val textViewListener = TextViewListener()
    private var significantColor: Int = 0
    private var lessSignificantColor: Int = 0
    private var errorColor: Int = 0
    private var deleteButtonDrawable: Drawable? = null
    private var contextButtonDrawable: Drawable? = null
    private var currencySymbolDrawable: Drawable? = null
    private var localCurrencyCode: String? = null
    private var inputFormat: MonetaryFormat? = null
    private var hint: Monetary? = null
    private var hintFormat = MonetaryFormat().noCode()
    private var amountSigned = false
    private var validateAmount = true

    var textView: TextView? = null
        private set
    private val deleteClickListener = OnClickListener {
        setAmount(null, true)
        textView?.requestFocus()
    }
    private var contextButton: View? = null
    private var listener: Listener? = null
    private var contextButtonClickListener: View.OnClickListener? = null

    val amount: Monetary?
        get() {
            if (!isValidAmount(false))
                return null

            val amountStr = textView?.text.toString().trim { it <= ' ' }
            return if (localCurrencyCode == null)
                inputFormat?.parse(amountStr)
            else
                inputFormat?.parseFiat(localCurrencyCode, amountStr)
        }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        val resources = context.resources
        significantColor = ContextCompat.getColor(context, R.color.fg_significant)
        lessSignificantColor = ContextCompat.getColor(context, R.color.fg_less_significant)
        errorColor = ContextCompat.getColor(context, R.color.fg_error)
        deleteButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_clear_grey_600_24dp)
    }

    fun setColor(significantColor: Int, lessSignificantColor: Int) {
        this.significantColor = significantColor
        this.lessSignificantColor = lessSignificantColor
        updateAppearance()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val context = context

        textView = getChildAt(0) as TextView
        textView?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        textView?.setHintTextColor(lessSignificantColor)
        textView?.isHorizontalFadingEdgeEnabled = true
        textView?.setSingleLine()
        setValidateAmount(textView is EditText)
        textView?.addTextChangedListener(textViewListener)
        textView?.onFocusChangeListener = textViewListener

        contextButton = object : View(context) {
            override fun onMeasure(wMeasureSpec: Int, hMeasureSpec: Int) {
                textView?.measuredHeight?.let { textView?.compoundPaddingRight?.let { it1 -> setMeasuredDimension(it1, it) } }
            }
        }
        val chooseViewParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        chooseViewParams.gravity = Gravity.RIGHT
        contextButton!!.layoutParams = chooseViewParams
        this.addView(contextButton)

        updateAppearance()
    }

    fun setCurrencySymbol(currencyCode: String?) {
        val bitcoinSymbol = Character.toString(Constants.CHAR_BITCOIN)
        val hasBitcoinSymbol =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && textView?.paint?.hasGlyph(bitcoinSymbol)!!
        val textSize = textView!!.textSize
        val smallerTextSize = textSize * (20f / 24f)
        val offset = textSize * 0.37f
        if (MonetaryFormat.CODE_BTC == currencyCode) {
            if (hasBitcoinSymbol)
                currencySymbolDrawable = CurrencySymbolDrawable(
                    bitcoinSymbol, smallerTextSize,
                    lessSignificantColor, offset
                )
            else
                currencySymbolDrawable = ContextCompat.getDrawable(context, R.drawable.currency_symbol_btc)
            localCurrencyCode = null
        } else if (MonetaryFormat.CODE_MBTC == currencyCode) {
            if (hasBitcoinSymbol)
                currencySymbolDrawable = CurrencySymbolDrawable(
                    "m$bitcoinSymbol", smallerTextSize,
                    lessSignificantColor, offset
                )
            else
                currencySymbolDrawable = ContextCompat.getDrawable(context, R.drawable.currency_symbol_mbtc)
            localCurrencyCode = null
        } else if (MonetaryFormat.CODE_UBTC == currencyCode) {
            if (hasBitcoinSymbol)
                currencySymbolDrawable = CurrencySymbolDrawable(
                    "µ$bitcoinSymbol", smallerTextSize,
                    lessSignificantColor, offset
                )
            else
                currencySymbolDrawable = ContextCompat.getDrawable(context, R.drawable.currency_symbol_ubtc)
            localCurrencyCode = null
        } else if (currencyCode != null) {
            currencySymbolDrawable = CurrencySymbolDrawable(
                GenericUtils.currencySymbol(currencyCode),
                smallerTextSize, lessSignificantColor, offset
            )
            localCurrencyCode = currencyCode
        } else {
            currencySymbolDrawable = null
            localCurrencyCode = null
        }

        updateAppearance()
    }

    fun setInputFormat(inputFormat: MonetaryFormat) {
        this.inputFormat = inputFormat.noCode()
    }

    fun setHintFormat(hintFormat: MonetaryFormat) {
        this.hintFormat = hintFormat.noCode()
        updateAppearance()
    }

    fun setHint(hint: Monetary?) {
        this.hint = hint
        updateAppearance()
    }

    fun setAmountSigned(amountSigned: Boolean) {
        this.amountSigned = amountSigned
    }

    fun setValidateAmount(validateAmount: Boolean) {
        this.validateAmount = validateAmount
    }

    fun setContextButton(contextButtonResId: Int, contextButtonClickListener: View.OnClickListener) {
        this.contextButtonDrawable = context.resources.getDrawable(contextButtonResId)
        this.contextButtonClickListener = contextButtonClickListener

        updateAppearance()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setAmount(amount: Monetary?, fireListener: Boolean) {
        if (!fireListener)
            textViewListener.setFire(false)

        if (amount != null)
            textView!!.text = MonetarySpannable(inputFormat, amountSigned, amount)
        else
            textView!!.text = null

        if (!fireListener)
            textViewListener.setFire(true)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        textView!!.isEnabled = enabled

        updateAppearance()
    }

    fun setTextColor(color: Int) {
        significantColor = color

        updateAppearance()
    }

    fun setStrikeThru(strikeThru: Boolean) {
        if (strikeThru)
            textView!!.paintFlags = textView!!.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            textView!!.paintFlags = textView!!.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }

    fun setNextFocusId(nextFocusId: Int) {
        textView!!.nextFocusDownId = nextFocusId
        textView!!.nextFocusForwardId = nextFocusId
    }

    private fun isValidAmount(zeroIsValid: Boolean): Boolean {
        val str = textView!!.text.toString().trim { it <= ' ' }

        try {
            if (!str.isEmpty()) {
                val amount: Monetary
                if (localCurrencyCode == null) {
                    amount = inputFormat!!.parse(str)
                    if (amount.isGreaterThan(Constants.NETWORK_PARAMETERS.maxMoney))
                        return false
                } else {
                    amount = inputFormat!!.parseFiat(localCurrencyCode, str)
                }

                // exactly zero
                return zeroIsValid || amount.signum() > 0
            }
        } catch (x: Exception) {
        }

        return false
    }

    private fun updateAppearance() {
        val enabled = textView!!.isEnabled

        contextButton!!.isEnabled = enabled

        val amount = textView!!.text.toString().trim { it <= ' ' }

        if (enabled && !amount.isEmpty()) {
            textView!!.setCompoundDrawablesWithIntrinsicBounds(currencySymbolDrawable, null, deleteButtonDrawable, null)
            contextButton!!.setOnClickListener(deleteClickListener)
        } else if (enabled && contextButtonDrawable != null) {
            textView!!.setCompoundDrawablesWithIntrinsicBounds(
                currencySymbolDrawable,
                null,
                contextButtonDrawable,
                null
            )
            contextButton!!.setOnClickListener(contextButtonClickListener)
        } else {
            textView!!.setCompoundDrawablesWithIntrinsicBounds(currencySymbolDrawable, null, null, null)
            contextButton!!.setOnClickListener(null)
        }

        contextButton!!.requestLayout()

        textView!!.setTextColor(if (!validateAmount || isValidAmount(true)) significantColor else errorColor)

        val hintSpannable = MonetarySpannable(hintFormat, if (hint != null) hint else Coin.ZERO)
            .applyMarkup(null, MonetarySpannable.STANDARD_INSIGNIFICANT_SPANS)
        textView!!.hint = hintSpannable
    }

    override fun onSaveInstanceState(): Parcelable? {
        val state = Bundle()
        state.putParcelable("super_state", super.onSaveInstanceState())
        state.putParcelable("child_textview", textView!!.onSaveInstanceState())
        state.putSerializable("amount", amount)
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable("super_state"))
            textView!!.onRestoreInstanceState(state.getParcelable("child_textview"))
            setAmount(state.getSerializable("amount") as Monetary, false)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    interface Listener {
        fun changed()

        fun focusChanged(hasFocus: Boolean)
    }

    private inner class TextViewListener : TextWatcher, View.OnFocusChangeListener {
        private var fire = true

        fun setFire(fire: Boolean) {
            this.fire = fire
        }

        override fun afterTextChanged(s: Editable) {
            // workaround for German keyboards
            val original = s.toString()
            val replaced = original.replace(',', '.')
            if (replaced != original) {
                s.clear()
                s.append(replaced)
            }

            MonetarySpannable.applyMarkup(
                s, null, MonetarySpannable.STANDARD_SIGNIFICANT_SPANS,
                MonetarySpannable.STANDARD_INSIGNIFICANT_SPANS
            )
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            updateAppearance()
            if (listener != null && fire)
                listener!!.changed()
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (!hasFocus) {
                val amount = amount
                if (amount != null)
                    setAmount(amount, false)
            }

            if (listener != null && fire)
                listener!!.focusChanged(hasFocus)
        }
    }
}
