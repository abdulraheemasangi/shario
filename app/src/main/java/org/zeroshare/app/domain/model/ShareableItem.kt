package org.zeroshare.app.domain.model

import android.net.Uri

enum class ItemCategory {
    PHOTO,
    VIDEO,
    AUDIO,
    DOCUMENT,
    APP,
    GENERIC_FILE,
    FOLDER
}

/**
 * Domain representation of any item selected for transfer.
 * Preserves the original Content URI or absolute path without modification.
 */
data class ShareableItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val category: ItemCategory,
    val lastModified: Long = System.currentTimeMillis(),
    val relativePath: String? = null,
    val folderName: String = "Internal Storage",
    val packageName: String? = null, // Set for installed apps
    val isAppBundle: Boolean = false,
    val splitApkUris: List<Uri> = emptyList() // Base + Split APKs
)
