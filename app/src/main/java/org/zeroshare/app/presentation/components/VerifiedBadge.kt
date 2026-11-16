package org.zeroshare.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zeroshare.app.R
import org.zeroshare.app.presentation.theme.ErrorRed
import org.zeroshare.app.presentation.theme.SuccessGreen

@Composable
fun VerifiedBadge(
    isVerified: Boolean,
    isFailed: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isVerified) {
        Box(
            modifier = modifier
                .background(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.badge_verified),
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else if (isFailed) {
        Box(
            modifier = modifier
                .background(
                    color = ErrorRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = ErrorRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.status_failed),
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
