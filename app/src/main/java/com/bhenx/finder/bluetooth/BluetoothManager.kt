package com.bhenx.finder.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.bhenx.finder.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Couche de gestion de l'état système Bluetooth et de la localisation.
 * Assure la communication directe et réelle avec les APIs Android.
 */
class BluetoothManager(private val context: Context) {

    private val androidBluetoothManager: AndroidBluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager

    val bluetoothAdapter: BluetoothAdapter? = androidBluetoothManager?.adapter

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _isBluetoothAvailable = MutableStateFlow(bluetoothAdapter != null)
    val isBluetoothAvailable: StateFlow<Boolean> = _isBluetoothAvailable.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isLocationEnabled = MutableStateFlow(checkLocationEnabled())
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()

    private val _bluetoothState = MutableStateFlow(calculateCurrentState())
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    private var isReceiverRegistered = false

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED,
                LocationManager.PROVIDERS_CHANGED_ACTION,
                "android.location.MODE_CHANGED" -> {
                    refreshStatus()
                }
            }
        }
    }

    init {
        registerReceiver()
        refreshStatus()
    }

    fun registerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
                addAction("android.location.MODE_CHANGED")
            }
            try {
                context.registerReceiver(systemReceiver, filter)
                isReceiverRegistered = true
            } catch (_: Exception) {
                // Ignore registration issues on specific restricted environments
            }
        }
    }

    fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(systemReceiver)
                isReceiverRegistered = false
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    fun refreshStatus() {
        val available = bluetoothAdapter != null
        _isBluetoothAvailable.value = available

        val enabled = try {
            bluetoothAdapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
        _isBluetoothEnabled.value = enabled

        _isLocationEnabled.value = checkLocationEnabled()
        _bluetoothState.value = calculateCurrentState()
    }

    private fun checkLocationEnabled(): Boolean {
        if (locationManager == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                locationManager.isLocationEnabled
            } catch (_: Exception) {
                false
            }
        } else {
            try {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (_: Exception) {
                false
            }
        }
    }

    fun calculateCurrentState(): BluetoothState {
        if (bluetoothAdapter == null) {
            return BluetoothState.BLUETOOTH_UNAVAILABLE
        }

        val enabled = try {
            bluetoothAdapter.isEnabled
        } catch (_: SecurityException) {
            false
        }

        if (!enabled) {
            return BluetoothState.BLUETOOTH_DISABLED
        }

        val permissionsGranted = PermissionUtils.areAllPermissionsGranted(context)
        if (!permissionsGranted) {
            return BluetoothState.PERMISSION_REQUIRED
        }

        return BluetoothState.READY
    }

    /**
     * Intent pour demander l'activation de Bluetooth via la boîte de dialogue système standard.
     */
    fun createEnableBluetoothIntent(): Intent {
        return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Intent de secours ouvrant les paramètres Bluetooth du système.
     */
    fun createOpenBluetoothSettingsIntent(): Intent {
        return Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Intent ouvrant les paramètres de localisation du système.
     */
    fun createOpenLocationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Intent ouvrant la page des paramètres de l'application pour accorder les permissions.
     */
    fun createOpenAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
