package com.campusquest.story

import android.content.Context
import android.graphics.*
import android.view.View

/** Lamp beam and cover respond directly to the task's verified progress. */
class LampView(context:Context):View(context) {
    var beamAngle=0f;set(value){field=value;invalidate()}
    var cover=0f;set(value){field=value.coerceIn(0f,1f);invalidate()}
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    init {contentDescription="Meeting lamp with a movable beam and glass cover"}
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        canvas.save();canvas.scale(width/320f,height/180f)
        paint.color=Color.rgb(18,43,32);canvas.drawRoundRect(0f,0f,320f,180f,18f,18f,paint)
        canvas.save();canvas.rotate(beamAngle.coerceIn(-90f,90f),160f,110f)
        val beam=Path().apply {moveTo(149f,110f);lineTo(85f,10f);lineTo(235f,10f);lineTo(171f,110f);close()}
        paint.color=Color.argb(90,255,213,126);canvas.drawPath(beam,paint);canvas.restore()
        paint.color=Color.rgb(211,160,79);canvas.drawRoundRect(128f,70f,192f,145f,8f,8f,paint)
        paint.color=Color.rgb(255,231,169);canvas.drawRoundRect(137f,80f,183f,132f,5f,5f,paint)
        paint.color=Color.rgb(159,115,55);canvas.drawRect(151f,145f,169f,164f,paint);canvas.drawRoundRect(125f,163f,195f,172f,5f,5f,paint)
        paint.color=Color.argb(160,133,192,187)
        canvas.drawRoundRect(131f,70f-(1-cover)*50f,189f,140f-(1-cover)*50f,5f,5f,paint)
        paint.color=Color.rgb(245,190,97);canvas.drawRect(127f,68f-(1-cover)*50f,193f,75f-(1-cover)*50f,paint)
        canvas.restore()
    }
}
