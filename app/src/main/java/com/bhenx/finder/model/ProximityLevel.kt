package com.bhenx.finder.model

/**
 * Niveaux de proximité basés exclusivement sur la force réelle du signal (RSSI filtré).
 * RÈGLE ABSOLUE : Aucune distance en mètres n'est calculée ni affichée.
 */
enum class ProximityLevel(
    val title: String,
    val description: String,
    val pulseDurationMs: Int,
    val progressFraction: Float
) {
    VERY_WEAK(
        title = "SIGNAL TRÈS FAIBLE",
        description = "Le téléphone est peut-être encore loin. Continue à te déplacer.",
        pulseDurationMs = 1800,
        progressFraction = 0.20f
    ),
    FAR(
        title = "ÉLOIGNÉ",
        description = "Signal détecté. Rapproche-toi pour affiner la recherche.",
        pulseDurationMs = 1300,
        progressFraction = 0.40f
    ),
    MEDIUM(
        title = "À PROXIMITÉ",
        description = "Le téléphone est dans la même pièce ou zone proche.",
        pulseDurationMs = 850,
        progressFraction = 0.65f
    ),
    NEAR(
        title = "PROCHE",
        description = "Le téléphone est tout près de toi.",
        pulseDurationMs = 500,
        progressFraction = 0.85f
    ),
    VERY_CLOSE(
        title = "TRÈS PROCHE",
        description = "Le signal Bluetooth est très fort.",
        pulseDurationMs = 280,
        progressFraction = 1.0f
    );

    companion object {
        /**
         * Détermine le niveau de proximité selon le RSSI lissé.
         * Seuils réalistes pour du BLE en environnement intérieur standard.
         */
        fun fromRssi(rssi: Int): ProximityLevel {
            return when {
                rssi >= -55 -> VERY_CLOSE
                rssi >= -68 -> NEAR
                rssi >= -78 -> MEDIUM
                rssi >= -88 -> FAR
                else -> VERY_WEAK
            }
        }
    }
}
