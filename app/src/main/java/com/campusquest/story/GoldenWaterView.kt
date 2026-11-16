package com.campusquest.story

import android.content.Context
import android.graphics.*
import android.view.View

class GoldenWaterView(context: Context) : View(context) {
    var water = GoldenWater()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        canvas.save()
        canvas.translate((width-size)/2f,(height-size)/2f)
        canvas.scale(size/400f,size/400f)
        paint.color=Color.rgb(16,45,35)
        canvas.drawRoundRect(0f,0f,400f,400f,24f,24f,paint)
        val jug=RectF(85f,75f,315f,335f)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=12f;paint.color=Color.rgb(194,157,87)
        canvas.drawRoundRect(290f,120f,365f,250f,28f,28f,paint)
        canvas.drawRoundRect(jug,32f,32f,paint)
        canvas.save()
        canvas.clipRect(92f,82f,308f,328f)
        val level=320f-water.remaining*175f
        val slope=water.leanX*100f
        val liquid=Path().apply {moveTo(90f,level-slope);quadTo(200f,level+water.leanY*55f,310f,level+slope);lineTo(310f,335f);lineTo(90f,335f);close()}
        paint.style=Paint.Style.FILL;paint.color=Color.rgb(245,185,47)
        canvas.drawPath(liquid,paint)
        paint.color=Color.rgb(255,227,133);paint.strokeWidth=5f
        canvas.drawLine(95f,level-slope,305f,level+slope,paint)
        canvas.restore()
        paint.color=Color.WHITE;paint.textSize=18f;paint.textAlign=Paint.Align.CENTER
        canvas.drawText("Keep the golden water inside",200f,38f,paint)
        canvas.drawText("${(water.remaining*100).toInt()}% water left",200f,375f,paint)
        contentDescription="Jug with ${(water.remaining*100).toInt()} percent golden water remaining"
        canvas.restore()
    }
}
