package com.campusquest.story

data class MazeWall(val left:Float,val top:Float,val right:Float,val bottom:Float)

/** One geometry definition drives both collision and rendering. Five switchbacks plus three short barriers. */
class KeyBox {
    companion object {
        const val RADIUS=.026f
        val walls=listOf(
            MazeWall(0f,.77f,.78f,.80f),
            MazeWall(.22f,.61f,1f,.64f),
            MazeWall(0f,.45f,.78f,.48f),
            MazeWall(.22f,.29f,1f,.32f),
            MazeWall(0f,.13f,.78f,.16f),
            MazeWall(.48f,.73f,.52f,.77f),
            MazeWall(.48f,.48f,.52f,.52f),
            MazeWall(.48f,.41f,.52f,.45f)
        )
    }
    var x=.12f; private set
    var y=.90f; private set
    private fun free(x:Float,y:Float):Boolean {
        if(x !in RADIUS..(1-RADIUS) || y !in RADIUS..(1-RADIUS))return false
        return walls.none {x+RADIUS>it.left && x-RADIUS<it.right && y+RADIUS>it.top && y-RADIUS<it.bottom}
    }
    fun move(dx:Float,dy:Float) {
        if(!dx.isFinite() || !dy.isFinite())return
        val steps=(kotlin.math.max(kotlin.math.abs(dx),kotlin.math.abs(dy))/.01f).toInt().coerceIn(1,400)
        repeat(steps){
            if(free(x+dx/steps,y))x+=dx/steps
            if(free(x,y+dy/steps))y+=dy/steps
        }
    }
    val complete get()=x>.84f && y<.105f
}
