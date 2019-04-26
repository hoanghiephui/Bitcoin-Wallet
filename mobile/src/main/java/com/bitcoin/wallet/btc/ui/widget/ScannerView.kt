package com.bitcoin.wallet.btc.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.bitcoin.wallet.btc.R
import com.google.zxing.ResultPoint
import java.util.HashMap

class ScannerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val maskPaint: Paint
    private val laserPaint: Paint
    private val dotPaint: Paint
    private var isResult: Boolean = false
    private val maskColor: Int
    private val maskResultColor: Int
    private val laserColor: Int
    private val dotColor: Int
    private val dotResultColor: Int
    private val dots = HashMap<FloatArray, Long>(16)
    private var frame: Rect? = null
    private val matrixs = Matrix()

    init {

        val res = resources
        maskColor = res.getColor(R.color.scan_mask)
        maskResultColor = res.getColor(R.color.scan_result_view)
        laserColor = res.getColor(R.color.scan_laser)
        dotColor = res.getColor(R.color.scan_dot)
        dotResultColor = res.getColor(R.color.scan_result_dots)

        maskPaint = Paint()
        maskPaint.style = Paint.Style.FILL

        laserPaint = Paint()
        laserPaint.strokeWidth = res.getDimensionPixelSize(R.dimen.scan_laser_width).toFloat()
        laserPaint.style = Paint.Style.STROKE

        dotPaint = Paint()
        dotPaint.alpha = DOT_OPACITY
        dotPaint.style = Paint.Style.STROKE
        dotPaint.strokeWidth = res.getDimension(R.dimen.scan_dot_size)
        dotPaint.isAntiAlias = true
    }

    fun setFraming(
        frame: Rect, framePreview: RectF, displayRotation: Int,
        cameraRotation: Int, cameraFlip: Boolean
    ) {
        this.frame = frame
        matrixs.setRectToRect(framePreview, RectF(frame), Matrix.ScaleToFit.FILL)
        matrixs.postRotate((-displayRotation).toFloat(), frame.exactCenterX(), frame.exactCenterY())
        matrixs.postScale((if (cameraFlip) -1 else 1).toFloat(), 1f, frame.exactCenterX(), frame.exactCenterY())
        matrixs.postRotate(cameraRotation.toFloat(), frame.exactCenterX(), frame.exactCenterY())

        invalidate()
    }

    fun setIsResult(isResult: Boolean) {
        this.isResult = isResult

        invalidate()
    }

    fun addDot(dot: ResultPoint) {
        dots[floatArrayOf(dot.x, dot.y)] = System.currentTimeMillis()

        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        if (frame == null)
            return

        val now = System.currentTimeMillis()

        val width = width
        val height = height

        val point = FloatArray(2)

        // draw mask darkened
        maskPaint.color = if (isResult) maskResultColor else maskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame!!.top.toFloat(), maskPaint)
        canvas.drawRect(0f, frame!!.top.toFloat(), frame!!.left.toFloat(), (frame!!.bottom + 1).toFloat(), maskPaint)
        canvas.drawRect(
            (frame!!.right + 1).toFloat(),
            frame!!.top.toFloat(),
            width.toFloat(),
            (frame!!.bottom + 1).toFloat(),
            maskPaint
        )
        canvas.drawRect(0f, (frame!!.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), maskPaint)

        if (isResult) {
            laserPaint.color = dotResultColor
            laserPaint.alpha = 160

            dotPaint.color = dotResultColor
        } else {
            laserPaint.color = laserColor
            val laserPhase = now / 600 % 2 == 0L
            laserPaint.alpha = if (laserPhase) 160 else 255

            dotPaint.color = dotColor

            // schedule redraw
            postInvalidateDelayed(LASER_ANIMATION_DELAY_MS)
        }

        canvas.drawRect(frame!!, laserPaint)

        // draw points
        val i = dots.entries.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            val age = now - entry.value
            if (age < DOT_TTL_MS) {
                dotPaint.alpha = ((DOT_TTL_MS - age) * 256 / DOT_TTL_MS).toInt()

                matrixs.mapPoints(point, entry.key)
                canvas.drawPoint(point[0], point[1], dotPaint)
            } else {
                i.remove()
            }
        }
    }

    companion object {
        private val LASER_ANIMATION_DELAY_MS = 100L
        private val DOT_OPACITY = 0xa0
        private val DOT_TTL_MS = 500
    }
}