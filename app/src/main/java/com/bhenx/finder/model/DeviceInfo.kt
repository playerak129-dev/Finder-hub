package com.bhenx.finder.model

/**
 * Représente un appareil Bluetooth détecté en temps réel.
 * En Phase 2, distingue clairement les téléphones BHENX FINDER.
 */
data class DeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int? = null,
    val isBonded: Boolean = false,
    val deviceType: String? = null,
    val isBhenxDevice: Boolean = false,
    val bhenxId: String? = null,
    val proximityLevel: ProximityLevel? = null
)
