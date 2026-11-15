package com.campusquest.story

import android.content.Context
import android.graphics.*
import android.view.View

class KeyBoxView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var box = KeyBox()
    var active = false
    var onSolved: () -> Unit = {}
    init { contentDescription = "Tilt-only key puzzle. Tilt your phone to guide the gold key around the two bars to the green opening." }
    fun step(dx: Float, dy: Float) {
        if (!active) return
        box.move(dx,dy); invalidate()
        if (box.complete) { active = false; onSolved() }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save();canvas.scale(width.toFloat(),height.toFloat())
        paint.color=Color.rgb(83,53,28);canvas.drawRoundRect(0f,0f,1f,1f,0.04f,0.04f,paint)
        paint.color=Color.rgb(154,109,57);canvas.drawRect(.32f,0f,.38f,.72f,paint);canvas.drawRect(.62f,.28f,.68f,1f,paint)
        paint.color=Color.rgb(115,182,119);canvas.drawRoundRect(.84f,.025f,.975f,.16f,.02f,.02f,paint)
        paint.color=Color.rgb(255,205,105);canvas.drawCircle(box.x,box.y,.035f,paint)
        canvas.restore()
    }
}
