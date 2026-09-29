package com.bhenx.finder.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.bhenx.finder.model.DeviceInfo
import com.bhenx.finder.model.ProximityLevel
import com.bhenx.finder.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Couche de scan Bluetooth / BLE réelle.
 * RÈGLE FONDAMENTALE : Aucun faux appareil, aucune fausse distance,
 * aucun faux signal RSSI n'est généré.
 */
class BluetoothScanner(
    private val context: Context,
    private val bluetoothManager: BluetoothManager
) {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices.asStateFlow()

    private val _state = MutableStateFlow(BluetoothState.READY)
    val state: StateFlow<BluetoothState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var leScanner: BluetoothLeScanner? = null
    private var isClassicReceiverRegistered = false

    private val discoveredAddresses = mutableSetOf<String>()

    // Suivi ciblé d'un appareil sélectionné en temps réel
    var trackingAddress: String? = null
    var onTrackingRssiUpdate: ((rssi: Int) -> Unit)? = null

    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { handleScanResult(it) }
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _state.value = BluetoothState.ERROR
            _errorMessage.value = "Le scan BLE a échoué (code $errorCode)"
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val address = device.address ?: return

        // Si cet appareil est actuellement suivi en temps réel dans l'écran de recherche
        if (trackingAddress != null && address.equals(trackingAddress, ignoreCase = true)) {
            onTrackingRssiUpdate?.invoke(result.rssi)
        }

        // Décodage des annonces BHENX FINDER
        val bhenxData = result.scanRecord?.getServiceData(BhenxBleProtocol.PARCEL_SERVICE_UUID)
        val decoded = BhenxBleProtocol.decodeAdvData(bhenxData)
        val hasBhenxUuid = result.scanRecord?.serviceUuids?.contains(BhenxBleProtocol.PARCEL_SERVICE_UUID) == true
        val isBhenx = decoded != null || hasBhenxUuid

        val bhenxId = decoded?.first
        val rawName = decoded?.second ?: result.scanRecord?.deviceName ?: device.name

        addDiscoveredDevice(
            name = rawName,
            address = address,
            rssi = result.rssi,
            isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
            type = if (isBhenx) "BHENX FINDER" else "BLE",
            isBhenx = isBhenx,
            bhenxId = bhenxId
        )
    }

    private val classicReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val rssi: Short =
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                    if (device != null) {
                        val rssiVal = if (rssi != Short.MIN_VALUE) rssi.toInt() else null
                        addDiscoveredDevice(
                            name = device.name,
                            address = device.address,
                            rssi = rssiVal,
                            isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
                            type = "Bluetooth",
                            isBhenx = false,
                            bhenxId = null
                        )
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (_isScanning.value) {
                        _isScanning.value = false
                        _state.value = BluetoothState.SCAN_FINISHED
                    }
                }
            }
        }
    }

    @Synchronized
    private fun addDiscoveredDevice(
        name: String?,
        address: String?,
        rssi: Int?,
        isBonded: Boolean,
        type: String,
        isBhenx: Boolean,
        bhenxId: String?
    ) {
        val safeAddress = address ?: return
        val safeName = if (!name.isNullOrBlank()) name else if (isBhenx) "Téléphone BHENX" else "Appareil Bluetooth"

        val updatedList = _devices.value.toMutableList()
        val existingIndex = updatedList.indexOfFirst { it.address.equals(safeAddress, ignoreCase = true) }

        val newInfo = DeviceInfo(
            name = safeName,
            address = safeAddress,
            rssi = rssi,
            isBonded = isBonded,
            deviceType = type,
            isBhenxDevice = isBhenx,
            bhenxId = bhenxId,
            proximityLevel = rssi?.let { ProximityLevel.fromRssi(it) }
        )

        if (existingIndex >= 0) {
            // Mise à jour de l'appareil existant en préservant le statut BHENX si déjà reconnu
            val existing = updatedList[existingIndex]
            updatedList[existingIndex] = newInfo.copy(
                isBhenxDevice = newInfo.isBhenxDevice || existing.isBhenxDevice,
                bhenxId = newInfo.bhenxId ?: existing.bhenxId
            )
        } else {
            updatedList.add(newInfo)
            discoveredAddresses.add(safeAddress)
        }

        // Tri : les appareils BHENX FINDER apparaissent TOUJOURS en premier
        updatedList.sortWith(
            compareByDescending<DeviceInfo> { it.isBhenxDevice }
                .thenByDescending { it.rssi ?: -120 }
        )

        _devices.value = updatedList
    }

    @SuppressLint("MissingPermission")
    fun startScan(targetAddress: String? = null) {
        _errorMessage.value = null
        trackingAddress = targetAddress

        val currentSystemState = bluetoothManager.calculateCurrentState()
        if (currentSystemState == BluetoothState.BLUETOOTH_UNAVAILABLE) {
            _state.value = BluetoothState.BLUETOOTH_UNAVAILABLE
            _errorMessage.value = "Bluetooth indisponible sur cet appareil."
            return
        }

        if (currentSystemState == BluetoothState.BLUETOOTH_DISABLED) {
            _state.value = BluetoothState.BLUETOOTH_DISABLED
            _errorMessage.value = "Bluetooth est désactivé."
            return
        }

        if (!PermissionUtils.areAllPermissionsGranted(context)) {
            _state.value = BluetoothState.PERMISSION_REQUIRED
            _errorMessage.value = "Permissions Bluetooth nécessaires non accordées."
            return
        }

        val adapter = bluetoothManager.bluetoothAdapter ?: return

        try {
            leScanner = adapter.bluetoothLeScanner
            if (leScanner != null) {
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build()

                val filters = mutableListOf<ScanFilter>()
                // Si on cherche en mode global ou ciblé, on ne restreint pas les filtres
                // afin d'assurer la réception continue des paquets d'annonce
                leScanner?.startScan(filters, scanSettings, leScanCallback)
            }

            if (!isClassicReceiverRegistered && targetAddress == null) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(classicReceiver, filter)
                isClassicReceiverRegistered = true
            }

            try {
                if (!adapter.isDiscovering && targetAddress == null) {
                    adapter.startDiscovery()
                }
            } catch (_: SecurityException) {
                // Scan BLE continuera si discovery classique échoue
            }

            _isScanning.value = true
            _state.value = BluetoothState.SCANNING
        } catch (se: SecurityException) {
            _state.value = BluetoothState.ERROR
            _errorMessage.value = "Permission manquante pour scanner les appareils."
            _isScanning.value = false
        } catch (e: Exception) {
            _state.value = BluetoothState.ERROR
            _errorMessage.value = "Impossible de démarrer la recherche."
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        trackingAddress = null
        onTrackingRssiUpdate = null

        try {
            leScanner?.stopScan(leScanCallback)
        } catch (_: Exception) {
            // Ignorer
        }

        try {
            val adapter = bluetoothManager.bluetoothAdapter
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
            }
        } catch (_: Exception) {
            // Ignorer
        }

        unregisterClassicReceiver()
        _isScanning.value = false
        _state.value = BluetoothState.SCAN_FINISHED
    }

    fun clearDevices() {
        discoveredAddresses.clear()
        _devices.value = emptyList()
    }

    private fun unregisterClassicReceiver() {
        if (isClassicReceiverRegistered) {
            try {
                context.unregisterReceiver(classicReceiver)
                isClassicReceiverRegistered = false
            } catch (_: Exception) {
                // Ignorer
            }
        }
    }

    fun release() {
        stopScan()
    }
}
