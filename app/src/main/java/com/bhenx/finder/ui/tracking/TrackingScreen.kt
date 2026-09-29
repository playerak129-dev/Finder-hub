package com.bhenx.finder.ui.tracking

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhenx.finder.R
import com.bhenx.finder.bluetooth.BhenxGattClient
import com.bhenx.finder.bluetooth.BluetoothScanner
import com.bhenx.finder.bluetooth.RingCommandStatus
import com.bhenx.finder.bluetooth.RssiSmoother
import com.bhenx.finder.data.HistoryRepository
import com.bhenx.finder.model.DeviceInfo
import com.bhenx.finder.model.ProximityLevel
import com.bhenx.finder.ui.components.BhenxButton
import com.bhenx.finder.ui.components.BhenxOutlinedButton
import com.bhenx.finder.ui.components.BhenxTopBar
import com.bhenx.finder.ui.theme.CobaltBlue
import com.bhenx.finder.ui.theme.DarkSurfaceElevated
import com.bhenx.finder.ui.theme.ElectricBlue
import com.bhenx.finder.ui.theme.SkyBlue
import com.bhenx.finder.ui.theme.StatusAmber
import com.bhenx.finder.ui.theme.StatusGreen
import com.bhenx.finder.ui.theme.StatusRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun TrackingScreen(
    device: DeviceInfo,
    bluetoothScanner: BluetoothScanner,
    gattClient: BhenxGattClient,
    historyRepository: HistoryRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val sessionToken = remember { UUID.randomUUID().toString().take(8) }

    val smoother = remember {
        RssiSmoother(windowSize = 6).apply {
            device.rssi?.let { addSample(it) }
        }
    }

    var currentSmoothedRssi by remember { mutableStateOf(smoother.getSmoothed() ?: (device.rssi ?: -80)) }
    var currentProximity by remember { mutableStateOf(ProximityLevel.fromRssi(currentSmoothedRssi)) }
    var lastPacketTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSignalLost by remember { mutableStateOf(false) }

    var showStopDialog by remember { mutableStateOf(false) }
    var hasRungOnce by remember { mutableStateOf(false) }

    val commandStatus by gattClient.commandStatus.collectAsState()
    val statusMessage by gattClient.statusMessage.collectAsState()

    // Configuration de l'écoute RSSI temps réel sur le scanner
    LaunchedEffect(device.address) {
        bluetoothScanner.trackingAddress = device.address
        bluetoothScanner.onTrackingRssiUpdate = { rawRssi ->
            lastPacketTime = System.currentTimeMillis()
            isSignalLost = false
            val smoothed = smoother.addSample(rawRssi)
            currentSmoothedRssi = smoothed
            currentProximity = ProximityLevel.fromRssi(smoothed)
        }

        // Si le scan n'est pas déjà actif, le démarrer pour ce device
        bluetoothScanner.startScan(device.address)
    }

    // Détection de perte de signal (si aucun paquet reçu pendant plus de 7 secondes)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            if (System.currentTimeMillis() - lastPacketTime > 7000L) {
                isSignalLost = true
            }
        }
    }

    // Nettoyage complet à la sortie
    DisposableEffect(Unit) {
        onDispose {
            bluetoothScanner.trackingAddress = null
            bluetoothScanner.onTrackingRssiUpdate = null
            gattClient.disconnect()
        }
    }

    fun finishTracking(resultLabel: String) {
        coroutineScope.launch {
            historyRepository.addSearchEntry(
                deviceName = device.name,
                deviceAddress = device.address,
                result = resultLabel
            )
        }
        bluetoothScanner.stopScan()
        gattClient.resetState()
        onBackClick()
    }

    BackHandler {
        showStopDialog = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BhenxTopBar(
                title = "CHERCHE TON TÉLÉPHONE",
                onBackClick = { showStopDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Indicateur central animé : anneaux et satellites autour du téléphone
                DynamicProximityRadar(
                    proximityLevel = currentProximity,
                    isSignalLost = isSignalLost
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Nom de l'appareil sélectionné
                Text(
                    text = device.name.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 0.5.sp
                )

                if (device.bhenxId != null) {
                    Text(
                        text = "ID : ${device.bhenxId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Badge de statut de proximité
                if (isSignalLost) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StatusRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed)
                    ) {
                        Text(
                            text = "SIGNAL PERDU",
                            style = MaterialTheme.typography.labelLarge,
                            color = StatusRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Le téléphone n'est plus détecté. Rapproche-toi ou vérifie son Bluetooth.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    val statusColor = when (currentProximity) {
                        ProximityLevel.VERY_CLOSE -> StatusGreen
                        ProximityLevel.NEAR -> ElectricBlue
                        ProximityLevel.MEDIUM -> SkyBlue
                        ProximityLevel.FAR -> StatusAmber
                        ProximityLevel.VERY_WEAK -> StatusRed
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = currentProximity.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentProximity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Jauge de signal Bluetooth réel (pas de fausse distance)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Signal Bluetooth",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isSignalLost) "-- dBm" else "$currentSmoothedRssi dBm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSignalLost) StatusRed else ElectricBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val progress = if (isSignalLost) 0f else currentProximity.progressFraction
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (isSignalLost) StatusRed else ElectricBlue,
                            trackColor = MaterialTheme.colorScheme.background
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isSignalLost) StatusRed else StatusGreen,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSignalLost) "Recherche en cours du signal…" else "Recherche en temps réel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Message d'état de commande (ex: "SONNERIE ACTIVÉE", "Envoi...")
                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (commandStatus) {
                            RingCommandStatus.RINGING_CONFIRMED -> StatusGreen.copy(alpha = 0.15f)
                            RingCommandStatus.UNREACHABLE -> StatusRed.copy(alpha = 0.15f)
                            else -> SkyBlue.copy(alpha = 0.15f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (commandStatus) {
                                RingCommandStatus.RINGING_CONFIRMED -> StatusGreen
                                RingCommandStatus.UNREACHABLE -> StatusRed
                                else -> SkyBlue
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (commandStatus == RingCommandStatus.SENDING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SkyBlue
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (commandStatus) {
                                    RingCommandStatus.RINGING_CONFIRMED -> StatusGreen
                                    RingCommandStatus.UNREACHABLE -> StatusRed
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // Boutons d'action en bas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .padding(top = 20.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Bouton FAIRE SONNER / ARRÊTER LA SONNERIE
                if (commandStatus == RingCommandStatus.RINGING_CONFIRMED) {
                    BhenxButton(
                        text = "ARRÊTER LA SONNERIE",
                        onClick = {
                            gattClient.sendStopRing(device.address, device.bhenxId ?: "ANY", sessionToken)
                        },
                        icon = Icons.Default.VolumeOff,
                        containerColor = StatusAmber,
                        contentColor = Color.Black,
                        testTag = "stop_ring_button",
                        modifier = Modifier.height(54.dp)
                    )
                } else {
                    val isBusy = commandStatus == RingCommandStatus.SENDING || commandStatus == RingCommandStatus.CONNECTING
                    BhenxButton(
                        text = if (isBusy) "ENVOI DE LA COMMANDE…" else "🔊 FAIRE SONNER MON TÉLÉPHONE",
                        onClick = {
                            hasRungOnce = true
                            gattClient.sendRing(device.address, device.bhenxId ?: "ANY", sessionToken)
                        },
                        icon = if (isBusy) null else Icons.Default.VolumeUp,
                        enabled = !isSignalLost && !isBusy,
                        testTag = "ring_phone_button",
                        modifier = Modifier.height(54.dp)
                    )
                }

                // Bouton ARRÊTER LA RECHERCHE
                BhenxOutlinedButton(
                    text = "ARRÊTER LA RECHERCHE",
                    onClick = { showStopDialog = true },
                    icon = Icons.Default.Stop,
                    testTag = "stop_tracking_button"
                )

                // Mention obligatoire d'estimation
                Text(
                    text = "La proximité est estimée à partir du signal Bluetooth réel.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Boîte de dialogue de confirmation d'arrêt
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = {
                Text(
                    text = "Arrêter la recherche ?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Voulez-vous terminer cette session de recherche ?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopDialog = false
                        val resultLabel = when {
                            hasRungOnce -> "Trouvé (fait sonner)"
                            isSignalLost -> "Signal perdu"
                            else -> "Recherche arrêtée"
                        }
                        finishTracking(resultLabel)
                    }
                ) {
                    Text(text = "ARRÊTER", color = StatusRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text(text = "CONTINUER")
                }
            }
        )
    }
}

/**
 * Radar central dynamique dont la vitesse de clignotement / pulsation
 * dépend fidèlement de la force réelle du signal Bluetooth (RSSI).
 */
@Composable
private fun DynamicProximityRadar(
    proximityLevel: ProximityLevel,
    isSignalLost: Boolean,
    modifier: Modifier = Modifier
) {
    val duration = if (isSignalLost) 2000 else proximityLevel.pulseDurationMs

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val effectiveAlpha = if (isSignalLost) 0.15f else pulseAlpha
    val effectiveScale = if (isSignalLost) 1.0f else pulseScale

    val tintColor = if (isSignalLost) {
        StatusRed
    } else {
        when (proximityLevel) {
            ProximityLevel.VERY_CLOSE -> StatusGreen
            ProximityLevel.NEAR -> ElectricBlue
            ProximityLevel.MEDIUM -> SkyBlue
            ProximityLevel.FAR -> StatusAmber
            ProximityLevel.VERY_WEAK -> StatusRed
        }
    }

    Box(
        modifier = modifier
            .size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Grand cercle de pulsation externe
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(effectiveScale)
                .background(tintColor.copy(alpha = effectiveAlpha * 0.25f), CircleShape)
                .border(1.5.dp, tintColor.copy(alpha = effectiveAlpha), CircleShape)
        )

        // Cercle moyen intermédiaire
        Box(
            modifier = Modifier
                .size(125.dp)
                .background(tintColor.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, tintColor.copy(alpha = 0.4f), CircleShape)
        )

        // Satellites décoratifs
        SatelliteDots(tintColor = tintColor, alpha = if (isSignalLost) 0.2f else effectiveAlpha)

        // Cœur central : Icône du téléphone
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tintColor.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    ),
                    shape = CircleShape
                )
                .border(2.dp, tintColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = "Téléphone recherché",
                tint = if (isSignalLost) MaterialTheme.colorScheme.onSurfaceVariant else tintColor,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun SatelliteDots(tintColor: Color, alpha: Float) {
    // 6 points satellites disposés en cercle (aspect radar ◉)
    Box(modifier = Modifier.size(145.dp)) {
        // Haut
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(10.dp)
                .background(tintColor.copy(alpha = alpha), CircleShape)
        )
        // Bas
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(10.dp)
                .background(tintColor.copy(alpha = alpha), CircleShape)
        )
        // Gauche haut
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(8.dp)
                .background(tintColor.copy(alpha = alpha), CircleShape)
        )
        // Droite haut
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(8.dp)
                .background(tintColor.copy(alpha = alpha), CircleShape)
        )
    }
}
