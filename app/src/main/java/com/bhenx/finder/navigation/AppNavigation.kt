package com.bhenx.finder.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bhenx.finder.bluetooth.BhenxAdvertiser
import com.bhenx.finder.bluetooth.BhenxGattClient
import com.bhenx.finder.bluetooth.BhenxRingManager
import com.bhenx.finder.bluetooth.BluetoothManager
import com.bhenx.finder.bluetooth.BluetoothScanner
import com.bhenx.finder.data.HistoryRepository
import com.bhenx.finder.data.SettingsRepository
import com.bhenx.finder.model.DeviceInfo
import com.bhenx.finder.ui.components.BhenxRingAlertOverlay
import com.bhenx.finder.ui.history.HistoryScreen
import com.bhenx.finder.ui.home.HomeScreen
import com.bhenx.finder.ui.preparation.PreparationScreen
import com.bhenx.finder.ui.scanner.ScannerScreen
import com.bhenx.finder.ui.settings.SettingsScreen
import com.bhenx.finder.ui.target.TargetModeScreen
import com.bhenx.finder.ui.tracking.TrackingScreen

sealed class Screen {
    object Home : Screen()
    object Preparation : Screen()
    object Scanner : Screen()
    data class Tracking(val device: DeviceInfo) : Screen()
    object TargetMode : Screen()
    object History : Screen()
    object Settings : Screen()
}

@Composable
fun AppNavigation(
    bluetoothManager: BluetoothManager,
    bluetoothScanner: BluetoothScanner,
    gattClient: BhenxGattClient,
    ringManager: BhenxRingManager,
    bhenxAdvertiser: BhenxAdvertiser,
    historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
    isTargetModeActive: Boolean,
    onToggleTargetMode: (Boolean) -> Unit,
    deviceIdentityId: String,
    modifier: Modifier = Modifier
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Home

    val isBluetoothEnabled by bluetoothManager.isBluetoothEnabled.collectAsState()
    val isLocationEnabled by bluetoothManager.isLocationEnabled.collectAsState()
    val isRinging by ringManager.isRinging.collectAsState()
    val deviceName by settingsRepository.deviceName.collectAsState()

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        navigateBack()
    }

    // Alerte d'urgence visible immédiatement si ce téléphone reçoit une commande de sonnerie
    if (isRinging) {
        BhenxRingAlertOverlay(
            onStopClick = { ringManager.stopRinging() }
        )
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "screen_transition",
        modifier = modifier
    ) { screen ->
        when (screen) {
            is Screen.Home -> {
                HomeScreen(
                    isBluetoothEnabled = isBluetoothEnabled,
                    isTargetModeActive = isTargetModeActive,
                    onSearchClick = {
                        bluetoothManager.refreshStatus()
                        navigateTo(Screen.Preparation)
                    },
                    onTargetModeClick = {
                        navigateTo(Screen.TargetMode)
                    },
                    onHistoryClick = {
                        navigateTo(Screen.History)
                    },
                    onSettingsClick = {
                        navigateTo(Screen.Settings)
                    }
                )
            }
            is Screen.Preparation -> {
                PreparationScreen(
                    bluetoothManager = bluetoothManager,
                    isBluetoothEnabled = isBluetoothEnabled,
                    isLocationEnabled = isLocationEnabled,
                    onBackClick = { navigateBack() },
                    onStartSearchClick = {
                        navigateTo(Screen.Scanner)
                    }
                )
            }
            is Screen.Scanner -> {
                ScannerScreen(
                    bluetoothScanner = bluetoothScanner,
                    bluetoothManager = bluetoothManager,
                    onBackClick = { navigateBack() },
                    onDeviceSelected = { selectedDevice ->
                        navigateTo(Screen.Tracking(selectedDevice))
                    }
                )
            }
            is Screen.Tracking -> {
                TrackingScreen(
                    device = screen.device,
                    bluetoothScanner = bluetoothScanner,
                    gattClient = gattClient,
                    historyRepository = historyRepository,
                    onBackClick = { navigateBack() }
                )
            }
            is Screen.TargetMode -> {
                TargetModeScreen(
                    isTargetModeActive = isTargetModeActive,
                    onToggleTargetMode = onToggleTargetMode,
                    bhenxAdvertiser = bhenxAdvertiser,
                    ringManager = ringManager,
                    bhenxId = deviceIdentityId,
                    deviceName = deviceName,
                    onBackClick = { navigateBack() }
                )
            }
            is Screen.History -> {
                HistoryScreen(
                    historyRepository = historyRepository,
                    onBackClick = { navigateBack() }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    isTargetModeActive = isTargetModeActive,
                    onToggleTargetMode = onToggleTargetMode,
                    bhenxId = deviceIdentityId,
                    isRinging = isRinging,
                    onTestRing = {
                        if (ringManager.isRinging.value) {
                            ringManager.stopRinging()
                        } else {
                            ringManager.startRinging()
                        }
                    },
                    onBackClick = { navigateBack() }
                )
            }
        }
    }
}
