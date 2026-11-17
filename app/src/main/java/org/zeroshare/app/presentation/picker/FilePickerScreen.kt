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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zeroshare.app.R
import org.zeroshare.app.core.extensions.FormatExtensions
import org.zeroshare.app.domain.model.ItemCategory
import org.zeroshare.app.domain.model.ShareableItem
import org.zeroshare.app.presentation.components.GlassCard
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
        Pair(stringResource(R.string.category_apps), apps)
    )

    val currentList = tabs[selectedTabIndex].second
    val totalSelectedBytes = selectedItems.values.sumOf { it.size }
    val totalAllItems = photos.size + videos.size + audio.size + apps.size

    LiquidGlassBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp)
            ) {
                // Apple-style Glass Top Bar with Safe Inset Spacing
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

                    if (currentList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x18FFFFFF))
                                .clickable {
                                    val allSelected = currentList.all { selectedItems.containsKey(it.id) }
                                    if (allSelected) {
                                        currentList.forEach { selectedItems.remove(it.id) }
                                    } else {
                                        currentList.forEach { selectedItems[it.id] = it }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            val allSelected = currentList.all { selectedItems.containsKey(it.id) }
                            Text(
                                text = if (allSelected) "Deselect" else "Select All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosBlue
                            )
                        }
                    }
                }

                // Apple-style Frosted Segmented Pill Switcher
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
                                            Color.Transparent
                                        }
                                    )
                                    .clickable { selectedTabIndex = index }
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

                Spacer(modifier = Modifier.height(14.dp))

                // Content Area: File List OR Permission Required Liquid Glass Card
                if (!hasPermission || (currentList.isEmpty() && totalAllItems == 0)) {
                    // Problem 3 Fix: Re-request file access with direct actions
                    PermissionRequiredCard(
                        onRequestPermissions = onRequestPermissions,
                        onOpenSettings = onOpenSettings
                    )
                } else if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No items in this category",
                                color = Color(0x88EBEBF5),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentList, key = { it.id }) { item ->
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
                            Spacer(modifier = Modifier.height(100.dp)) // Extra space for floating dock
                        }
                    }
                }
            }

            // Floating Liquid Glass Action Dock at the bottom
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
                    text = "Shario requires media and storage access to transfer photos, 4K videos, audio, and APKs with 100% original quality without compression.",
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
            val icon: ImageVector = when (item.category) {
                ItemCategory.PHOTO -> Icons.Default.Image
                ItemCategory.VIDEO -> Icons.Default.VideoFile
                ItemCategory.AUDIO -> Icons.Default.AudioFile
                ItemCategory.APP -> Icons.Default.Android
                else -> Icons.Default.Description
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) IosBlue.copy(alpha = 0.25f) else Color(0x18FFFFFF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) IosCyan else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

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
