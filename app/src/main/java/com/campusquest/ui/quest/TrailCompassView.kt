package com.campusquest.ui.quest

import android.content.Context
import android.graphics.*
import android.hardware.*
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import kotlin.math.*

/** A direct-bearing compass and proximity gauge, not a walking-route planner. */
class TrailCompassView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs), SensorEventListener {
    private val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotation = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var heading: Float? = null
    private var bearing: Float? = null
    private var distance: Float? = null
    private var declination = 0f
    private var angle = 0f
    private var listening = false
    private var reliable = true

    fun update(bearing: Float?, distance: Float?, latitude: Double?, longitude: Double?) {
        this.bearing = bearing
        this.distance = distance
        if (latitude != null && longitude != null) declination = GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis()).declination
        contentDescription = if (distance == null) "Compass waiting for GPS" else "${TrailGuidance.warmth(distance)}, approximately ${distance.roundToInt()} metres to checkpoint. Direct bearing; check your surroundings."
        invalidate()
    }

    fun start() {
        if (!listening && rotation != null) listening = sensors.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI)
    }
    fun stop() { sensors.unregisterListener(this); listening = false; heading = null }
    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { reliable = accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE; invalidate() }
    override fun onSensorChanged(event: SensorEvent) {
        val matrix = FloatArray(9)
        val adjusted = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        val axes = when (display?.rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, adjusted)
        heading = Math.toDegrees(SensorManager.getOrientation(adjusted, FloatArray(3))[0].toDouble()).toFloat() + declination
        val target = bearing?.let { TrailGuidance.relativeAngle(it, heading!!) } ?: 0f
        angle += TrailGuidance.relativeAngle(target, angle) * 0.24f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = min(width / 320f, height / 340f)
        canvas.save()
        canvas.translate((width - 320 * scale) / 2, (height - 340 * scale) / 2)
        canvas.scale(scale, scale)
        val gold = Color.rgb(242, 192, 103)
        val cream = Color.rgb(255, 242, 213)
        val heat = distance?.let(TrailGuidance::heat) ?: 0f
        val accent = if (heat > 0.9f) Color.rgb(255, 157, 84) else gold
        fun text(value: String, x: Float, y: Float, size: Float, color: Int = cream) {
            paint.style = Paint.Style.FILL; paint.color = color; paint.textSize = size; paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            canvas.drawText(value, x, y, paint)
        }
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(22, 49, 41)
        canvas.drawCircle(160f, 135f, 113f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = Color.rgb(94, 107, 76)
        canvas.drawCircle(160f, 135f, 109f, paint)
        for (i in 0 until 48) {
            canvas.save(); canvas.rotate(i * 7.5f, 160f, 135f)
            paint.color = if (i % 4 == 0) gold else Color.rgb(66, 86, 65)
            canvas.drawLine(160f, 33f, 160f, if (i % 4 == 0) 44f else 39f, paint); canvas.restore()
        }
        paint.strokeWidth = 7f; paint.strokeCap = Paint.Cap.ROUND; paint.color = accent
        canvas.drawArc(RectF(53f,28f,267f,242f), -90f, heat * 360, false, paint)
        val valid = bearing != null && heading != null && reliable
        if (valid) {
            canvas.save(); canvas.rotate(angle, 160f, 135f)
            val arrow = Path().apply { moveTo(160f,65f); lineTo(191f,164f); lineTo(160f,147f); lineTo(129f,164f); close() }
            paint.style = Paint.Style.FILL; paint.color = gold; canvas.drawPath(arrow,paint)
            canvas.restore()
        } else text("✦", 160f, 145f, 48f, gold)
        text(when { distance == null -> "FINDING YOUR POSITION"; rotation == null -> "COMPASS UNAVAILABLE"; !reliable -> "MOVE PHONE IN A FIGURE 8"; heading == null -> "HOLD PHONE LEVEL"; else -> "FOLLOW THE GOLD ARROW" },160f, 266f, 11f,gold)
        text(distance?.let { "~${if (it >= 1000f) "${(it / 1000f).roundToInt()} km" else "${it.roundToInt()} m"}  ·  ${TrailGuidance.warmth(it)}" } ?: "Waiting for GPS",160f,296f,22f)
        for (i in 0..19) {
            paint.color = if ((i + 1) / 20f <= heat) accent else Color.rgb(49, 70, 58)
            canvas.drawRoundRect(20f+i*14f,312f,30f+i*14f,321f,3f,3f,paint)
        }
        text("COLD",33f,337f,9f); text("VERY HOT",262f,337f,9f)
        canvas.restore()
    }
}
