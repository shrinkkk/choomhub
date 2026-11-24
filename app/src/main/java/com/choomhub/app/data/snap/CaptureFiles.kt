package com.choomhub.app.data.snap

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Files the camera app writes photos into. They live in the app's cache
 * (cleared by Android when space is short) and are deleted once sent.
 */
object CaptureFiles {
    private const val DIR = "captures"

    fun newCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        // Only the newest capture matters; clear older ones.
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "snap_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    fun clear(context: Context) {
        File(context.cacheDir, DIR).listFiles()?.forEach { it.delete() }
    }
}
