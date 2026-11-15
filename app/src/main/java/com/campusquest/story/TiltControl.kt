package com.campusquest.story

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.atan2
import kotlin.math.sqrt

/** Accelerometer-only tilt input. Warm-up and neutral calibration never require a gyroscope. */
class TiltControl {
    private var filteredX=0f
    private var filteredY=0f
    private var filteredZ=0f
    private var neutralX=0f
    private var neutralY=0f
    private var samples=0
    private var pendingCalibration=true
    private var lastRotation=0
    private var lastSample:Long?=null
    var x=0f; private set
    var y=0f; private set
    var calibrated=false; private set

    fun reset() {samples=0;lastSample=null;calibrated=false;pendingCalibration=true;x=0f;y=0f}
    fun calibrate() {pendingCalibration=true;calibrated=false;x=0f;y=0f}
    fun fresh(now:Long)=calibrated && lastSample?.let {now-it in 0..1000}==true

    fun sample(rawX:Float,rawY:Float,rotation:Int,now:Long,rawZ:Float=9.81f) {
        if(!rawX.isFinite() || !rawY.isFinite() || !rawZ.isFinite())return
        if(rotation!=lastRotation){lastRotation=rotation;samples=0;calibrate()}
        if(samples==0){filteredX=rawX;filteredY=rawY;filteredZ=rawZ}
        else {filteredX += (rawX-filteredX)*.2f;filteredY += (rawY-filteredY)*.2f;filteredZ += (rawZ-filteredZ)*.2f}
        samples++;lastSample=now
        val axes=when(rotation){1 -> filteredY to -filteredX;2 -> -filteredX to -filteredY;3 -> -filteredY to filteredX;else -> filteredX to filteredY}
        val roll=atan2(axes.first,sqrt(axes.second*axes.second+filteredZ*filteredZ))
        val pitch=atan2(axes.second,filteredZ)
        if(pendingCalibration && samples>=12){neutralX=roll;neutralY=pitch;pendingCalibration=false;calibrated=true}
        if(!calibrated)return
        fun control(delta:Float):Float {
            val wrapped=atan2(kotlin.math.sin(delta),kotlin.math.cos(delta))
            val value=(wrapped/Math.toRadians(20.0).toFloat()).coerceIn(-1f,1f)
            return if(abs(value)<.06f)0f else sign(value)*(abs(value)-.06f)/.94f
        }
        x=control(neutralX-roll)
        y=control(pitch-neutralY)
    }
}
