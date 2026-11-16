package org.zeroshare.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zeroshare.app.core.extensions.FormatExtensions
import org.zeroshare.app.presentation.theme.PrimaryBlue
import org.zeroshare.app.presentation.theme.SuccessGreen

@Composable
fun SpeedometerView(
    bytesTransferred: Long,
    totalBytes: Long,
    speedBytesPerSec: Long,
    etaFormatted: String,
    modifier: Modifier = Modifier
) {
    val progress = if (totalBytes > 0) {
        (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "ProgressAnimation")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
                    text = FormatExtensions.formatSpeed(speedBytesPerSec),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlue,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "Transfer Speed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = etaFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "Remaining Time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = PrimaryBlue,
            trackColor = MaterialTheme.colorScheme.surface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${FormatExtensions.formatBytes(bytesTransferred)} / ${FormatExtensions.formatBytes(totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
