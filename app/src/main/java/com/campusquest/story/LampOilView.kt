package com.campusquest.story

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import androidx.core.content.ContextCompat
import com.campusquest.R

class LampOilView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gold = ContextCompat.getColor(context, R.color.secondary_amber)
    var fill = 0f
        set(value) { field = value.coerceIn(0f, 1f); updateDescription(); invalidate() }
    var tilt = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }
    var status = OilPourStatus.READY
        set(value) { field = value; updateDescription(); invalidate() }

    init { updateDescription() }

    private fun updateDescription() {
        contentDescription = "Virtual lamp oil bottle. Lamp ${Math.round(fill * 100)} percent full. ${status.name.lowercase().replace('_', ' ')}."
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(width / 320f, height / 260f)
        paint.shader = RadialGradient(160f, 130f, 260f, Color.rgb(28, 67, 48), Color.rgb(8, 27, 21), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(0f, 0f, 320f, 260f, 18f, 18f, paint)
        paint.shader = null

        canvas.save()
        canvas.rotate(tilt * 62f, 88f, 72f)
        paint.color = Color.rgb(83, 55, 30)
        canvas.drawRoundRect(48f, 35f, 118f, 125f, 12f, 12f, paint)
        paint.color = Color.rgb(246, 207, 103)
        canvas.drawRoundRect(55f, 62f, 111f, 118f, 8f, 8f, paint)
        paint.color = Color.rgb(151, 102, 42)
        canvas.drawRoundRect(106f, 52f, 143f, 76f, 6f, 6f, paint)
        paint.color = Color.rgb(255, 244, 190)
        canvas.drawCircle(75f, 78f, 7f, paint)
        canvas.restore()

        if (status == OilPourStatus.POURING) {
            paint.color = gold
            paint.strokeWidth = 9f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(154f, 95f, 218f, 158f, paint)
            paint.style = Paint.Style.FILL
        } else if (status == OilPourStatus.TOO_FAST || status == OilPourStatus.TOO_FAR) {
            paint.color = gold
            canvas.drawCircle(172f, 135f, 7f, paint)
            canvas.drawCircle(145f, 158f, 5f, paint)
            canvas.drawCircle(190f, 178f, 4f, paint)
        }

        paint.color = Color.rgb(128, 82, 35)
        val lamp = Path().apply { moveTo(196f, 168f); lineTo(286f, 168f); lineTo(272f, 232f); lineTo(210f, 232f); close() }
        canvas.drawPath(lamp, paint)
        paint.color = Color.rgb(255, 221, 126)
        canvas.drawRect(211f, 225f - 52f * fill, 271f, 225f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.rgb(219, 169, 82)
        canvas.drawPath(lamp, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 239, 177)
        canvas.drawOval(205f, 154f, 277f, 179f, paint)
        canvas.restore()
    }
}
