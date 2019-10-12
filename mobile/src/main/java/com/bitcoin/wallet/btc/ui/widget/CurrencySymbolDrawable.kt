package com.bitcoin.wallet.btc.ui.widget

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.bitcoin.wallet.btc.Constants

class CurrencySymbolDrawable(symbol: String, textSize: Float, color: Int, private val y: Float) :
    Drawable() {
    private val paint = Paint()
    private val symbol: String

    init {
        paint.color = color
        paint.isAntiAlias = true
        paint.textSize = textSize

        this.symbol = symbol + Constants.CHAR_HAIR_SPACE
    }

    override fun draw(canvas: Canvas) {
        canvas.drawText(symbol, 0f, y, paint)
    }

    override fun getIntrinsicWidth(): Int {
        return paint.measureText(symbol).toInt()
    }

    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(cf: ColorFilter?) {}
}
