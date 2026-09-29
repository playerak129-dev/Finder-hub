package com.bhenx.finder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bhenx.finder.navigation.AppNavigation
import com.bhenx.finder.ui.theme.BhenxFinderTheme

class MainActivity : ComponentActivity() {

    private val app by lazy { application as BhenxFinderApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreference by app.settingsRepository.theme.collectAsState()
            val isTargetModeActive by app.isTargetModeActive.collectAsState()
            val identity = app.getDeviceIdentity()

            BhenxFinderTheme(themePreference = themePreference) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        bluetoothManager = app.bluetoothManager,
                        bluetoothScanner = app.bluetoothScanner,
                        gattClient = app.bhenxGattClient,
                        ringManager = app.bhenxRingManager,
                        bhenxAdvertiser = app.bhenxAdvertiser,
                        historyRepository = app.historyRepository,
                        settingsRepository = app.settingsRepository,
                        isTargetModeActive = isTargetModeActive,
                        onToggleTargetMode = { active -> app.setTargetMode(active) },
                        deviceIdentityId = identity.id
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualisation automatique de l'état système au retour de l'utilisateur
        app.bluetoothManager.refreshStatus()
    }
}
