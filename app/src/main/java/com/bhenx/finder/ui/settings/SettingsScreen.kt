package com.bhenx.finder.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhenx.finder.R
import com.bhenx.finder.data.LanguagePreference
import com.bhenx.finder.data.SettingsRepository
import com.bhenx.finder.data.ThemePreference
import com.bhenx.finder.ui.components.BhenxTopBar
import com.bhenx.finder.ui.theme.ElectricBlue

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.platform.LocalContext
import com.bhenx.finder.ui.components.BhenxButton
import com.bhenx.finder.ui.theme.CobaltBlue
import com.bhenx.finder.ui.theme.SkyBlue
import com.bhenx.finder.ui.theme.StatusGreen
import com.bhenx.finder.ui.theme.StatusInactive
import com.bhenx.finder.ui.theme.StatusRed
import com.bhenx.finder.util.PermissionUtils

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    isTargetModeActive: Boolean,
    onToggleTargetMode: (Boolean) -> Unit,
    bhenxId: String,
    isRinging: Boolean,
    onTestRing: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceName by settingsRepository.deviceName.collectAsState()
    val themePreference by settingsRepository.theme.collectAsState()
    val languagePreference by settingsRepository.language.collectAsState()
    val notificationsEnabled by settingsRepository.notificationsEnabled.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember(deviceName) { mutableStateOf(deviceName) }

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
                title = stringResource(R.string.settings_title),
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // SECTION : TÉLÉPHONE À RETROUVER (MODE CIBLE)
                SettingsSection(title = "TÉLÉPHONE À RETROUVER") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTargetModeActive) StatusGreen else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Podcasts,
                                    contentDescription = null,
                                    tint = if (isTargetModeActive) StatusGreen else ElectricBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Activer la détection BHENX",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
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
                                            text = if (isTargetModeActive) "Détection active" else "Détection inactive",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTargetModeActive) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isTargetModeActive,
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
                                    modifier = Modifier.testTag("target_mode_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Permet à ce téléphone d'être détectable et localisable par un autre téléphone BHENX à proximité. Le Bluetooth doit rester actif.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Identifiant : $bhenxId",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SkyBlue
                                )

                                TextButton(
                                    onClick = onTestRing,
                                    modifier = Modifier.testTag("test_ring_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (isRinging) "Arrêter test" else "Tester sonnerie")
                                }
                            }
                        }
                    }
                }

                // SECTION 1 : MON APPAREIL
                SettingsSection(title = stringResource(R.string.settings_section_device)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempName = deviceName
                                showEditNameDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_device_name_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = deviceName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = {
                                    tempName = deviceName
                                    showEditNameDialog = true
                                },
                                modifier = Modifier.testTag("edit_device_name_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifier",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // SECTION 2 : APPARENCE
                SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeChoiceChip(
                                    label = stringResource(R.string.settings_theme_dark),
                                    icon = Icons.Default.DarkMode,
                                    isSelected = themePreference == ThemePreference.DARK,
                                    onClick = { settingsRepository.setTheme(ThemePreference.DARK) },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeChoiceChip(
                                    label = stringResource(R.string.settings_theme_light),
                                    icon = Icons.Default.LightMode,
                                    isSelected = themePreference == ThemePreference.LIGHT,
                                    onClick = { settingsRepository.setTheme(ThemePreference.LIGHT) },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeChoiceChip(
                                    label = stringResource(R.string.settings_theme_system),
                                    icon = Icons.Default.SettingsBrightness,
                                    isSelected = themePreference == ThemePreference.SYSTEM,
                                    onClick = { settingsRepository.setTheme(ThemePreference.SYSTEM) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // SECTION 3 : LANGUE
                SettingsSection(title = stringResource(R.string.settings_section_language)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LanguageChoiceChip(
                                label = stringResource(R.string.settings_lang_fr),
                                isSelected = languagePreference == LanguagePreference.FRENCH,
                                onClick = { settingsRepository.setLanguage(LanguagePreference.FRENCH) },
                                modifier = Modifier.weight(1f)
                            )
                            LanguageChoiceChip(
                                label = stringResource(R.string.settings_lang_en),
                                isSelected = languagePreference == LanguagePreference.ENGLISH,
                                onClick = { settingsRepository.setLanguage(LanguagePreference.ENGLISH) },
                                modifier = Modifier.weight(1f)
                            )
                            LanguageChoiceChip(
                                label = stringResource(R.string.settings_lang_ht),
                                isSelected = languagePreference == LanguagePreference.CREOLE,
                                onClick = { settingsRepository.setLanguage(LanguagePreference.CREOLE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // SECTION 4 : NOTIFICATIONS
                SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_notifications_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.settings_notifications_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { settingsRepository.setNotificationsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = ElectricBlue
                                ),
                                modifier = Modifier.testTag("notifications_switch")
                            )
                        }
                    }
                }

                // SECTION 5 : À PROPOS
                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_app_name_version),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_version),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.settings_about_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialogue d'édition du nom
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_device_name_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsRepository.setDeviceName(tempName)
                        showEditNameDialog = false
                    },
                    modifier = Modifier.testTag("save_device_name_button")
                ) {
                    Text(text = stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(text = stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun ThemeChoiceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ElectricBlue,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}

@Composable
private fun LanguageChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ElectricBlue,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}
