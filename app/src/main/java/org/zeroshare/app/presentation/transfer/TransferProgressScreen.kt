package org.zeroshare.app.presentation.transfer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
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
import org.zeroshare.app.presentation.components.SpeedometerView
import org.zeroshare.app.presentation.components.VerifiedBadge
import org.zeroshare.app.presentation.theme.ErrorRed
import org.zeroshare.app.presentation.theme.PrimaryBlue
import org.zeroshare.app.presentation.theme.SuccessGreen

@Composable
fun TransferProgressScreen(
    state: TransferSessionState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (state.isSender) "Sending Files" else "Receiving Files",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Device: ${state.remoteDeviceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.status == TransferStatus.COMPLETED) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuccessGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "100% COMPLETE",
                        color = SuccessGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speedometer View (live speed, total progress, ETA)
        SpeedometerView(
            bytesTransferred = state.bytesTransferred,
            totalBytes = state.totalBytes,
            speedBytesPerSec = state.speedBytesPerSec,
            etaFormatted = state.etaFormatted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Controls: Pause / Resume / Cancel
        if (state.status != TransferStatus.COMPLETED && state.status != TransferStatus.FAILED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.status == TransferStatus.PAUSED) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_resume))
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_pause))
                    }
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_cancel))
                }
            }
        } else {
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Done", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Files (${state.fileStates.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // File Queue List with Live Verification Badges
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.fileStates, key = { it.fileId }) { fileState ->
                FileTransferCard(fileState = fileState)
            }
        }
    }
}

@Composable
private fun FileTransferCard(fileState: FileTransferItemState) {
    val progress = if (fileState.size > 0) {
        (fileState.bytesTransferred.toFloat() / fileState.size.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fileState.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            VerifiedBadge(
                isVerified = fileState.isVerified,
                isFailed = fileState.isFailed
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (fileState.isVerified) SuccessGreen else PrimaryBlue,
            trackColor = MaterialTheme.colorScheme.surface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${FormatExtensions.formatBytes(fileState.bytesTransferred)} / ${FormatExtensions.formatBytes(fileState.size)}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Chunk ${fileState.currentChunkIndex} / ${fileState.totalChunks}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (fileState.sha256Actual != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SHA-256: ${fileState.sha256Actual.take(16)}…",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (fileState.isVerified) SuccessGreen else ErrorRed
            )
        }
    }
}
