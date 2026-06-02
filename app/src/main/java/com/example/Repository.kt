package com.example

import kotlinx.coroutines.flow.Flow

class AppRepository(private val dao: AppDao) {
    val allComments: Flow<List<CommentEntity>> = dao.getAllComments()
    val allCustomTools: Flow<List<CustomToolEntity>> = dao.getAllCustomTools()
    val allHiddenBuiltins: Flow<List<HiddenBuiltinEntity>> = dao.getAllHiddenBuiltins()
    val qrHistory: Flow<List<QrHistoryEntity>> = dao.getQrHistory()

    suspend fun insertComment(name: String, text: String, timestamp: String) {
        dao.insertComment(CommentEntity(name = name, text = text, timestamp = timestamp))
    }

    suspend fun deleteComment(id: Int) {
        dao.deleteComment(id)
    }

    suspend fun deleteAllComments() {
        dao.deleteAllComments()
    }

    suspend fun insertCustomTool(id: String, icon: String, name: String, desc: String, href: String) {
        dao.insertCustomTool(CustomToolEntity(id, icon, name, desc, href))
    }

    suspend fun deleteCustomTool(id: String) {
        dao.deleteCustomTool(id)
    }

    suspend fun hideBuiltin(id: String) {
        dao.insertHiddenBuiltin(HiddenBuiltinEntity(id))
    }

    suspend fun showBuiltin(id: String) {
        dao.deleteHiddenBuiltin(id)
    }

    suspend fun getSetting(key: String): String? {
        return dao.getSetting(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        dao.saveSetting(AdminSettingEntity(key, value))
    }

    suspend fun insertQrHistory(text: String, fg: String, bg: String, timestamp: String) {
        dao.deleteQrHistoryByText(text) // De-duplicate
        dao.insertQrHistory(QrHistoryEntity(text = text, fg = fg, bg = bg, timestamp = timestamp))
    }

    suspend fun clearQrHistory() {
        dao.clearQrHistory()
    }
}
