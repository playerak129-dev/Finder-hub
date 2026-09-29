package com.bhenx.finder.ui.preparation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhenx.finder.R
import com.bhenx.finder.bluetooth.BluetoothManager
import com.bhenx.finder.ui.components.BhenxButton
import com.bhenx.finder.ui.components.BhenxOutlinedButton
import com.bhenx.finder.ui.components.BhenxTopBar
import com.bhenx.finder.ui.theme.StatusAmber
import com.bhenx.finder.ui.theme.StatusGreen
import com.bhenx.finder.ui.theme.StatusRed
import com.bhenx.finder.util.PermissionUtils

@Composable
fun PreparationScreen(
    bluetoothManager: BluetoothManager,
    isBluetoothEnabled: Boolean,
    isLocationEnabled: Boolean,
    onBackClick: () -> Unit,
    onStartSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionsGranted by remember {
        mutableStateOf(PermissionUtils.areAllPermissionsGranted(context))
    }
    var permissionDeniedExplanation by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        permissionsGranted = allGranted
        bluetoothManager.refreshStatus()
        if (!allGranted) {
            permissionDeniedExplanation = true
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        bluetoothManager.refreshStatus()
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BhenxTopBar(
                title = stringResource(R.string.prep_title),
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.prep_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Condition Bluetooth
                ConditionCard(
                    title = stringResource(R.string.step_bluetooth_title),
                    description = stringResource(R.string.step_bluetooth_desc),
                    isActive = isBluetoothEnabled,
                    icon = Icons.Default.Bluetooth,
                    actionText = if (!isBluetoothEnabled) stringResource(R.string.btn_enable_bluetooth) else null,
                    onActionClick = {
                        try {
                            enableBluetoothLauncher.launch(bluetoothManager.createEnableBluetoothIntent())
                        } catch (_: Exception) {
                            try {
                                context.startActivity(bluetoothManager.createOpenBluetoothSettingsIntent())
                            } catch (_: Exception) {
                                // Géré sans crash
                            }
                        }
                    }
                )

                // 2. Condition Localisation
                ConditionCard(
                    title = stringResource(R.string.step_location_title),
                    description = stringResource(R.string.step_location_desc),
                    isActive = isLocationEnabled,
                    icon = Icons.Default.LocationOn,
                    actionText = if (!isLocationEnabled) stringResource(R.string.btn_open_location_settings) else null,
                    onActionClick = {
                        try {
                            context.startActivity(bluetoothManager.createOpenLocationSettingsIntent())
                        } catch (_: Exception) {
                            // Géré sans crash
                        }
                    }
                )

                // 3. Condition Proximité
                ConditionCard(
                    title = stringResource(R.string.step_proximity_title),
                    description = stringResource(R.string.step_proximity_desc),
                    isActive = true, // Consigne d'usage physique
                    icon = Icons.Default.NearMe,
                    actionText = null,
                    onActionClick = null
                )

                // Alerte ou demande de permission si non accordée
                if (!permissionsGranted) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = StatusAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Autorisation requise",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.permission_required_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (permissionDeniedExplanation) {
                                BhenxOutlinedButton(
                                    text = stringResource(R.string.btn_open_settings),
                                    onClick = {
                                        try {
                                            context.startActivity(bluetoothManager.createOpenAppSettingsIntent())
                                        } catch (_: Exception) {
                                            // Géré sans crash
                                        }
                                    }
                                )
                            } else {
                                BhenxButton(
                                    text = stringResource(R.string.btn_grant_permissions),
                                    onClick = {
                                        permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bouton [ COMMENCER LA RECHERCHE ]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .padding(top = 24.dp, bottom = 12.dp)
            ) {
                val canStart = isBluetoothEnabled && permissionsGranted
                BhenxButton(
                    text = stringResource(R.string.btn_start_search),
                    onClick = {
                        if (!permissionsGranted) {
                            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
                        } else if (!isBluetoothEnabled) {
                            try {
                                enableBluetoothLauncher.launch(bluetoothManager.createEnableBluetoothIntent())
                            } catch (_: Exception) {
                                context.startActivity(bluetoothManager.createOpenBluetoothSettingsIntent())
                            }
                        } else {
                            onStartSearchClick()
                        }
                    },
                    icon = Icons.Default.PlayArrow,
                    testTag = "start_search_button",
                    enabled = true,
                    modifier = Modifier.height(54.dp)
                )
            }
        }
    }
}

@Composable
private fun ConditionCard(
    title: String,
    description: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String?,
    onActionClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) StatusGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isActive) StatusGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Statut réel
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (isActive) StatusGreen else StatusRed).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isActive) "ACTIF" else "DÉSACTIVÉ",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) StatusGreen else StatusRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                BhenxOutlinedButton(
                    text = actionText,
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
