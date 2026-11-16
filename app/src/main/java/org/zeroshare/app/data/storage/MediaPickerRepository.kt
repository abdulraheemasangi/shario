package org.zeroshare.app.data.storage

import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zeroshare.app.domain.model.ItemCategory
import org.zeroshare.app.domain.model.ShareableItem
import java.io.File

class MediaPickerRepository(private val context: Context) {

    suspend fun queryPhotos(): List<ShareableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ShareableItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                list.add(
                    ShareableItem(
                        id = "photo_$id",
                        uri = contentUri,
                        name = cursor.getString(nameCol) ?: "photo_$id.jpg",
                        size = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                        category = ItemCategory.PHOTO,
                        lastModified = cursor.getLong(modCol) * 1000
                    )
                )
            }
        }
        list
    }

    suspend fun queryVideos(): List<ShareableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ShareableItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                list.add(
                    ShareableItem(
                        id = "video_$id",
                        uri = contentUri,
                        name = cursor.getString(nameCol) ?: "video_$id.mp4",
                        size = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                        category = ItemCategory.VIDEO,
                        lastModified = cursor.getLong(modCol) * 1000
                    )
                )
            }
        }
        list
    }

    suspend fun queryAudio(): List<ShareableItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ShareableItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                list.add(
                    ShareableItem(
                        id = "audio_$id",
                        uri = contentUri,
                        name = cursor.getString(nameCol) ?: "audio_$id.mp3",
                        size = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol) ?: "audio/mpeg",
                        category = ItemCategory.AUDIO,
                        lastModified = cursor.getLong(modCol) * 1000
                    )
                )
            }
        }
        list
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
            // Filter out internal system apps without launcher intents unless updated
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
