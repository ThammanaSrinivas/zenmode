package com.zenlauncher.zenmode.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ── Shareable images ──────────────────────────────────────────────
// The streak milestone card and the Zen Circle leaderboard card both end up here:
// written to the cache and handed to the share sheet, or saved to Pictures/ZenMode.

private const val TAG = "ShareImage"

/** Writes [bitmap] to the share cache and opens the system share sheet with [text]. */
suspend fun shareImage(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    text: String,
    chooserTitle: String
) {
    try {
        val file = withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, "shared_images").apply { mkdirs() }
            File(folder, fileName).also { f ->
                f.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: Exception) {
        Log.e(TAG, "share failed", e)
    }
}

/** Saves [bitmap] as a PNG under Pictures/ZenMode and confirms with a toast. */
suspend fun saveImageToPictures(context: Context, bitmap: Bitmap, fileBaseName: String) {
    try {
        val saved = withContext(Dispatchers.IO) {
            val fileName = "${fileBaseName}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ZenMode")
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false.also { Log.e(TAG, "MediaStore insert() returned null") }
                val stream = resolver.openOutputStream(uri)
                    ?: return@withContext false.also { Log.e(TAG, "openOutputStream() returned null for $uri") }
                stream.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.apply { mkdirs() }
                File(dir, fileName).outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            }
            true
        }
        if (saved) Toast.makeText(context, "Saved to Pictures", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e(TAG, "save failed", e)
    }
}
