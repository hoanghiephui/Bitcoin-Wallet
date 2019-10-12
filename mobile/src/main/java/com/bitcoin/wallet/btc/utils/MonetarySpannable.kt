package com.bitcoin.wallet.btc.utils

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.bitcoin.wallet.btc.Constants
import org.bitcoinj.core.Monetary
import org.bitcoinj.utils.MonetaryFormat

class MonetarySpannable : SpannableString {

    constructor(
        format: MonetaryFormat?, signed: Boolean,
        monetary: Monetary?
    ) : super(format(format, signed, monetary))

    constructor(format: MonetaryFormat?, monetary: Monetary?) : super(
        format(
            format,
            false,
            monetary
        )
    )

    fun applyMarkup(
        prefixSpans: Array<Any>?,
        insignificantSpans: Array<Any>?
    ): MonetarySpannable {
        applyMarkup(this, prefixSpans, STANDARD_SIGNIFICANT_SPANS, insignificantSpans)
        return this
    }

    companion object {
        val BOLD_SPAN: Any = StyleSpan(Typeface.BOLD)
        val SMALLER_SPAN = RelativeSizeSpan(0.85f)
        @JvmField
        val STANDARD_SIGNIFICANT_SPANS = arrayOf(BOLD_SPAN)
        @JvmField
        val STANDARD_INSIGNIFICANT_SPANS = arrayOf<Any>(MonetarySpannable.SMALLER_SPAN)

        private fun format(
            format: MonetaryFormat?, signed: Boolean,
            monetary: Monetary?
        ): CharSequence {
            if (monetary == null)
                return ""
            if (format == null)
                return monetary.toString()
            if (monetary.signum() >= 0 || signed) {

                return if (signed)
                    format.negativeSign(Constants.CURRENCY_MINUS_SIGN).positiveSign(Constants.CURRENCY_PLUS_SIGN)
                        .format(monetary)
                else
                    format.format(monetary)
            } else {
                return ""
            }
        }

        fun applyMarkup(
            spannable: Spannable, prefixSpans: Array<Any>?,
            significantSpans: Array<Any>?, insignificantSpans: Array<Any>?
        ) {
            if (prefixSpans != null)
                for (span in prefixSpans)
                    spannable.removeSpan(span)
            if (significantSpans != null)
                for (span in significantSpans)
                    spannable.removeSpan(span)
            if (insignificantSpans != null)
                for (span in insignificantSpans)
                    spannable.removeSpan(span)

            val m = Formats.PATTERN_MONETARY_SPANNABLE.matcher(spannable)
            if (m.find()) {
                var i = 0

                if (m.group(Formats.PATTERN_GROUP_PREFIX) != null) {
                    val end = m.end(Formats.PATTERN_GROUP_PREFIX)
                    if (prefixSpans != null)
                        for (span in prefixSpans)
                            spannable.setSpan(span, i, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end
                }

                if (m.group(Formats.PATTERN_GROUP_SIGNIFICANT) != null) {
                    val end = m.end(Formats.PATTERN_GROUP_SIGNIFICANT)
                    if (significantSpans != null)
                        for (span in significantSpans)
                            spannable.setSpan(span, i, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end
                }

                if (m.group(Formats.PATTERN_GROUP_INSIGNIFICANT) != null) {
                    val end = m.end(Formats.PATTERN_GROUP_INSIGNIFICANT)
                    if (insignificantSpans != null)
                        for (span in insignificantSpans)
                            spannable.setSpan(span, i, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }
}