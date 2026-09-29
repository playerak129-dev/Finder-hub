package com.bhenx.finder.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhenx.finder.R
import com.bhenx.finder.model.DeviceInfo
import com.bhenx.finder.ui.theme.ElectricBlue
import com.bhenx.finder.ui.theme.SkyBlue
import com.bhenx.finder.ui.theme.StatusGreen

/**
 * Composant réutilisable pour afficher un appareil détecté.
 * Met en évidence les téléphones BHENX FINDER avec badge et niveau de proximité.
 */
@Composable
fun DeviceItem(
    device: DeviceInfo,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isBhenx = device.isBhenxDevice
    val borderColor = if (isBhenx) ElectricBlue else MaterialTheme.colorScheme.outline
    val surfaceColor = if (isBhenx) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = androidx.compose.foundation.BorderStroke(if (isBhenx) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .testTag("device_item_${device.address}")
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isBhenx) ElectricBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = if (isBhenx) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name.ifBlank { stringResource(R.string.device_unknown) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                if (isBhenx) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricBlue.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "BHENX FINDER",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (device.proximityLevel != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•  ${device.proximityLevel.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (device.rssi != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "${device.rssi} dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBhenx) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (onClick != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Sélectionner",
                        tint = if (isBhenx) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
