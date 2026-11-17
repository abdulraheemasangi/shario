package org.zeroshare.app.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zeroshare.app.R
import org.zeroshare.app.core.extensions.FormatExtensions
import org.zeroshare.app.domain.model.FileTransferItemState
import org.zeroshare.app.domain.model.TransferSessionState
import org.zeroshare.app.domain.model.TransferStatus
import org.zeroshare.app.presentation.components.LiquidGlassBackground
import org.zeroshare.app.presentation.components.SpeedometerView
import org.zeroshare.app.presentation.components.VerifiedBadge
import org.zeroshare.app.presentation.theme.GlassBorderLight
import org.zeroshare.app.presentation.theme.GlassBorderUltraLight
import org.zeroshare.app.presentation.theme.IosBlue
import org.zeroshare.app.presentation.theme.IosCyan
import org.zeroshare.app.presentation.theme.IosGreen
import org.zeroshare.app.presentation.theme.IosIndigo
import org.zeroshare.app.presentation.theme.IosRed

@Composable
fun TransferProgressScreen(
    state: TransferSessionState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit
) {
    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (state.isSender) "Streaming to Peer" else "Receiving Original Files",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.3).sp
                        )
                    )
                    Text(
                        text = "Device: ${state.remoteDeviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xAAEBEBF5)
                    )
                }

                if (state.status == TransferStatus.COMPLETED) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(IosGreen.copy(alpha = 0.2f))
                            .border(1.dp, IosGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = IosGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COMPLETED",
                                color = IosGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Speedometer View (live speed, progress, ETA)
            SpeedometerView(
                bytesTransferred = state.bytesTransferred,
                totalBytes = state.totalBytes,
                speedBytesPerSec = state.speedBytesPerSec,
                etaFormatted = state.etaFormatted
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Controls: Pause / Resume / Cancel or Done
            if (state.status != TransferStatus.COMPLETED && state.status != TransferStatus.FAILED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.status == TransferStatus.PAUSED) {
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(IosGreen, Color(0xFF30D158)))),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_resume), fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPause,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(Color(0x44FFFFFF), Color(0x18FFFFFF)))
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_pause), fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(IosRed.copy(alpha = 0.25f))
                            .border(1.dp, IosRed.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = IosRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.Bold, color = IosRed)
                    }
                }
            } else {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(IosBlue, IosIndigo))),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(text = "View Transfer History", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Queue (${state.fileStates.size} files)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // File Queue List with Live Verification Badges
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.fileStates, key = { it.fileId }) { fileState ->
                    LiquidGlassFileTransferCard(fileState = fileState)
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassFileTransferCard(fileState: FileTransferItemState) {
    val progress = if (fileState.size > 0) {
        (fileState.bytesTransferred.toFloat() / fileState.size.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0x16FFFFFF))
            .border(1.dp, Brush.verticalGradient(listOf(GlassBorderLight.copy(alpha = 0.25f), GlassBorderUltraLight)), cardShape)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fileState.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                VerifiedBadge(
                    isVerified = fileState.isVerified,
                    isFailed = fileState.isFailed
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (fileState.isVerified) IosGreen else IosBlue,
                trackColor = Color(0x1AFFFFFF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${FormatExtensions.formatBytes(fileState.bytesTransferred)} / ${FormatExtensions.formatBytes(fileState.size)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xAAEBEBF5)
                )

                Text(
                    text = "Chunk ${fileState.currentChunkIndex} / ${fileState.totalChunks}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xAAEBEBF5)
                )
            }

            if (fileState.sha256Actual != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SHA-256: ${fileState.sha256Actual.take(16)}…",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (fileState.isVerified) IosGreen else IosRed
                )
            }
        }
    }
}
