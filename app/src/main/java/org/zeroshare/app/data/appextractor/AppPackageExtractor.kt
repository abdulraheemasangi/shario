package org.zeroshare.app.data.appextractor

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zeroshare.app.domain.model.ShareableItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handles extraction of single APKs and multi-split App Bundles (.apks),
 * as well as zero-failure installation of split packages via Android PackageInstaller sessions.
 */
class AppPackageExtractor(private val context: Context) {

    /**
     * If the app has multiple split APKs, bundle them into a single staging `.apks` container (zip archive).
     * If it is a monolithic APK, returns the raw APK stream directly.
     */
    suspend fun prepareAppForTransfer(item: ShareableItem): File = withContext(Dispatchers.IO) {
        if (!item.isAppBundle || item.splitApkUris.size <= 1) {
            // Single APK
            return@withContext File(item.uri.path ?: throw IllegalArgumentException("Invalid APK URI"))
        }

        // Split APK Bundle: Create an .apks zip container
        val cacheDir = File(context.cacheDir, "extracted_bundles")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val bundleFile = File(cacheDir, "${item.name}.apks")
        if (bundleFile.exists()) bundleFile.delete()

        ZipOutputStream(FileOutputStream(bundleFile)).use { zos ->
            item.splitApkUris.forEachIndexed { index, uri ->
                val sourceFile = File(uri.path ?: return@forEachIndexed)
                val entryName = if (index == 0) "base.apk" else sourceFile.name
                zos.putNextEntry(ZipEntry(entryName))
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(zos, bufferSize = 64 * 1024)
                }
                zos.closeEntry()
            }
        }
        bundleFile
    }

    /**
     * Installs a received split APK bundle or single APK directly using Android's atomic [PackageInstaller].
     */
    suspend fun installPackage(apkOrBundleFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            if (apkOrBundleFile.name.endsWith(".apks") || apkOrBundleFile.name.endsWith(".zip")) {
                // Multi-APK bundle extraction directly into the installer session
                ZipInputStream(FileInputStream(apkOrBundleFile)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                            session.openWrite(entry.name, 0, entry.size).use { out ->
                                zis.copyTo(out, bufferSize = 64 * 1024)
                                session.fsync(out)
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            } else {
                // Monolithic single APK
                session.openWrite("base.apk", 0, apkOrBundleFile.length()).use { out ->
                    FileInputStream(apkOrBundleFile).use { input ->
                        input.copyTo(out, bufferSize = 64 * 1024)
                        session.fsync(out)
                    }
                }
            }

            // Commit session
            val intent = Intent("org.zeroshare.app.INSTALL_COMPLETE")
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pendingIntent.intentSender)
            session.close()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
