package com.bhenx.finder.ui.target

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhenx.finder.bluetooth.BhenxAdvertiser
import com.bhenx.finder.bluetooth.BhenxRingManager
import com.bhenx.finder.ui.components.BhenxButton
import com.bhenx.finder.ui.components.BhenxTopBar
import com.bhenx.finder.ui.theme.ElectricBlue
import com.bhenx.finder.ui.theme.SkyBlue
import com.bhenx.finder.ui.theme.StatusAmber
import com.bhenx.finder.ui.theme.StatusGreen
import com.bhenx.finder.ui.theme.StatusInactive
import com.bhenx.finder.ui.theme.StatusRed
import com.bhenx.finder.util.PermissionUtils

@Composable
fun TargetModeScreen(
    isTargetModeActive: Boolean,
    onToggleTargetMode: (Boolean) -> Unit,
    bhenxAdvertiser: BhenxAdvertiser,
    ringManager: BhenxRingManager,
    bhenxId: String,
    deviceName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHardwareSupported = bhenxAdvertiser.isHardwareSupported()

    val advertisePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            onToggleTargetMode(true)
        }
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BhenxTopBar(
                title = "Téléphone à retrouver",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Icône d'onde / balise
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (isTargetModeActive) StatusGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            if (isTargetModeActive) StatusGreen else MaterialTheme.colorScheme.outline,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Podcasts,
                        contentDescription = null,
                        tint = if (isTargetModeActive) StatusGreen else ElectricBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Titre et Explication
                Text(
                    text = "Téléphone à retrouver",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Activez ce mode pour permettre à un autre téléphone BHENX FINDER de retrouver cet appareil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Si le matériel ne supporte pas l'émission BLE
                if (!isHardwareSupported) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StatusAmber.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ce téléphone ne prend pas en charge la détection BHENX nécessaire pour ce mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Carte Principale de Contrôle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isTargetModeActive) StatusGreen else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isTargetModeActive) "ACTIVÉ" else "DÉSACTIVÉ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTargetModeActive) StatusGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (isTargetModeActive) StatusGreen else StatusInactive,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTargetModeActive) "Disponible pour être retrouvé" else "Téléphone non détectable par BHENX FINDER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isTargetModeActive) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isTargetModeActive,
                                enabled = isHardwareSupported,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (PermissionUtils.hasBluetoothAdvertisePermission(context)) {
                                            onToggleTargetMode(true)
                                        } else {
                                            advertisePermissionLauncher.launch(PermissionUtils.getAdvertisePermissions())
                                        }
                                    } else {
                                        onToggleTargetMode(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = StatusGreen
                                ),
                                modifier = Modifier.testTag("target_mode_screen_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Identité locale
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Nom : $deviceName",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Identifiant : $bhenxId",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkyBlue
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        if (ringManager.isRinging.value) {
                                            ringManager.stopRinging()
                                        } else {
                                            ringManager.startRinging()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (ringManager.isRinging.value) "Arrêter" else "Tester son")
                                }
                            }
                        }
                    }
                }

                // Information sur la consommation de batterie
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = SkyBlue,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Le mode téléphone à retrouver utilise Bluetooth en arrière-plan et peut augmenter légèrement la consommation de batterie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
