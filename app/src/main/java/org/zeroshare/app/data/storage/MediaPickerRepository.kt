package org.zeroshare.app.data.storage

import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zeroshare.app.domain.model.ItemCategory
import org.zeroshare.app.domain.model.ShareableItem
import java.io.File

class MediaPickerRepository(private val context: Context) {

    suspend fun queryPhotos(): List<ShareableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ShareableItem>()
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val bucketName = if (bucketCol != -1) cursor.getString(bucketCol) else null

                    list.add(
                        ShareableItem(
                            id = "photo_$id",
                            uri = contentUri,
                            name = cursor.getString(nameCol) ?: "photo_$id.jpg",
                            size = cursor.getLong(sizeCol),
                            mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                            category = ItemCategory.PHOTO,
                            lastModified = cursor.getLong(modCol) * 1000,
                            folderName = bucketName?.takeIf { it.isNotBlank() } ?: "Camera"
                        )
                    )
                }
            }
        } catch (ignored: Exception) {}
        list
    }

    suspend fun queryVideos(): List<ShareableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ShareableItem>()
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val bucketName = if (bucketCol != -1) cursor.getString(bucketCol) else null

                    list.add(
                        ShareableItem(
                            id = "video_$id",
                            uri = contentUri,
                            name = cursor.getString(nameCol) ?: "video_$id.mp4",
                            size = cursor.getLong(sizeCol),
                            mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                            category = ItemCategory.VIDEO,
                            lastModified = cursor.getLong(modCol) * 1000,
                            folderName = bucketName?.takeIf { it.isNotBlank() } ?: "Videos"
                        )
                    )
                }
            }
        } catch (ignored: Exception) {}
        list
    }

    suspend fun queryAudio(): List<ShareableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ShareableItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.ALBUM
        )
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumName = if (albumCol != -1) cursor.getString(albumCol) else null

                    list.add(
                        ShareableItem(
                            id = "audio_$id",
                            uri = contentUri,
                            name = cursor.getString(nameCol) ?: "audio_$id.mp3",
                            size = cursor.getLong(sizeCol),
                            mimeType = cursor.getString(mimeCol) ?: "audio/mpeg",
                            category = ItemCategory.AUDIO,
                            lastModified = cursor.getLong(modCol) * 1000,
                            folderName = albumName?.takeIf { it.isNotBlank() } ?: "Music"
                        )
                    )
                }
            }
        } catch (ignored: Exception) {}
        list
    }

    suspend fun queryDocuments(): List<ShareableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ShareableItem>()
        val seenPaths = mutableSetOf<String>()

        val extensions = listOf(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".zip", ".rar", ".7z", ".csv", ".apk", ".epub", ".rtf"
        )

        // 1. Query MediaStore Files
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val selection = extensions.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?" }
            val selectionArgs = extensions.map { "%$it" }.toTypedArray()
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            val contentUri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(
                contentUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "document_$id"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val mod = cursor.getLong(modCol) * 1000
                    val fileUri = ContentUris.withAppendedId(contentUri, id)

                    val folder = inferDocumentFolder(name)
                    if (seenPaths.add(name.lowercase())) {
                        list.add(
                            ShareableItem(
                                id = "doc_$id",
                                uri = fileUri,
                                name = name,
                                size = size,
                                mimeType = mime,
                                category = ItemCategory.DOCUMENT,
                                lastModified = mod,
                                folderName = folder
                            )
                        )
                    }
                }
            }
        } catch (ignored: Exception) {}

        // 2. Query MediaStore Downloads on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.SIZE,
                    MediaStore.Downloads.MIME_TYPE,
                    MediaStore.Downloads.DATE_MODIFIED
                )
                val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                    val modCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameCol) ?: continue
                        val isDoc = extensions.any { name.lowercase().endsWith(it) }
                        if (!isDoc) continue

                        val id = cursor.getLong(idCol)
                        val size = cursor.getLong(sizeCol)
                        val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                        val mod = cursor.getLong(modCol) * 1000
                        val fileUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)

                        if (seenPaths.add(name.lowercase())) {
                            list.add(
                                ShareableItem(
                                    id = "dl_$id",
                                    uri = fileUri,
                                    name = name,
                                    size = size,
                                    mimeType = mime,
                                    category = ItemCategory.DOCUMENT,
                                    lastModified = mod,
                                    folderName = "Downloads"
                                )
                            )
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }

        // 3. Scan physical folders directly for any PDFs, Excels, Docs
        val searchDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Documents"),
            File("/storage/emulated/0/WhatsApp/Media/WhatsApp Documents"),
            File("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents"),
            File("/storage/emulated/0/Telegram/Telegram Documents")
        )

        searchDirs.forEach { dir ->
            if (dir != null && dir.exists() && dir.canRead()) {
                scanDirForDocuments(dir, list, seenPaths, extensions, maxDepth = 2, currentDepth = 0)
            }
        }

        list.sortedByDescending { it.lastModified }
    }

    private fun scanDirForDocuments(
        dir: File,
        results: MutableList<ShareableItem>,
        seen: MutableSet<String>,
        extensions: List<String>,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirForDocuments(file, results, seen, extensions, maxDepth, currentDepth + 1)
                }
            } else {
                val nameLower = file.name.lowercase()
                if (extensions.any { nameLower.endsWith(it) }) {
                    if (seen.add(nameLower)) {
                        val folder = file.parentFile?.name ?: "Downloads"
                        results.add(
                            ShareableItem(
                                id = "file_${file.absolutePath.hashCode()}",
                                uri = Uri.fromFile(file),
                                name = file.name,
                                size = file.length(),
                                mimeType = inferMimeType(nameLower),
                                category = ItemCategory.DOCUMENT,
                                lastModified = file.lastModified(),
                                folderName = folder
                            )
                        )
                    }
                }
            }
        }
    }

    private fun inferDocumentFolder(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.contains("download") -> "Downloads"
            lower.contains("wa") || lower.contains("whatsapp") -> "WhatsApp Documents"
            lower.contains("telegram") -> "Telegram"
            lower.endsWith(".pdf") -> "PDF Documents"
            lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv") -> "Spreadsheets"
            lower.endsWith(".ppt") || lower.endsWith(".pptx") -> "Presentations"
            lower.endsWith(".doc") || lower.endsWith(".docx") -> "Word Documents"
            lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z") -> "Archives"
            else -> "Documents"
        }
    }

    private fun inferMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".doc") -> "application/msword"
            lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".xls") -> "application/vnd.ms-excel"
            lower.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            lower.endsWith(".ppt") -> "application/vnd.ms-powerpoint"
            lower.endsWith(".pptx") -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".zip") -> "application/zip"
            lower.endsWith(".rar") -> "application/x-rar-compressed"
            lower.endsWith(".7z") -> "application/x-7z-compressed"
            lower.endsWith(".apk") -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    suspend fun queryInstalledApps(): List<ShareableItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }

        val appItems = mutableListOf<ShareableItem>()

        for (appInfo in installedApps) {
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            if (!isSystemApp || isUpdatedSystemApp) {
                val sourceDir = appInfo.sourceDir ?: continue
                val sourceFile = File(sourceDir)
                if (!sourceFile.exists()) continue

                val appLabel = pm.getApplicationLabel(appInfo).toString()
                val splitSourceDirs = appInfo.splitSourceDirs

                val isAppBundle = !splitSourceDirs.isNullOrEmpty()
                var totalAppSize = sourceFile.length()

                val splitUris = mutableListOf<Uri>()
                splitUris.add(Uri.fromFile(sourceFile))

                if (isAppBundle) {
                    splitSourceDirs.forEach { splitPath ->
                        val splitFile = File(splitPath)
                        if (splitFile.exists()) {
                            totalAppSize += splitFile.length()
                            splitUris.add(Uri.fromFile(splitFile))
                        }
                    }
                }

                appItems.add(
                    ShareableItem(
                        id = "app_${appInfo.packageName}",
                        uri = Uri.fromFile(sourceFile),
                        name = if (isAppBundle) "$appLabel.apks" else "$appLabel.apk",
                        size = totalAppSize,
                        mimeType = if (isAppBundle) "application/vnd.android.package-archive-split" else "application/vnd.android.package-archive",
                        category = ItemCategory.APP,
                        lastModified = sourceFile.lastModified(),
                        folderName = "Installed Apps",
                        packageName = appInfo.packageName,
                        isAppBundle = isAppBundle,
                        splitApkUris = splitUris
                    )
                )
            }
        }
        appItems.sortedBy { it.name.lowercase() }
    }
}
