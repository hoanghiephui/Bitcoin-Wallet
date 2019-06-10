package com.bitcoin.wallet.btc.ui.widget

import android.content.Context
import android.os.SystemClock
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.lang.ref.WeakReference

class ShortTimeView(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs, android.R.attr.textViewStyle) {

    private val ticker = TickerRunnable(this)

    var showAbsoluteTime: Boolean = false
        set(value) {
            field = value
            invalidateTime()
        }

    var time: Long = 0
        set(value) {
            field = value
            invalidateTime()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    private fun invalidateTime() {
        if (showAbsoluteTime) {
            setTextIfChanged(formatSameDayTime(context, time))
        } else {
            val current = System.currentTimeMillis()
            if (Math.abs(current - time) > 60 * 1000) {
                setTextIfChanged(
                    DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL))
            } else {
                setTextIfChanged("Just now")
            }
        }
    }

    private fun setTextIfChanged(text: CharSequence?) {
        if (text == this.text) return
        setText(text)
    }

    private class TickerRunnable(view: ShortTimeView) : Runnable {

        private val viewRef = WeakReference(view)

        override fun run() {
            val view = viewRef.get() ?: return
            val handler = view.handler ?: return
            view.invalidateTime()
            val now = SystemClock.uptimeMillis()
            val next = now + TICKER_DURATION - now % TICKER_DURATION
            handler.postAtTime(this, next)
        }
    }

    companion object {
        private const val TICKER_DURATION = 5000L

        fun formatSameDayTime(context: Context, timestamp: Long): String? {
            if (DateUtils.isToday(timestamp))
                return DateUtils.formatDateTime(
                    context, timestamp,
                    if (DateFormat.is24HourFormat(context))
                        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_24HOUR
                    else
                        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_12HOUR
                )
            return DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE)
        }
    }

}