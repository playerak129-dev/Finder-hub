package com.bhenx.finder.bluetooth

/**
 * États du matériel et du cycle de vie Bluetooth.
 */
enum class BluetoothState {
    BLUETOOTH_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    BLUETOOTH_ENABLED,
    PERMISSION_REQUIRED,
    READY,
    SCANNING,
    SCAN_FINISHED,
    ERROR
}

/**
 * Erreurs Bluetooth typées pour une gestion sans crash ni message technique obscur.
 */
sealed class BluetoothError {
    object Unavailable : BluetoothError()
    object Disabled : BluetoothError()
    object PermissionRequired : BluetoothError()
    object LocationDisabled : BluetoothError()
    object ScanInterrupted : BluetoothError()
    data class General(val message: String) : BluetoothError()
}
