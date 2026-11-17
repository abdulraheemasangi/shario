package org.zeroshare.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zeroshare.app.core.extensions.FormatExtensions
import org.zeroshare.app.presentation.theme.GlassBorderLight
import org.zeroshare.app.presentation.theme.GlassBorderUltraLight
import org.zeroshare.app.presentation.theme.IosBlue
import org.zeroshare.app.presentation.theme.IosCyan
import org.zeroshare.app.presentation.theme.IosMint

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
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x1EFFFFFF))
            .border(1.dp, Brush.verticalGradient(listOf(GlassBorderLight, GlassBorderUltraLight)), shape)
            .padding(20.dp)
    ) {
        Column {
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
                            color = IosCyan,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Transfer Speed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0x99EBEBF5)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = etaFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = IosMint,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0x99EBEBF5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // iOS Frosted Glass Progress Indicator
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = IosBlue,
                trackColor = Color(0x1FFFFFFF)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${FormatExtensions.formatBytes(bytesTransferred)} of ${FormatExtensions.formatBytes(totalBytes)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xAAEBEBF5)
                )

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
