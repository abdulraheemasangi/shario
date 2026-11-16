package org.zeroshare.app.presentation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zeroshare.app.R
import org.zeroshare.app.core.extensions.FormatExtensions
import org.zeroshare.app.domain.model.ItemCategory
import org.zeroshare.app.domain.model.ShareableItem
import org.zeroshare.app.presentation.theme.PrimaryBlue
import org.zeroshare.app.presentation.theme.SuccessGreen

@Composable
fun FilePickerScreen(
    photos: List<ShareableItem>,
    videos: List<ShareableItem>,
    audio: List<ShareableItem>,
    apps: List<ShareableItem>,
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

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, pair ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text("${pair.first} (${pair.second.size})") }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(currentList, key = { it.id }) { item ->
                val isSelected = selectedItems.containsKey(item.id)
                FileItemRow(
                    item = item,
                    isSelected = isSelected,
                    onToggleSelect = {
                        if (isSelected) selectedItems.remove(item.id)
                        else selectedItems[item.id] = item
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom Bar when items are selected
        if (selectedItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedItems.size} items selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = FormatExtensions.formatBytes(totalSelectedBytes),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onProceedToSend(selectedItems.values.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Send (${selectedItems.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    item: ShareableItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .clickable { onToggleSelect() }
            .padding(12.dp),
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
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = FormatExtensions.formatBytes(item.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.isAppBundle) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Split APK Bundle",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Checkbox Indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) PrimaryBlue else Color.Transparent)
                .padding(2.dp),
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
