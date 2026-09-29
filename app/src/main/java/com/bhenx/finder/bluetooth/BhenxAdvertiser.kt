package com.bhenx.finder.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import com.bhenx.finder.model.BhenxDeviceIdentity
import com.bhenx.finder.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gère l'émission d'annonces BLE pour le Téléphone B (mode cible).
 * Faible consommation d'énergie (ADVERTISE_MODE_BALANCED) et aucun identifiant personnel.
 */
class BhenxAdvertiser(
    private val context: Context,
    private val bluetoothManager: BluetoothManager
) {

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var advertiser: BluetoothLeAdvertiser? = null

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _isAdvertising.value = true
            _errorMessage.value = null
        }

        override fun onStartFailure(errorCode: Int) {
            _isAdvertising.value = false
            _errorMessage.value = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Taille des données BLE dépassée"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Trop d'annonces Bluetooth actives"
                ADVERTISE_FAILED_ALREADY_STARTED -> "L'annonce est déjà en cours"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Erreur système Bluetooth interne"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Ce téléphone ne prend pas en charge la détection BHENX nécessaire pour ce mode."
                else -> "Échec du démarrage de l'émission BLE (code $errorCode)"
            }
        }
    }

    fun isHardwareSupported(): Boolean {
        val adapter = bluetoothManager.bluetoothAdapter ?: return false
        return adapter.bluetoothLeAdvertiser != null
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(identity: BhenxDeviceIdentity): Boolean {
        _errorMessage.value = null

        if (!bluetoothManager.isBluetoothEnabled.value) {
            _errorMessage.value = "Active le Bluetooth pour rendre ce téléphone détectable."
            return false
        }

        if (!PermissionUtils.hasBluetoothAdvertisePermission(context)) {
            _errorMessage.value = "Permission d'émission Bluetooth requise."
            return false
        }

        val adapter: BluetoothAdapter = bluetoothManager.bluetoothAdapter ?: run {
            _errorMessage.value = "Bluetooth non disponible."
            return false
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            _errorMessage.value = "Ce téléphone ne prend pas en charge la détection BHENX nécessaire pour ce mode."
            return false
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0) // Fonctionne jusqu'à arrêt explicite par l'utilisateur
            .build()

        val payload = BhenxBleProtocol.encodeAdvData(identity)

        val data = AdvertiseData.Builder()
            .addServiceUuid(BhenxBleProtocol.PARCEL_SERVICE_UUID)
            .addServiceData(BhenxBleProtocol.PARCEL_SERVICE_UUID, payload)
            .setIncludeDeviceName(false) // Permet de rester sous la limite de 31 octets
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        try {
            advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
            return true
        } catch (_: SecurityException) {
            _errorMessage.value = "Permission manquante pour démarrer l'émission BLE."
            return false
        } catch (e: Exception) {
            _errorMessage.value = "Erreur : ${e.message}"
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (_isAdvertising.value && advertiser != null) {
            try {
                advertiser?.stopAdvertising(advertiseCallback)
            } catch (_: Exception) {
                // Ignorer
            }
        }
        _isAdvertising.value = false
    }
}
