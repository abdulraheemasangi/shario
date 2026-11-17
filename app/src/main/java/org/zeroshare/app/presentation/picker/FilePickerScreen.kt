package org.zeroshare.app.presentation.picker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zeroshare.app.R
import org.zeroshare.app.core.extensions.FormatExtensions
import org.zeroshare.app.domain.model.ShareableItem
import org.zeroshare.app.presentation.components.GlassCard
import org.zeroshare.app.presentation.components.ItemThumbnail
import org.zeroshare.app.presentation.components.LiquidGlassBackground
import org.zeroshare.app.presentation.theme.GlassBorderLight
import org.zeroshare.app.presentation.theme.GlassBorderUltraLight
import org.zeroshare.app.presentation.theme.IosBlue
import org.zeroshare.app.presentation.theme.IosCyan
import org.zeroshare.app.presentation.theme.IosGreen
import org.zeroshare.app.presentation.theme.IosIndigo
import org.zeroshare.app.presentation.theme.IosOrange

@Composable
fun FilePickerScreen(
    photos: List<ShareableItem>,
    videos: List<ShareableItem>,
    audio: List<ShareableItem>,
    documents: List<ShareableItem> = emptyList(),
    apps: List<ShareableItem>,
    hasPermission: Boolean = true,
    onRequestPermissions: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onProceedToSend: (List<ShareableItem>) -> Unit,
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedItems = remember { mutableStateMapOf<String, ShareableItem>() }

    val tabs = listOf(
        Pair(stringResource(R.string.category_photos), photos),
        Pair(stringResource(R.string.category_videos), videos),
        Pair(stringResource(R.string.category_audio), audio),
        Pair(stringResource(R.string.category_docs), documents),
        Pair(stringResource(R.string.category_apps), apps)
    )

    val currentList = tabs[selectedTabIndex].second
    val totalSelectedBytes = selectedItems.values.sumOf { it.size }
    val totalAllItems = photos.size + videos.size + audio.size + documents.size + apps.size

    val availableFolders = remember(currentList) {
        val folders = currentList.map { it.folderName }.filter { it.isNotBlank() }.distinct().sorted()
        if (folders.size > 1) listOf("All Folders") + folders else emptyList()
    }
    var selectedFolder by remember(selectedTabIndex) { mutableStateOf("All Folders") }

    val displayedList = remember(currentList, selectedFolder, availableFolders) {
        if (selectedFolder == "All Folders" || availableFolders.isEmpty()) currentList
        else currentList.filter { it.folderName == selectedFolder }
    }

    LiquidGlassBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Brush.verticalGradient(listOf(GlassBorderLight, GlassBorderUltraLight)), CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Content",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.3).sp
                            )
                        )
                        Text(
                            text = if (selectedItems.isEmpty()) "Tap files to add to queue" else "${selectedItems.size} selected • ${FormatExtensions.formatBytes(totalSelectedBytes)}",
                            fontSize = 12.sp,
                            color = if (selectedItems.isEmpty()) Color(0x99EBEBF5) else IosCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (displayedList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x18FFFFFF))
                                .clickable {
                                    val allSelected = displayedList.all { selectedItems.containsKey(it.id) }
                                    if (allSelected) {
                                        displayedList.forEach { selectedItems.remove(it.id) }
                                    } else {
                                        displayedList.forEach { selectedItems[it.id] = it }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            val allSelected = displayedList.all { selectedItems.containsKey(it.id) }
                            Text(
                                text = if (allSelected) "Deselect" else "Select All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosBlue
                            )
                        }
                    }
                }

                // Apple-style Frosted Segmented Category Switcher
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0x14FFFFFF),
                    borderWidth = 1.dp
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(tabs) { index, pair ->
                            val isSelected = selectedTabIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.linearGradient(listOf(IosBlue, IosIndigo))
                                        } else {
                                            SolidColor(Color.Transparent)
                                        }
                                    )
                                    .clickable {
                                        selectedTabIndex = index
                                        selectedFolder = "All Folders"
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${pair.first} (${pair.second.size})",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xBBEBEBF5)
                                )
                            }
                        }
                    }
                }

                // Folder Filter Chips Bar (Grouped by Folder / Album)
                if (availableFolders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableFolders) { folder ->
                            val isFolderSelected = folder == selectedFolder
                            val count = if (folder == "All Folders") currentList.size else currentList.count { it.folderName == folder }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isFolderSelected) Color(0x33007AFF) else Color(0x14FFFFFF)
                                    )
                                    .border(
                                        1.dp,
                                        if (isFolderSelected) IosCyan.copy(alpha = 0.8f) else Color(0x24FFFFFF),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedFolder = folder }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isFolderSelected) IosCyan else Color(0xCCEBEBF5),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$folder ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isFolderSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isFolderSelected) Color.White else Color(0xBBEBEBF5)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content View
                if (totalAllItems == 0 && hasPermission) {
                    // Loading State
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = IosCyan,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Loading files and folders...",
                                color = Color(0xCCEBEBF5),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (!hasPermission && totalAllItems == 0) {
                    PermissionRequiredCard(
                        onRequestPermissions = onRequestPermissions,
                        onOpenSettings = onOpenSettings
                    )
                } else if (displayedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0x44FFFFFF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedFolder == "All Folders") "No files found in this category" else "No files in $selectedFolder",
                                color = Color(0x88EBEBF5),
                                fontSize = 14.sp
                            )
                            if (selectedFolder != "All Folders") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x22FFFFFF))
                                        .clickable { selectedFolder = "All Folders" }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Show All Folders", color = IosCyan, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedList, key = { it.id }) { item ->
                            val isSelected = selectedItems.containsKey(item.id)
                            LiquidGlassFileRow(
                                item = item,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    if (isSelected) selectedItems.remove(item.id)
                                    else selectedItems[item.id] = item
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }

            // Floating Bottom Action Dock
            AnimatedVisibility(
                visible = selectedItems.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = Color(0x2E1C2136),
                    borderWidth = 1.2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedItems.size} items queued",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = FormatExtensions.formatBytes(totalSelectedBytes),
                                fontSize = 12.sp,
                                color = IosCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onProceedToSend(selectedItems.values.toList()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(listOf(IosBlue, IosIndigo)))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Send Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = Color(0x22FFFFFF),
            borderWidth = 1.2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(IosOrange.copy(alpha = 0.2f))
                        .border(1.dp, IosOrange.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Permission Required",
                        tint = IosOrange,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Storage Access Required",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Shario requires storage access to transfer photos, 4K videos, audio, and documents with 100% original quality without compression.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xBBEBEBF5),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(IosBlue, IosIndigo))),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = "Grant File Permission",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(Color(0x44FFFFFF), Color(0x18FFFFFF)))
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xDDEBEBF5),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open App Settings",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xDDEBEBF5)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassFileRow(
    item: ShareableItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    val rowShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(
                if (isSelected) Color(0x28007AFF) else Color(0x12FFFFFF)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (isSelected) {
                        listOf(IosBlue.copy(alpha = 0.8f), IosCyan.copy(alpha = 0.3f))
                    } else {
                        listOf(GlassBorderLight.copy(alpha = 0.3f), GlassBorderUltraLight)
                    }
                ),
                shape = rowShape
            )
            .clickable { onToggleSelect() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemThumbnail(item = item)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FormatExtensions.formatBytes(item.size),
                        fontSize = 12.sp,
                        color = Color(0xAAEBEBF5)
                    )

                    if (item.folderName.isNotBlank() && item.folderName != "Internal Storage") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.folderName,
                                fontSize = 10.sp,
                                color = Color(0xCCEBEBF5)
                            )
                        }
                    }

                    if (item.isAppBundle) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Split APK Bundle",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // iOS-style Circular Checkbox
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) IosBlue else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) IosBlue else Color(0x55FFFFFF),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
