package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Hashtable

object QrGenerator {
    fun generateQrCode(
        text: String,
        size: Int,
        fgColorHex: String,
        bgColorHex: String,
        eccLevel: String
    ): Bitmap {
        val hints = Hashtable<EncodeHintType, Any>()
        val level = when (eccLevel) {
            "L" -> ErrorCorrectionLevel.L
            "M" -> ErrorCorrectionLevel.M
            "Q" -> ErrorCorrectionLevel.Q
            else -> ErrorCorrectionLevel.H
        }
        hints[EncodeHintType.ERROR_CORRECTION] = level
        hints[EncodeHintType.MARGIN] = 1

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        val fg = try { Color.parseColor(fgColorHex) } catch(e: Exception) { Color.WHITE }
        val bg = try { Color.parseColor(bgColorHex) } catch(e: Exception) { Color.BLACK }

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) fg else bg
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
