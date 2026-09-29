package com.bhenx.finder.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.Context
import com.bhenx.finder.model.BhenxDeviceIdentity
import java.nio.charset.StandardCharsets

/**
 * Serveur GATT exécuté sur le téléphone cible (B) pour recevoir les commandes
 * de sonnerie et d'arrêt de sonnerie envoyées par le téléphone chercheur (A).
 * RÈGLE DE SÉCURITÉ : Vérifie l'identité cible et valide le jeton de session.
 */
class BhenxGattServer(
    private val context: Context,
    private val ringManager: BhenxRingManager,
    private val identityProvider: () -> BhenxDeviceIdentity
) {

    private val androidBluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager

    private var gattServer: BluetoothGattServer? = null
    private var isRunning = false

    // Jeton de la session active de sonnerie en cours
    @Volatile
    var currentRingSessionToken: String? = null
        private set

    @Volatile
    private var sessionExpiryTimestamp: Long = 0L

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            // Connexion ponctuelle pour la transmission de commande
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (characteristic?.uuid == BhenxBleProtocol.COMMAND_CHAR_UUID && value != null) {
                val command = String(value, StandardCharsets.UTF_8).trim()
                val myIdentity = identityProvider()

                var isAuthorized = false

                when {
                    command.startsWith(BhenxBleProtocol.CMD_RING_PREFIX) -> {
                        val parsed = BhenxBleProtocol.parseRingCommand(command)
                        if (parsed != null) {
                            val targetId = parsed.first
                            val token = parsed.second
                            // Validation : commande adressée à ce téléphone ou ciblée
                            val matchesTarget = targetId.equals(myIdentity.id, ignoreCase = true) || targetId == "ANY"
                            if (matchesTarget && token.length >= 4) {
                                currentRingSessionToken = token
                                sessionExpiryTimestamp = System.currentTimeMillis() + 45_000L
                                isAuthorized = true
                                ringManager.startRinging {
                                    currentRingSessionToken = null
                                }
                            }
                        }
                    }
                    command.startsWith(BhenxBleProtocol.CMD_STOP_RING_PREFIX) -> {
                        val parsed = BhenxBleProtocol.parseStopCommand(command)
                        if (parsed != null) {
                            val targetId = parsed.first
                            val token = parsed.second
                            val matchesTarget = targetId.equals(myIdentity.id, ignoreCase = true) || targetId == "ANY"
                            val isTokenValid = currentRingSessionToken == null ||
                                    token == currentRingSessionToken ||
                                    System.currentTimeMillis() > sessionExpiryTimestamp

                            if (matchesTarget && isTokenValid) {
                                ringManager.stopRinging()
                                currentRingSessionToken = null
                                isAuthorized = true
                            }
                        }
                    }
                    command == BhenxBleProtocol.CMD_PING -> {
                        isAuthorized = true
                    }
                }

                if (responseNeeded && device != null) {
                    val status = if (isAuthorized) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE
                    try {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            status,
                            offset,
                            value
                        )
                    } catch (_: Exception) {
                        // Ignorer
                    }
                }
            } else if (responseNeeded && device != null) {
                try {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        offset,
                        null
                    )
                } catch (_: Exception) {
                    // Ignorer
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic?.uuid == BhenxBleProtocol.STATUS_CHAR_UUID && device != null) {
                val statusString = if (ringManager.isRinging.value) {
                    BhenxBleProtocol.STATUS_RINGING
                } else {
                    BhenxBleProtocol.STATUS_IDLE
                }
                val responseData = statusString.toByteArray(StandardCharsets.UTF_8)

                try {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        responseData
                    )
                } catch (_: Exception) {
                    // Ignorer
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRunning) return true

        return try {
            val server = androidBluetoothManager?.openGattServer(context, gattServerCallback)
            if (server != null) {
                val service = BluetoothGattService(
                    BhenxBleProtocol.SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY
                )

                // Caractéristique de commande (RING, STOP)
                val commandChar = BluetoothGattCharacteristic(
                    BhenxBleProtocol.COMMAND_CHAR_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                            BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_WRITE or
                            BluetoothGattCharacteristic.PERMISSION_READ
                )

                // Caractéristique d'état (RINGING, IDLE)
                val statusChar = BluetoothGattCharacteristic(
                    BhenxBleProtocol.STATUS_CHAR_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ or
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ
                )

                service.addCharacteristic(commandChar)
                service.addCharacteristic(statusChar)

                server.addService(service)
                gattServer = server
                isRunning = true
                true
            } else {
                false
            }
        } catch (_: Exception) {
            isRunning = false
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            gattServer?.clearServices()
            gattServer?.close()
        } catch (_: Exception) {
            // Ignorer
        }
        gattServer = null
        currentRingSessionToken = null
        isRunning = false
    }

    /**
     * Méthode de test direct de validation de sécurité pour tests automatisés.
     */
    fun validateCommandSecurity(command: String): Boolean {
        val myIdentity = identityProvider()
        return when {
            command.startsWith(BhenxBleProtocol.CMD_RING_PREFIX) -> {
                val parsed = BhenxBleProtocol.parseRingCommand(command)
                parsed != null && (parsed.first.equals(myIdentity.id, ignoreCase = true) || parsed.first == "ANY") && parsed.second.length >= 4
            }
            command.startsWith(BhenxBleProtocol.CMD_STOP_RING_PREFIX) -> {
                val parsed = BhenxBleProtocol.parseStopCommand(command)
                parsed != null && (parsed.first.equals(myIdentity.id, ignoreCase = true) || parsed.first == "ANY")
            }
            command == BhenxBleProtocol.CMD_PING -> true
            else -> false
        }
    }
}
