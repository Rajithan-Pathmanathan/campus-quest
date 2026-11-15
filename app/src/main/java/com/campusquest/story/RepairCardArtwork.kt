package com.campusquest.story

import android.graphics.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object RepairCardArtwork {
    fun render(c:StoryConfigEntity):Bitmap {
        val matrix=QRCodeWriter().encode(LightTheWay.qr(c),BarcodeFormat.QR_CODE,600,600)
        val qr=Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888)
        for(y in 0 until 600)for(x in 0 until 600)qr.setPixel(x,y,if(matrix[x,y])Color.BLACK else Color.WHITE)
        return Bitmap.createBitmap(800,950,Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas=Canvas(bitmap);canvas.drawColor(Color.WHITE)
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.BLACK;textSize=32f;textAlign=Paint.Align.CENTER}
            canvas.drawText("Light the Way — Repair Card",400f,65f,paint)
            canvas.drawBitmap(qr,100f,100f,paint)
            canvas.drawText("Backup code: ${c.backupCode}",400f,775f,paint)
            paint.textSize=24f;canvas.drawText("Scan this card in Campus Quest",400f,835f,paint)
            qr.recycle()
        }
    }
}
