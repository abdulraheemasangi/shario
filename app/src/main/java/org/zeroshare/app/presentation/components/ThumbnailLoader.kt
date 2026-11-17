package org.zeroshare.app.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewCozy
import androidx.compose.material.icons.filled.ZipFolder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zeroshare.app.domain.model.ItemCategory
import org.zeroshare.app.domain.model.ShareableItem
import org.zeroshare.app.presentation.theme.IosBlue
import org.zeroshare.app.presentation.theme.IosCyan
import org.zeroshare.app.presentation.theme.IosGreen
import org.zeroshare.app.presentation.theme.IosOrange
import org.zeroshare.app.presentation.theme.IosRed

object ThumbnailLoader {
    private val memoryCache = LruCache<String, ImageBitmap>(200)

    suspend fun loadThumbnail(context: Context, item: ShareableItem): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = item.id
        memoryCache.get(cacheKey)?.let { return@withContext it }

        val bitmap: Bitmap? = try {
            when (item.category) {
                ItemCategory.APP -> {
                    item.packageName?.let { pkg ->
                        val drawable = context.packageManager.getApplicationIcon(pkg)
                        drawableToBitmap(drawable)
                    }
                }
                ItemCategory.PHOTO, ItemCategory.VIDEO -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            context.contentResolver.loadThumbnail(item.uri, Size(128, 128), null)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        try {
                            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                                val options = BitmapFactory.Options().apply {
                                    inSampleSize = 4
                                }
                                BitmapFactory.decodeStream(stream, null, options)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }

        bitmap?.asImageBitmap()?.also { imageBitmap ->
            memoryCache.put(cacheKey, imageBitmap)
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

@Composable
fun ItemThumbnail(
    item: ShareableItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(item.id) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.id) {
        bitmap = ThumbnailLoader.loadThumbnail(context, item)
    }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(Color(0x18FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val (icon, tint) = getCategoryVectorIcon(item)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun getCategoryVectorIcon(item: ShareableItem): Pair<ImageVector, Color> {
    val nameLower = item.name.lowercase()
    return when {
        item.category == ItemCategory.PHOTO -> Pair(Icons.Default.Image, IosCyan)
        item.category == ItemCategory.VIDEO -> Pair(Icons.Default.VideoFile, IosBlue)
        item.category == ItemCategory.AUDIO -> Pair(Icons.Default.AudioFile, IosOrange)
        item.category == ItemCategory.APP -> Pair(Icons.Default.Android, IosGreen)
        nameLower.endsWith(".pdf") -> Pair(Icons.Default.PictureAsPdf, IosRed)
        nameLower.endsWith(".xls") || nameLower.endsWith(".xlsx") || nameLower.endsWith(".csv") -> Pair(Icons.Default.TableChart, IosGreen)
        nameLower.endsWith(".ppt") || nameLower.endsWith(".pptx") -> Pair(Icons.Default.ViewCozy, IosOrange)
        nameLower.endsWith(".zip") || nameLower.endsWith(".rar") || nameLower.endsWith(".7z") -> Pair(Icons.Default.Description, Color(0xFFD0BCFF))
        else -> Pair(Icons.Default.Description, Color(0xFFD0E2FF))
    }
}
