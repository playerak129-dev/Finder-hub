package com.bhenx.finder.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RingCommandStatus {
    IDLE,
    CONNECTING,
    SENDING,
    RINGING_CONFIRMED,
    STOPPED,
    UNREACHABLE
}

/**
 * Client GATT exécuté sur le téléphone chercheur (A) pour transmettre
 * les commandes RING et STOP au téléphone cible (B).
 */
class BhenxGattClient(
    private val context: Context,
    private val bluetoothManager: BluetoothManager
) {

    private val _commandStatus = MutableStateFlow(RingCommandStatus.IDLE)
    val commandStatus: StateFlow<RingCommandStatus> = _commandStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pendingCommand: ByteArray? = null
    private var isSendingRing = false

    private val timeoutRunnable = Runnable {
        if (_commandStatus.value == RingCommandStatus.CONNECTING || _commandStatus.value == RingCommandStatus.SENDING) {
            _commandStatus.value = RingCommandStatus.UNREACHABLE
            _statusMessage.value = "Impossible de faire sonner le téléphone. Rapproche-toi ou vérifie que le téléphone cible est actif."
            disconnect()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mainHandler.post {
                    if (_commandStatus.value == RingCommandStatus.RINGING_CONFIRMED) {
                        _statusMessage.value = "Connexion perdue — la sonnerie s'arrêtera automatiquement."
                    } else {
                        _commandStatus.value = RingCommandStatus.UNREACHABLE
                        _statusMessage.value = "Impossible de faire sonner le téléphone. Rapproche-toi ou vérifie que le téléphone cible est actif."
                    }
                    disconnect()
                }
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mainHandler.post {
                    _statusMessage.value = "Envoi de la commande…"
                }
                try {
                    gatt?.discoverServices()
                } catch (_: Exception) {
                    mainHandler.post {
                        _commandStatus.value = RingCommandStatus.UNREACHABLE
                        _statusMessage.value = "Impossible de faire sonner le téléphone. Rapproche-toi ou vérifie que le téléphone cible est actif."
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mainHandler.post {
                    if (_commandStatus.value == RingCommandStatus.RINGING_CONFIRMED) {
                        _statusMessage.value = "Connexion perdue — la sonnerie s'arrêtera automatiquement."
                    } else if (_commandStatus.value == RingCommandStatus.CONNECTING || _commandStatus.value == RingCommandStatus.SENDING) {
                        _commandStatus.value = RingCommandStatus.UNREACHABLE
                        _statusMessage.value = "Impossible de faire sonner le téléphone. Rapproche-toi ou vérifie que le téléphone cible est actif."
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(BhenxBleProtocol.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BhenxBleProtocol.COMMAND_CHAR_UUID)

                val cmd = pendingCommand
                if (characteristic != null && cmd != null) {
                    mainHandler.post {
                        _commandStatus.value = RingCommandStatus.SENDING
                        _statusMessage.value = "Envoi de la commande…"
                    }
                    characteristic.value = cmd
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    gatt.writeCharacteristic(characteristic)
                } else {
                    mainHandler.post {
                        _commandStatus.value = RingCommandStatus.UNREACHABLE
                        _statusMessage.value = "Impossible de faire sonner le téléphone. Rapproche-toi ou vérifie que le téléphone cible est actif."
                        disconnect()
                    }
                }
            } else {
                mainHandler.post {
                    _commandStatus.value = RingCommandStatus.UNREACHABLE
                    _statusMessage.value = "Impossible de faire sonner le téléphone. Rapproche-toi ou vérifie que le téléphone cible est actif."
                    disconnect()
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            mainHandler.removeCallbacks(timeoutRunnable)
            mainHandler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (isSendingRing) {
                        _commandStatus.value = RingCommandStatus.RINGING_CONFIRMED
                        _statusMessage.value = "Téléphone en train de sonner"
                    } else {
                        _commandStatus.value = RingCommandStatus.STOPPED
                        _statusMessage.value = "Sonnerie arrêtée"
                    }
                } else {
                    _commandStatus.value = RingCommandStatus.UNREACHABLE
                    _statusMessage.value = "Impossible de faire sonner le téléphone. Rapproche-toi ou vérifie que le téléphone cible est actif."
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendRing(deviceAddress: String, sessionToken: String) {
        sendRing(deviceAddress, "ANY", sessionToken)
    }

    @SuppressLint("MissingPermission")
    fun sendRing(deviceAddress: String, targetBhenxId: String, sessionToken: String) {
        disconnect()
        isSendingRing = true
        pendingCommand = BhenxBleProtocol.createRingCommand(targetBhenxId, sessionToken)
        _commandStatus.value = RingCommandStatus.CONNECTING
        _statusMessage.value = "Connexion au téléphone…"

        // Timeout de sécurité de 8 secondes
        mainHandler.postDelayed(timeoutRunnable, 8000)

        connectAndExecute(deviceAddress)
    }

    @SuppressLint("MissingPermission")
    fun sendStopRing(deviceAddress: String, sessionToken: String) {
        sendStopRing(deviceAddress, "ANY", sessionToken)
    }

    @SuppressLint("MissingPermission")
    fun sendStopRing(deviceAddress: String, targetBhenxId: String, sessionToken: String) {
        isSendingRing = false
        pendingCommand = BhenxBleProtocol.createStopCommand(targetBhenxId, sessionToken)
        _commandStatus.value = RingCommandStatus.SENDING
        _statusMessage.value = "Arrêt de la sonnerie…"

        mainHandler.postDelayed(timeoutRunnable, 8000)

        if (activeGatt != null) {
            val service = activeGatt?.getService(BhenxBleProtocol.SERVICE_UUID)
            val characteristic = service?.getCharacteristic(BhenxBleProtocol.COMMAND_CHAR_UUID)
            val cmd = pendingCommand
            if (characteristic != null && cmd != null) {
                characteristic.value = cmd
                activeGatt?.writeCharacteristic(characteristic)
                return
            }
        }

        connectAndExecute(deviceAddress)
    }

    @SuppressLint("MissingPermission")
    private fun connectAndExecute(deviceAddress: String) {
        val adapter: BluetoothAdapter = bluetoothManager.bluetoothAdapter ?: run {
            _commandStatus.value = RingCommandStatus.UNREACHABLE
            _statusMessage.value = "Bluetooth non disponible"
            return
        }

        try {
            val remoteDevice = adapter.getRemoteDevice(deviceAddress)
            activeGatt = remoteDevice.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (_: Exception) {
            _commandStatus.value = RingCommandStatus.UNREACHABLE
            _statusMessage.value = "Impossible de contacter le téléphone"
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        mainHandler.removeCallbacks(timeoutRunnable)
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {
            // Ignorer
        }
        activeGatt = null
        pendingCommand = null
    }

    fun resetState() {
        disconnect()
        _commandStatus.value = RingCommandStatus.IDLE
        _statusMessage.value = null
    }
}
