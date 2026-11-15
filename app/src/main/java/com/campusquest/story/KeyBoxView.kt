package com.campusquest.story

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class KeyBoxView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var box = KeyBox()
    var touchControls = false
    var active = false
    var onSolved: () -> Unit = {}
    private var previousX = 0f
    private var previousY = 0f
    init { contentDescription = "Key puzzle. Guide the gold key around the two bars to the green opening." }
    fun step(dx: Float, dy: Float) {
        if (!active) return
        box.move(dx,dy); invalidate()
        if (box.complete) { active = false; onSolved() }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!active || !touchControls) return false
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { previousX=event.x;previousY=event.y;parent.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE -> { step((event.x-previousX)/width,(event.y-previousY)/height); previousX=event.x;previousY=event.y }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { parent.requestDisallowInterceptTouchEvent(false); performClick() }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
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
