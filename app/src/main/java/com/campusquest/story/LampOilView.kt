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
    var jarFill = 1f
        set(value) { field = value.coerceIn(0f, 1f); updateDescription(); invalidate() }
    var tilt = 0f
        set(value) { field = value.coerceIn(-90f, 90f); invalidate() }
    var status = OilPourStatus.READY
        set(value) { field = value; updateDescription(); invalidate() }

    init { updateDescription() }

    private fun updateDescription() {
        contentDescription = "Virtual lamp oil bottle. Lamp ${Math.round(fill * 100)} percent full, Jar ${Math.round(jarFill * 100)} percent remaining. ${status.name.lowercase().replace('_', ' ')}."
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(width / 320f, height / 260f)
        paint.shader = RadialGradient(160f, 130f, 260f, Color.rgb(28, 67, 48), Color.rgb(8, 27, 21), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(0f, 0f, 320f, 260f, 18f, 18f, paint)
        paint.shader = null

        val drawTilt = tilt.coerceIn(-90f, 90f)
        val jarPivotX = 88f
        val jarPivotY = 72f

        // Draw rotated jar matching phone's clockwise tilt physics
        canvas.save()
        canvas.rotate(drawTilt, jarPivotX, jarPivotY) // tilt is in degrees
        paint.color = Color.rgb(83, 55, 30)
        canvas.drawRoundRect(48f, 35f, 118f, 125f, 12f, 12f, paint)
        if (jarFill > 0f) {
            paint.color = Color.rgb(246, 207, 103)
            val liquidTop = 118f - (56f * jarFill)
            canvas.drawRoundRect(55f, liquidTop, 111f, 118f, 6f, 6f, paint)
        }
        paint.color = Color.rgb(151, 102, 42)
        canvas.drawRoundRect(106f, 52f, 143f, 76f, 6f, 6f, paint)
        paint.color = Color.rgb(255, 244, 190)
        canvas.drawCircle(75f, 78f, 7f, paint)
        canvas.restore()

        // Calculate spout tip position after clockwise tilt rotation
        val rad = Math.toRadians(drawTilt.toDouble())
        val unrotatedSpoutX = 143f - jarPivotX // 55f
        val unrotatedSpoutY = 64f - jarPivotY  // -8f
        val rotSpoutX = (jarPivotX + unrotatedSpoutX * Math.cos(rad) - unrotatedSpoutY * Math.sin(rad)).toFloat()
        val rotSpoutY = (jarPivotY + unrotatedSpoutX * Math.sin(rad) + unrotatedSpoutY * Math.cos(rad)).toFloat()

        if (status == OilPourStatus.POURING) {
            paint.color = gold
            paint.strokeWidth = 9f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(rotSpoutX, rotSpoutY, 240f, 168f, paint)
            paint.style = Paint.Style.FILL
        } else if (status == OilPourStatus.TOO_FAST || status == OilPourStatus.TOO_FAR) {
            paint.color = gold
            canvas.drawCircle(rotSpoutX + 5f, rotSpoutY + 20f, 7f, paint)
            canvas.drawCircle(rotSpoutX - 10f, rotSpoutY + 40f, 5f, paint)
            canvas.drawCircle(rotSpoutX + 15f, rotSpoutY + 60f, 4f, paint)
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
