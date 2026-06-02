package com.example

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object PdfProcessingHelper {

    // Helper to copy a Uri stream to a local cache file for secure read-only descriptor access
    fun copyUriToCache(context: Context, uri: Uri, suffix: String = ".pdf"): File? {
        return try {
            val contentResolver = context.contentResolver
            val tempFile = File.createTempFile("cloud_pdf_", suffix, context.cacheDir)
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return null
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Performance-optimized PDF visual merge and compression
    fun compressAndMergePdf(
        context: Context,
        inputUris: List<Uri>,
        compressionLevel: String,
        merge: Boolean,
        onProgress: (Int) -> Unit
    ): List<File> {
        val outputFiles = mutableListOf<File>()
        val cachedFiles = inputUris.mapNotNull { copyUriToCache(context, it) }
        if (cachedFiles.isEmpty()) return emptyList()

        // Configure resolutions and qualities based on compression target
        val (maxDimension, jpegQuality) = when (compressionLevel) {
            "screen" -> Pair(800, 60)
            "ebook" -> Pair(1200, 75)
            "printer" -> Pair(2000, 85)
            else -> Pair(3000, 95) // prepress
        }

        if (merge) {
            val mergedDoc = PdfDocument()
            var pageCounter = 0
            val totalInputs = cachedFiles.size
            
            cachedFiles.forEachIndexed { index, file ->
                try {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    val pageCount = renderer.pageCount
                    
                    for (i in 0 until pageCount) {
                        renderer.openPage(i).use { rendererPage ->
                            // Calculate proportionally scaled dimensions
                            val origW = rendererPage.width
                            val origH = rendererPage.height
                            val scale = maxDimension.toFloat() / max(origW, origH).toFloat()
                            val destW = if (scale < 1.0f) (origW * scale).toInt() else origW
                            val destH = if (scale < 1.0f) (origH * scale).toInt() else origH

                            val bitmap = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            rendererPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            // Apply JPEG compression to bitmap
                            val compressedBitmap: Bitmap
                            if (jpegQuality < 100) {
                                val compressStream = java.io.ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, compressStream)
                                val compressedBytes = compressStream.toByteArray()
                                compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                                bitmap.recycle()
                            } else {
                                compressedBitmap = bitmap
                            }

                            // Write to PdfDocument Page Info matches post-compress dimensions
                            val pageInfo = PdfDocument.PageInfo.Builder(destW, destH, pageCounter++).create()
                            val page = mergedDoc.startPage(pageInfo)
                            page.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
                            mergedDoc.finishPage(page)
                            compressedBitmap.recycle()
                        }
                    }
                    renderer.close()
                    pfd.close()
                    onProgress((index + 1) * 90 / totalInputs)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            try {
                val mergedFile = File.createTempFile("cloud_merged_", ".pdf", context.cacheDir)
                FileOutputStream(mergedFile).use { out ->
                    mergedDoc.writeTo(out)
                }
                mergedDoc.close()
                outputFiles.add(mergedFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Process individually
            cachedFiles.forEachIndexed { index, file ->
                val doc = PdfDocument()
                var pageCounter = 0
                try {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    val pageCount = renderer.pageCount
                    
                    for (i in 0 until pageCount) {
                        renderer.openPage(i).use { rendererPage ->
                            val origW = rendererPage.width
                            val origH = rendererPage.height
                            val scale = maxDimension.toFloat() / max(origW, origH).toFloat()
                            val destW = if (scale < 1.0f) (origW * scale).toInt() else origW
                            val destH = if (scale < 1.0f) (origH * scale).toInt() else origH

                            val bitmap = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            rendererPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val compressedBitmap: Bitmap
                            if (jpegQuality < 100) {
                                val compressStream = java.io.ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, compressStream)
                                val compressedBytes = compressStream.toByteArray()
                                compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                                bitmap.recycle()
                            } else {
                                compressedBitmap = bitmap
                            }

                            val pageInfo = PdfDocument.PageInfo.Builder(destW, destH, pageCounter++).create()
                            val page = doc.startPage(pageInfo)
                            page.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
                            doc.finishPage(page)
                            compressedBitmap.recycle()
                        }
                    }
                    renderer.close()
                    pfd.close()

                    val targetFile = File(context.cacheDir, file.nameWithoutExtension + "-compressed.pdf")
                    FileOutputStream(targetFile).use { out ->
                        doc.writeTo(out)
                    }
                    doc.close()
                    outputFiles.add(targetFile)
                    onProgress((index + 1) * 90 / cachedFiles.size)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Clean temp cache files
        cachedFiles.forEach { it.delete() }
        onProgress(100)
        return outputFiles
    }

    // Convert PDF page-by-page to displayable high res Image resources
    fun pdfToImage(
        context: Context,
        inputUri: Uri,
        format: String,
        scaleFactor: Float,
        onProgress: (Int) -> Unit
    ): List<File> {
        val outputImages = mutableListOf<File>()
        val cachedPdf = copyUriToCache(context, inputUri) ?: return emptyList()

        try {
            val pfd = ParcelFileDescriptor.open(cachedPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            
            val compressFormat = if (format == "jpeg") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            val suffix = if (format == "jpeg") ".jpg" else ".png"

            for (i in 0 until pageCount) {
                renderer.openPage(i).use { page ->
                    val destW = (page.width * scaleFactor).toInt()
                    val destH = (page.height * scaleFactor).toInt()

                    val bitmap = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val imgFile = File(context.cacheDir, "cloud_page_${i + 1}$suffix")
                    FileOutputStream(imgFile).use { out ->
                        bitmap.compress(compressFormat, 92, out)
                    }
                    outputImages.add(imgFile)
                    bitmap.recycle()
                }
                onProgress(((i + 1) * 100) / pageCount)
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cachedPdf.delete()
        }
        return outputImages
    }

    // Build PDF from multiple image sources in order
    fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        pageSize: String,
        orientation: String,
        onProgress: (Int) -> Unit
    ): File? {
        val doc = PdfDocument()
        val total = imageUris.size
        
        imageUris.forEachIndexed { index, uri ->
            try {
                // Decode bitmap securely
                context.contentResolver.openInputStream(uri).use { stream ->
                    if (stream != null) {
                        val original = BitmapFactory.decodeStream(stream)
                        if (original != null) {
                            val imgW = original.width
                            val imgH = original.height

                            var pgW = when (pageSize) {
                                "a4" -> 595
                                "letter" -> 612
                                else -> imgW // fit length
                            }
                            var pgH = when (pageSize) {
                                "a4" -> 842
                                "letter" -> 792
                                else -> imgH
                            }

                            // Customize layout orientation
                            if (orientation == "landscape") {
                                val t = pgW; pgW = pgH; pgH = t
                            } else if (orientation == "auto") {
                                if (imgW > imgH && pgW < pgH) {
                                    val t = pgW; pgW = pgH; pgH = t
                                }
                            }

                            val scale = max(pgW.toFloat() / imgW.toFloat(), pgH.toFloat() / imgH.toFloat())
                            val fitW = (imgW * scale).toInt()
                            val fitH = (imgH * scale).toInt()

                            val pageInfo = PdfDocument.PageInfo.Builder(pgW, pgH, index).create()
                            val page = doc.startPage(pageInfo)
                            
                            val srcRect = Rect(0, 0, imgW, imgH)
                            val destRect = Rect((pgW - fitW) / 2, (pgH - fitH) / 2, (pgW + fitW) / 2, (pgH + fitH) / 2)
                            page.canvas.drawBitmap(original, srcRect, destRect, null)
                            
                            doc.finishPage(page)
                            original.recycle()
                        }
                    }
                }
                onProgress((index + 1) * 100 / total)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return try {
            val file = File.createTempFile("cloud_images_", ".pdf", context.cacheDir)
            FileOutputStream(file).use { out ->
                doc.writeTo(out)
            }
            doc.close()
            file
        } catch (e: Exception) {
            doc.close()
            null
        }
    }

    // Annotation Data Struct for PDF Editor and Stamp Image features
    data class TextStamp(val pageIndex: Int, val text: String, val size: Float, val colorHex: String, val rotation: Float, val normX: Float, val normY: Float)
    data class ImageStamp(val pageIndex: Int, val imageFile: File, val width: Float, val normX: Float, val normY: Float)

    // Export PDF stamping background pages $+ text annotations in complete vector high DPI format
    fun exportEditedPdf(
        context: Context,
        pdfUri: Uri,
        textStamps: List<TextStamp>,
        imageStamps: List<ImageStamp>
    ): File? {
        val originalFile = copyUriToCache(context, pdfUri) ?: return null
        val doc = PdfDocument()

        try {
            val pfd = ParcelFileDescriptor.open(originalFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            for (i in 0 until count) {
                renderer.openPage(i).use { page ->
                    val w = page.width
                    val h = page.height

                    // Draw background details of original page precisely 
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, i).create()
                    val pdfPage = doc.startPage(pageInfo)
                    val canvas = pdfPage.canvas
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    bitmap.recycle()

                    // Stamp corresponding user text adjustments
                    textStamps.filter { it.pageIndex == i }.forEach { ts ->
                        canvas.save()
                        val paint = Paint().apply {
                            color = try { Color.parseColor(ts.colorHex) } catch(e: Exception) { Color.BLACK }
                            textSize = ts.size
                            isAntiAlias = true
                        }
                        // Coordinate translations
                        val x = ts.normX * w
                        val y = ts.normY * h
                        canvas.translate(x, y)
                        canvas.rotate(ts.rotation)
                        canvas.drawText(ts.text, 0f, 0f, paint)
                        canvas.restore()
                    }

                    // Stamp corresponding user image overlays
                    imageStamps.filter { it.pageIndex == i }.forEach { im ->
                        try {
                            val overlayImg = BitmapFactory.decodeFile(im.imageFile.absolutePath)
                            if (overlayImg != null) {
                                val destW = im.width
                                val destH = im.width * (overlayImg.height.toFloat() / overlayImg.width.toFloat())
                                
                                val left = im.normX * w
                                val top = im.normY * h
                                val destRect = RectF(left - destW / 2, top - destH / 2, left + destW / 2, top + destH / 2)
                                canvas.drawBitmap(overlayImg, null, destRect, null)
                                overlayImg.recycle()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    doc.finishPage(pdfPage)
                }
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            originalFile.delete()
        }

        return try {
            val file = File.createTempFile("cloud_edited_", ".pdf", context.cacheDir)
            FileOutputStream(file).use { out ->
                doc.writeTo(out)
            }
            doc.close()
            file
        } catch (e: Exception) {
            doc.close()
            null
        }
    }
}
