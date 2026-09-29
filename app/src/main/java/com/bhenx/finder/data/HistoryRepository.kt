package com.bhenx.finder.data

import com.bhenx.finder.data.database.SearchHistoryDao
import com.bhenx.finder.model.SearchHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Dépôt pour la gestion de l'historique de recherche réel.
 * En Phase 1, aucun élément factice n'est inséré automatiquement.
 */
class HistoryRepository(private val dao: SearchHistoryDao) {

    val historyFlow: Flow<List<SearchHistory>> = dao.getAllHistory()

    suspend fun addSearchEntry(
        deviceName: String,
        deviceAddress: String,
        result: String
    ): Long = withContext(Dispatchers.IO) {
        val entry = SearchHistory(
            deviceName = deviceName.ifBlank { "Appareil inconnu" },
            deviceAddress = deviceAddress,
            timestamp = System.currentTimeMillis(),
            result = result
        )
        dao.insert(entry)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun deleteEntry(entry: SearchHistory) = withContext(Dispatchers.IO) {
        dao.delete(entry)
    }
}
