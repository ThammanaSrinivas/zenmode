package com.zenlauncher.zenmode.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.widget.faces.Face
import com.zenlauncher.zenmode.widget.faces.FaceInput
import com.zenlauncher.zenmode.widget.faces.FaceResult
import com.zenlauncher.zenmode.widget.faces.FaceScope
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Draws one widget face into a bitmap at the device's density, inside Android's bitmap budget.
 *
 * The budget (Android 17 behaviour changes): all the bitmaps one widget update carries may not
 * pass 6 × screen width × height bytes; past it the update throws at target 37. A face is one
 * ARGB bitmap of its own size, and a launcher may ask for two sizes (portrait and landscape),
 * so each face is held to a third of the budget, drawn at a lower scale only if it must be.
 */
object WidgetRenderer {

    class Rendered(val bitmap: Bitmap, val face: FaceResult)

    @Synchronized
    fun render(context: Context, face: Face, input: FaceInput, density: Float, budgetBytes: Long = budget(context)): Rendered {
        val scale = scaleFor(input.w, input.h, density, budgetBytes / 3)
        val bitmap = Bitmap.createBitmap(
            (input.w * scale).roundToInt().coerceAtLeast(1),
            (input.h * scale).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        canvas.clipPath(android.graphics.Path().apply {
            addRoundRect(android.graphics.RectF(0f, 0f, input.w, input.h), input.radius, input.radius, android.graphics.Path.Direction.CW)
        })
        val result = face.draw(FaceScope(WidgetKit(context), canvas, input, scale))
        return Rendered(bitmap, result)
    }

    /** The density to draw at: the device's, unless the bitmap would pass [maxBytes]. */
    fun scaleFor(w: Float, h: Float, density: Float, maxBytes: Long): Float {
        val bytesAtDensity = w * density * h * density * 4
        if (bytesAtDensity <= maxBytes) return density
        return sqrt(maxBytes / (w * h * 4)).coerceAtLeast(1f)
    }

    fun budget(context: Context): Long {
        val m = context.resources.displayMetrics
        return 6L * m.widthPixels * m.heightPixels
    }
}
