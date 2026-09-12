package com.zenlauncher.zenmode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FileResult(
    val displayName: String,
    val mimeType: String?,
    val uri: Uri
)

/**
 * On-device file search over MediaStore, the same surface stock launcher search uses.
 *
 * Scope note: on API 33+ the READ_MEDIA_* grants only expose images, video and audio,
 * so documents (PDFs and the like) are not reachable. Broadening that needs
 * MANAGE_EXTERNAL_STORAGE, which carries a Play Console review of its own. On API 32
 * and below READ_EXTERNAL_STORAGE does return every file type.
 *
 * The whole feature is compiled behind [BuildConfig.FILE_SEARCH_ENABLED] so a release
 * can ship with the storage permissions absent from the merged manifest.
 */
object FileSearchRepository {

    private const val RESULT_LIMIT = 12

    val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun isEnabled(): Boolean = BuildConfig.FILE_SEARCH_ENABLED

    fun hasPermission(context: Context): Boolean =
        isEnabled() && requiredPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    suspend fun search(context: Context, query: String): List<FileResult> {
        if (query.isBlank() || !hasPermission(context)) return emptyList()

        return withContext(Dispatchers.IO) {
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE
            )
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            val order = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            val results = mutableListOf<FileResult>()
            runCatching {
                context.contentResolver.query(
                    collection, projection, selection, selectionArgs, order
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val mimeColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                    while (cursor.moveToNext() && results.size < RESULT_LIMIT) {
                        val name = cursor.getString(nameColumn) ?: continue
                        results += FileResult(
                            displayName = name,
                            mimeType = cursor.getString(mimeColumn),
                            uri = android.content.ContentUris.withAppendedId(
                                collection,
                                cursor.getLong(idColumn)
                            )
                        )
                    }
                }
            }
            results
        }
    }

    fun open(context: Context, file: FileResult) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, file.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
    }
}
