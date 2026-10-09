package com.zenlauncher.zenmode.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.zenlauncher.zenmode.ui.components.saveImageToPictures
import com.zenlauncher.zenmode.ui.components.shareImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Getting a card out of the app. Every export draws a fresh [ShareArt] from [ShareKit] on a
 * background thread — never the instance on screen, whose paints the preview is busy with — at
 * the art's full size, so the file is the same 1080-wide card on every phone.
 */
object ShareExport {

    /** The settled card as a bitmap at its full export size. */
    fun render(art: ShareArt): Bitmap =
        Bitmap.createBitmap(art.format.width, art.format.height, Bitmap.Config.ARGB_8888).also {
            art.draw(Canvas(it), art.introMs.toFloat())
        }

    suspend fun shareImage(context: Context, art: (ShareKit) -> ShareArt, fileBaseName: String, text: String, chooserTitle: String) {
        val bitmap = withContext(Dispatchers.Default) { render(art(ShareKit(context))) }
        shareImage(context, bitmap, "$fileBaseName.png", text, chooserTitle)
    }

    suspend fun saveImage(context: Context, art: (ShareKit) -> ShareArt, fileBaseName: String) {
        val bitmap = withContext(Dispatchers.Default) { render(art(ShareKit(context))) }
        saveImageToPictures(context, bitmap, fileBaseName)
    }

    /**
     * Encodes the card's clip into the share cache and opens the share sheet with it. Returns
     * false if this phone couldn't make the clip (the caller then offers the image instead).
     * Cancelling the calling coroutine stops the encode.
     */
    suspend fun shareClip(
        context: Context,
        art: (ShareKit) -> ShareArt,
        fileBaseName: String,
        text: String,
        chooserTitle: String,
        onProgress: (Float) -> Unit
    ): Boolean {
        val file = encode(context, art, fileBaseName, onProgress) ?: return false
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, chooserTitle))
        }.onFailure { Log.e(TAG, "clip share failed", it) }.isSuccess
    }

    /** Encodes the clip and saves it under Movies/ZenMode. False if it couldn't be made or saved. */
    suspend fun saveClip(context: Context, art: (ShareKit) -> ShareArt, fileBaseName: String, onProgress: (Float) -> Unit): Boolean {
        val file = encode(context, art, fileBaseName, onProgress) ?: return false
        val saved = withContext(Dispatchers.IO) {
            runCatching {
                val name = "${fileBaseName}_${System.currentTimeMillis()}.mp4"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ZenMode")
                    }
                    val uri = requireNotNull(resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values))
                    requireNotNull(resolver.openOutputStream(uri)).use { out -> file.inputStream().use { it.copyTo(out) } }
                } else {
                    val dir = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)).apply { mkdirs() }
                    file.copyTo(File(dir, name), overwrite = true)
                }
            }.onFailure { Log.e(TAG, "clip save failed", it) }.isSuccess
        }
        if (saved) Toast.makeText(context, "Saved to Movies", Toast.LENGTH_SHORT).show()
        return saved
    }

    private suspend fun encode(context: Context, art: (ShareKit) -> ShareArt, fileBaseName: String, onProgress: (Float) -> Unit): File? {
        val folder = File(context.cacheDir, CLIP_FOLDER).apply { mkdirs() }
        val file = File(folder, "$fileBaseName.mp4")
        // Clips run to megabytes: sweep out old ones (not any a share target may still be reading).
        val stale = System.currentTimeMillis() - STALE_CLIP_MS
        folder.listFiles()?.filter { it != file && it.lastModified() < stale }?.forEach { it.delete() }
        return runCatching {
            runInterruptible(Dispatchers.Default) {
                ShareClip.encode(context, art(ShareKit(context)), file, onProgress)
            }
            file.takeIf { it.length() > 0 }
        }.onFailure {
            if (it is kotlinx.coroutines.CancellationException) throw it
            Log.e(TAG, "clip encode failed", it)
        }.getOrNull()
    }

    private const val TAG = "ShareExport"
    private const val STALE_CLIP_MS = 30 * 60 * 1_000L

    /** Matches the cache-path in res/xml/file_paths.xml. */
    private const val CLIP_FOLDER = "shared_clips"
}
