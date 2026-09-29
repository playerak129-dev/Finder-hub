package com.bhenx.finder.bluetooth

import java.util.ArrayDeque

/**
 * Filtre et stabilise les valeurs RSSI réelles par moyenne glissante pondérée.
 * Évite les sauts brusques d'indicateur dus aux interférences et multi-trajets.
 */
class RssiSmoother(private val windowSize: Int = 6) {

    private val samples = ArrayDeque<Int>(windowSize)

    @Synchronized
    fun addSample(rssi: Int): Int {
        if (samples.size >= windowSize) {
            samples.removeFirst()
        }
        samples.addLast(rssi)
        return calculateSmoothed()
    }

    @Synchronized
    fun getSmoothed(): Int? {
        if (samples.isEmpty()) return null
        return calculateSmoothed()
    }

    @Synchronized
    fun reset() {
        samples.clear()
    }

    private fun calculateSmoothed(): Int {
        if (samples.isEmpty()) return -100

        // Moyenne arithmétique simple et robuste
        val sum = samples.sum()
        return (sum.toDouble() / samples.size).toInt()
    }
}
