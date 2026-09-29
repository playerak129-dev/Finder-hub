package com.bhenx.finder.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité de l'historique des recherches réelles.
 * Aucun historique fictif n'est généré automatiquement.
 */
@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceName: String,
    val deviceAddress: String,
    val timestamp: Long,
    val result: String
)
