package com.campusquest.story

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.content.ContextCompat
import com.campusquest.R

class KeyBoxView(context:Context):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val gold=ContextCompat.getColor(context,R.color.secondary_amber)
    private val cream=ContextCompat.getColor(context,R.color.on_primary_emerald)
    var box=KeyBox()
    var active=false
    var onSolved:()->Unit={}
    init {contentDescription="Tilt-only key maze. Follow five alternating passages around eight barriers to the gold opening at the top right."}
    fun step(dx:Float,dy:Float) {
        if(!active)return
        box.move(dx,dy);invalidate()
        if(box.complete){active=false;onSolved()}
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val side=kotlin.math.min(width,height).toFloat()
        canvas.save();canvas.translate((width-side)/2,(height-side)/2);canvas.scale(side/1000f,side/1000f)
        paint.style=Paint.Style.FILL
        paint.shader=RadialGradient(480f,500f,750f,intArrayOf(Color.rgb(29,68,49),Color.rgb(9,29,23)),null,Shader.TileMode.CLAMP)
        canvas.drawRoundRect(0f,0f,1000f,1000f,38f,38f,paint);paint.shader=null
        paint.color=Color.rgb(53,79,59)
        for(x in 1..19)for(y in 1..19)canvas.drawCircle(x*50f,y*50f,1.8f,paint)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=5f;paint.color=Color.rgb(146,135,83)
        canvas.drawRoundRect(5f,5f,995f,995f,35f,35f,paint)
        paint.style=Paint.Style.FILL
        for(wall in KeyBox.walls) {
            val r=RectF(wall.left*1000,wall.top*1000,wall.right*1000,wall.bottom*1000)
            paint.color=Color.rgb(7,24,18);canvas.drawRoundRect(r.left+4,r.top+6,r.right+4,r.bottom+6,5f,5f,paint)
            paint.color=Color.rgb(169,143,84);canvas.drawRoundRect(r,5f,5f,paint)
            paint.color=Color.rgb(228,199,130);canvas.drawRect(r.left+3,r.top+2,r.right-3,r.top+5,paint)
        }
        paint.color=Color.argb(70,244,191,98);canvas.drawRoundRect(825f,18f,975f,112f,16f,16f,paint)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=4f;paint.color=gold
        canvas.drawRoundRect(825f,18f,975f,112f,16f,16f,paint)
        paint.style=Paint.Style.FILL;paint.typeface=Typeface.create("sans-serif-medium",Typeface.BOLD);paint.textAlign=Paint.Align.CENTER;paint.textSize=26f;paint.color=cream
        canvas.drawText("EXIT",900f,80f,paint)
        paint.color=Color.argb(70,244,191,98);canvas.drawCircle(120f,900f,40f,paint)
        paint.color=Color.rgb(179,189,157);paint.textSize=21f;canvas.drawText("START",120f,963f,paint)
        val x=box.x*1000;val y=box.y*1000
        paint.color=Color.argb(40,255,207,113);canvas.drawCircle(x,y,37f,paint)
        paint.color=gold;canvas.drawCircle(x,y,KeyBox.RADIUS*1000,paint)
        paint.color=Color.rgb(30,55,35);paint.style=Paint.Style.STROKE;paint.strokeWidth=5f
        canvas.drawCircle(x-7,y-4,8f,paint);canvas.drawLine(x-1,y+2,x+13,y+16,paint);canvas.drawLine(x+7,y+9,x+13,y+3,paint)
        paint.style=Paint.Style.FILL
        canvas.restore()
    }
}
