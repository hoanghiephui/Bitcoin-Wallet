package com.bitcoin.wallet.btc.ui.widget

import android.content.Context
import android.graphics.Paint
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.utils.MonetarySpannable
import org.bitcoinj.core.Monetary
import org.bitcoinj.utils.MonetaryFormat

class CurrencyTextView : AppCompatTextView {
    private var amount: Monetary? = null
    private var format: MonetaryFormat? = null
    private var alwaysSigned = false
    private var prefixRelativeSizeSpan: RelativeSizeSpan? = null
    private var prefixScaleXSpan: ScaleXSpan? = null
    private var prefixColorSpan: ForegroundColorSpan? = null
    private var insignificantRelativeSizeSpan: RelativeSizeSpan? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun setAmount(amount: Monetary) {
        this.amount = amount
        updateView()
    }

    fun setFormat(format: MonetaryFormat?) {
        this.format = format?.codeSeparator(Constants.CHAR_HAIR_SPACE)
        updateView()
    }

    fun setAlwaysSigned(alwaysSigned: Boolean) {
        this.alwaysSigned = alwaysSigned
        updateView()
    }

    fun setStrikeThru(strikeThru: Boolean) {
        if (strikeThru)
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }

    fun setInsignificantRelativeSize(insignificantRelativeSize: Float) {
        if (insignificantRelativeSize != 1f) {
            this.prefixRelativeSizeSpan = RelativeSizeSpan(insignificantRelativeSize)
            this.insignificantRelativeSizeSpan = RelativeSizeSpan(insignificantRelativeSize)
        } else {
            this.prefixRelativeSizeSpan = null
            this.insignificantRelativeSizeSpan = null
        }
    }

    fun setPrefixColor(prefixColor: Int) {
        this.prefixColorSpan = ForegroundColorSpan(prefixColor)
        updateView()
    }

    fun setPrefixScaleX(prefixScaleX: Float) {
        this.prefixScaleXSpan = ScaleXSpan(prefixScaleX)
        updateView()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        //setPrefixColor(getResources().getColor(R.color.white));
        setPrefixScaleX(1f)
        setInsignificantRelativeSize(0.85f)
        setSingleLine()
    }

    private fun updateView() {
        try {
            val text: MonetarySpannable?
            if (amount != null)
                text = MonetarySpannable(format, alwaysSigned, amount).applyMarkup(
                    prefixRelativeSizeSpan?.let {
                        prefixScaleXSpan?.let { it1 ->
                            prefixColorSpan?.let { it2 ->
                                arrayOf<Any>(
                                    it,
                                    it1, it2
                                )
                            }
                        }
                    },
                    insignificantRelativeSizeSpan?.let { arrayOf<Any>(it) }
                )
            else
                text = null
            if (text != null) {
                setText(text)
            }
        } catch (ex: Exception) {

        }

    }
}
