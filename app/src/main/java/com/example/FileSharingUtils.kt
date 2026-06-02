package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileSharingUtils {
    fun shareFile(context: Context, file: File, mimeType: String = "*/*") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.cloud.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Save or Share File"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
