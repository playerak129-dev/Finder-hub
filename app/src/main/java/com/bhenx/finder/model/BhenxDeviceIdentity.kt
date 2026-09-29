package com.bhenx.finder.model

import android.content.Context
import java.util.UUID

/**
 * Identité locale anonyme et sécurisée de l'appareil BHENX.
 * RÈGLE ABSOLUE DE SÉCURITÉ :
 * - Généré aléatoirement en local
 * - Persistant
 * - Strictement aucun identifiant personnel (IMEI, tél, email, Android ID, pub)
 * - Jamais transmis sur Internet
 */
data class BhenxDeviceIdentity(
    val id: String, // ex: "BHX-7F8A3B"
    val name: String
) {
    companion object {
        private const val PREFS_IDENTITY = "bhenx_identity_prefs"
        private const val KEY_UNIQUE_ID = "key_bhenx_unique_id"

        fun getOrCreate(context: Context, customName: String): BhenxDeviceIdentity {
            val prefs = context.getSharedPreferences(PREFS_IDENTITY, Context.MODE_PRIVATE)
            var currentId = prefs.getString(KEY_UNIQUE_ID, null)
            if (currentId.isNullOrBlank()) {
                val randomHex = UUID.randomUUID().toString()
                    .replace("-", "")
                    .take(6)
                    .uppercase()
                currentId = "BHX-$randomHex"
                prefs.edit().putString(KEY_UNIQUE_ID, currentId).apply()
            }
            return BhenxDeviceIdentity(
                id = currentId,
                name = customName.ifBlank { "Mon téléphone" }
            )
        }
    }
}
