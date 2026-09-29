package com.bhenx.finder.bluetooth

import android.os.ParcelUuid
import com.bhenx.finder.model.BhenxDeviceIdentity
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Protocole de communication BLE minimaliste entre deux téléphones BHENX FINDER.
 * Aucune donnée personnelle n'est envoyée.
 */
object BhenxBleProtocol {

    // UUIDs dédiés à BHENX FINDER
    val SERVICE_UUID: UUID = UUID.fromString("0000B4E0-0000-1000-8000-00805F9B34FB")
    val PARCEL_SERVICE_UUID: ParcelUuid = ParcelUuid(SERVICE_UUID)

    // Caractéristique d'écriture des commandes (A -> B : RING, STOP_RING)
    val COMMAND_CHAR_UUID: UUID = UUID.fromString("0000B4E1-0000-1000-8000-00805F9B34FB")

    // Caractéristique de lecture/notification d'état (B -> A : RING_RECEIVED, IDLE)
    val STATUS_CHAR_UUID: UUID = UUID.fromString("0000B4E2-0000-1000-8000-00805F9B34FB")

    // Commandes
    const val CMD_RING_PREFIX = "RING:"
    const val CMD_STOP_RING_PREFIX = "STOP:"
    const val CMD_PING = "PING"

    // Réponses de confirmation
    const val RESP_RING_RECEIVED = "RING_RECEIVED"
    const val RESP_STOP_RECEIVED = "STOP_RECEIVED"
    const val RESP_PONG = "PONG"
    const val STATUS_IDLE = "IDLE"
    const val STATUS_RINGING = "RINGING"

    // En-tête des trames d'annonce BLE (Service Data)
    private val HEADER = byteArrayOf(0x42, 0x48) // 'B', 'H'

    /**
     * Encode les données d'annonce BLE pour le téléphone cible B.
     * Format compact : [HEADER 2 octets] + [ID len 1 octet] + [ID] + [NAME len 1 octet] + [NAME]
     */
    fun encodeAdvData(identity: BhenxDeviceIdentity): ByteArray {
        val idBytes = identity.id.toByteArray(StandardCharsets.UTF_8)
        val nameBytes = identity.name.take(12).toByteArray(StandardCharsets.UTF_8)

        val totalLength = 2 + 1 + idBytes.size + 1 + nameBytes.size
        val buffer = ByteArray(totalLength)

        buffer[0] = HEADER[0]
        buffer[1] = HEADER[1]

        buffer[2] = idBytes.size.toByte()
        System.arraycopy(idBytes, 0, buffer, 3, idBytes.size)

        val nameOffset = 3 + idBytes.size
        buffer[nameOffset] = nameBytes.size.toByte()
        System.arraycopy(nameBytes, 0, buffer, nameOffset + 1, nameBytes.size)

        return buffer
    }

    /**
     * Décode les données d'annonce BLE d'un appareil détecté.
     */
    fun decodeAdvData(data: ByteArray?): Pair<String, String>? {
        if (data == null || data.size < 5) return null
        if (data[0] != HEADER[0] || data[1] != HEADER[1]) return null

        try {
            val idLength = data[2].toInt() and 0xFF
            if (3 + idLength > data.size) return null
            val id = String(data, 3, idLength, StandardCharsets.UTF_8)

            val nameOffset = 3 + idLength
            if (nameOffset >= data.size) return Pair(id, "Téléphone BHENX")

            val nameLength = data[nameOffset].toInt() and 0xFF
            val name = if (nameOffset + 1 + nameLength <= data.size && nameLength > 0) {
                String(data, nameOffset + 1, nameLength, StandardCharsets.UTF_8)
            } else {
                "Téléphone BHENX"
            }

            return Pair(id, name)
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Crée une commande RING avec le jeton de session (cible par défaut ANY).
     */
    fun createRingCommand(sessionToken: String): ByteArray = createRingCommand("ANY", sessionToken)

    /**
     * Crée une commande RING sécurisée avec l'identifiant cible et le jeton de session.
     * Format : RING:<targetId>:<sessionToken>
     */
    fun createRingCommand(targetId: String, sessionToken: String): ByteArray {
        val cleanTarget = targetId.ifBlank { "ANY" }
        return "$CMD_RING_PREFIX$cleanTarget:$sessionToken".toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Crée une commande STOP avec le jeton de session (cible par défaut ANY).
     */
    fun createStopCommand(sessionToken: String): ByteArray = createStopCommand("ANY", sessionToken)

    /**
     * Crée une commande STOP sécurisée avec l'identifiant cible et le jeton de session.
     * Format : STOP:<targetId>:<sessionToken>
     */
    fun createStopCommand(targetId: String, sessionToken: String): ByteArray {
        val cleanTarget = targetId.ifBlank { "ANY" }
        return "$CMD_STOP_RING_PREFIX$cleanTarget:$sessionToken".toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Décode et valide une commande RING reçue.
     * Retourne Pair(targetId, sessionToken) ou null si le format est invalide.
     */
    fun parseRingCommand(raw: String): Pair<String, String>? {
        if (!raw.startsWith(CMD_RING_PREFIX)) return null
        val payload = raw.removePrefix(CMD_RING_PREFIX)
        val parts = payload.split(":")
        return when {
            parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank() -> {
                Pair(parts[0], parts[1])
            }
            parts.size == 1 && parts[0].isNotBlank() -> {
                // Compatibilité avec format simplifié RING:<sessionToken>
                Pair("ANY", parts[0])
            }
            else -> null
        }
    }

    /**
     * Décode et valide une commande STOP reçue.
     * Retourne Pair(targetId, sessionToken) ou null si le format est invalide.
     */
    fun parseStopCommand(raw: String): Pair<String, String>? {
        if (!raw.startsWith(CMD_STOP_RING_PREFIX)) return null
        val payload = raw.removePrefix(CMD_STOP_RING_PREFIX)
        val parts = payload.split(":")
        return when {
            parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank() -> {
                Pair(parts[0], parts[1])
            }
            parts.size == 1 && parts[0].isNotBlank() -> {
                Pair("ANY", parts[0])
            }
            else -> null
        }
    }
}
