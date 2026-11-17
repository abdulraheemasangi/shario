package org.zeroshare.app.presentation.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zeroshare.app.R
import org.zeroshare.app.presentation.components.GlassCard
import org.zeroshare.app.presentation.components.LiquidGlassBackground
import org.zeroshare.app.presentation.theme.GlassBorderLight
import org.zeroshare.app.presentation.theme.GlassBorderUltraLight
import org.zeroshare.app.presentation.theme.IosBlue
import org.zeroshare.app.presentation.theme.IosCyan
import org.zeroshare.app.presentation.theme.IosGreen
import org.zeroshare.app.presentation.theme.IosIndigo
import org.zeroshare.app.presentation.theme.IosMint

@Composable
fun HomeScreen(
    onNavigateToSend: () -> Unit,
    onNavigateToReceive: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Apple-style Glass Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            brush = Brush.linearGradient(
                                listOf(Color.White, Color(0xFFD0E2FF))
                            )
                        )
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xCCEBEBF5),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Frosted Circular Glass History Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(listOf(GlassBorderLight, GlassBorderUltraLight)),
                            shape = CircleShape
                        )
                        .clickable { onNavigateToHistory() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Zero-Loss Trust Liquid Glass Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = Color(0x1AFFFFFF)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(IosGreen.copy(alpha = 0.2f))
                            .border(1.dp, IosGreen.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = IosMint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Bit-for-Bit Identical",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "No re-encoding • Original EXIF GPS • SHA-256 Verified",
                            fontSize = 12.sp,
                            color = Color(0xAAEBEBF5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Dual Liquid Glass Action Cards (Send & Receive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LiquidGlassActionCard(
                    title = stringResource(R.string.action_send),
                    subtitle = "100% Original Quality",
                    icon = Icons.Default.Send,
                    gradientColors = listOf(IosBlue, IosIndigo),
                    accentColor = IosCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSend
                )

                LiquidGlassActionCard(
                    title = stringResource(R.string.action_receive),
                    subtitle = "Accept Files via QR",
                    icon = Icons.Default.FileDownload,
                    gradientColors = listOf(IosMint, IosGreen),
                    accentColor = Color(0xFF64D2FF),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToReceive
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Liquid Glass Feature Badges with SVGs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassMiniBadge(icon = Icons.Default.Bolt, label = "Up to 50 MB/s", modifier = Modifier.weight(1f))
                GlassMiniBadge(icon = Icons.Default.Lock, label = "AES-256-GCM", modifier = Modifier.weight(1f))
                GlassMiniBadge(icon = Icons.Default.WifiOff, label = "No Internet", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Liquid Glass Brand Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Shario — Fast Sharing. Zero Hassle.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0x88FFFFFF),
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .height(210.dp)
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        gradientColors[0].copy(alpha = 0.85f),
                        gradientColors[1].copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shape = cardShape
            )
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Glass bubble for icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GlassMiniBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IosCyan,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xDDEBEBF5)
            )
        }
    }
}
