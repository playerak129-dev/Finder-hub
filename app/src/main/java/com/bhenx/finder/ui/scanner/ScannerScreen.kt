package com.bhenx.finder.ui.scanner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhenx.finder.R
import com.bhenx.finder.bluetooth.BluetoothManager
import com.bhenx.finder.bluetooth.BluetoothScanner
import com.bhenx.finder.model.DeviceInfo
import com.bhenx.finder.ui.components.BhenxButton
import com.bhenx.finder.ui.components.BhenxTopBar
import com.bhenx.finder.ui.components.StatusBadge
import com.bhenx.finder.ui.theme.CobaltBlue
import com.bhenx.finder.ui.theme.ElectricBlue
import com.bhenx.finder.ui.theme.SkyBlue
import com.bhenx.finder.ui.theme.StatusAmber
import com.bhenx.finder.ui.theme.StatusRed
import com.bhenx.finder.util.PermissionUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannerScreen(
    bluetoothScanner: BluetoothScanner,
    bluetoothManager: BluetoothManager,
    onBackClick: () -> Unit,
    onDeviceSelected: (DeviceInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isScanning by bluetoothScanner.isScanning.collectAsState()
    val devices by bluetoothScanner.devices.collectAsState()
    val errorMessage by bluetoothScanner.errorMessage.collectAsState()

    val isBluetoothEnabled by bluetoothManager.isBluetoothEnabled.collectAsState()
    val isLocationEnabled by bluetoothManager.isLocationEnabled.collectAsState()
    val isPermissionGranted = PermissionUtils.areAllPermissionsGranted(context)

    // Démarrage du scan réel à l'entrée
    LaunchedEffect(Unit) {
        bluetoothManager.refreshStatus()
        bluetoothScanner.startScan()
    }

    // Arrêt propre du scan à la sortie
    DisposableEffect(Unit) {
        onDispose {
            bluetoothScanner.stopScan()
        }
    }

    BackHandler {
        bluetoothScanner.stopScan()
        onBackClick()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BhenxTopBar(
                title = stringResource(R.string.scanner_title),
                onBackClick = {
                    bluetoothScanner.stopScan()
                    onBackClick()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sous-titre
                Text(
                    text = stringResource(R.string.scanner_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Barre d'indicateurs système réels
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(
                        label = stringResource(R.string.scanner_bluetooth_label),
                        isActive = isBluetoothEnabled,
                        activeText = stringResource(R.string.scanner_active),
                        inactiveText = stringResource(R.string.scanner_inactive)
                    )
                    StatusBadge(
                        label = stringResource(R.string.scanner_location_label),
                        isActive = isLocationEnabled,
                        activeText = stringResource(R.string.scanner_active),
                        inactiveText = stringResource(R.string.scanner_inactive)
                    )
                    StatusBadge(
                        label = stringResource(R.string.scanner_permission_label),
                        isActive = isPermissionGranted,
                        activeText = stringResource(R.string.scanner_granted),
                        inactiveText = stringResource(R.string.scanner_required)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Indicateur de scan réel
                if (isScanning) {
                    ScanningPulseIndicator()
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Erreur éventuelle
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusRed,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Notice Phase 1 honnête
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SkyBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.scanner_phase1_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Liste des appareils ou état vide réel
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.scanner_no_devices),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.scanner_no_devices_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Appareils détectés (appuie pour suivre) :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(devices, key = { it.address }) { device ->
                            DeviceItem(
                                device = device,
                                onClick = { onDeviceSelected(device) }
                            )
                        }
                    }
                }
            }

            // Bouton de contrôle en bas : ARRÊTER ou RELANCER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(vertical = 12.dp)
            ) {
                if (isScanning) {
                    BhenxButton(
                        text = stringResource(R.string.btn_stop_search),
                        onClick = {
                            bluetoothScanner.stopScan()
                        },
                        icon = Icons.Default.Stop,
                        containerColor = StatusRed,
                        contentColor = Color.White,
                        testTag = "stop_search_button",
                        modifier = Modifier.height(52.dp)
                    )
                } else {
                    BhenxButton(
                        text = stringResource(R.string.btn_restart_search),
                        onClick = {
                            bluetoothScanner.startScan()
                        },
                        icon = Icons.Default.Refresh,
                        containerColor = ElectricBlue,
                        testTag = "restart_search_button",
                        modifier = Modifier.height(52.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanningPulseIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = ElectricBlue,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ElectricBlue.copy(alpha = progress), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.scanner_scanning_active),
                style = MaterialTheme.typography.labelSmall,
                color = ElectricBlue.copy(alpha = progress)
            )
        }
    }
}
