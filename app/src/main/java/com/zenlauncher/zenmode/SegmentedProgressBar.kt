package com.zenlauncher.zenmode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class SegmentedProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var segmentCount = 15
    private var segmentGap = 4f // Pixels
    private var cornerRadius = 4f // Pixels
    
    private var progress = 100 // 0 to 100
    private var filledColor = ContextCompat.getColor(context, R.color.zen_mindfulness_happy)
    private var emptyColor = ContextCompat.getColor(context, R.color.zen_mindfulness_track)

    fun setProgress(value: Int) {
        this.progress = value.coerceIn(0, 100)
        invalidate()
    }

    fun setFilledColor(color: Int) {
        this.filledColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        
        if (segmentCount <= 0) return

        val totalGapWidth = segmentGap * (segmentCount - 1)
        val segmentWidth = (w - totalGapWidth) / segmentCount
        
        // Calculate how many segments are filled
        val segmentsFilled = (progress.toFloat() / 100 * segmentCount).toInt().coerceAtLeast(1)

        for (i in 0 until segmentCount) {
            val left = i * (segmentWidth + segmentGap)
            val right = left + segmentWidth
            
            paint.color = if (i < segmentsFilled) filledColor else emptyColor
            
            // Draw rounded rectangle
            canvas.drawRoundRect(left, 0f, right, h, cornerRadius, cornerRadius, paint)
        }
    }
}
