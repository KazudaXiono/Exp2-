package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val text: String,
    val timestamp: String
)

@Entity(tableName = "custom_tools")
data class CustomToolEntity(
    @PrimaryKey val id: String,
    val icon: String,
    val name: String,
    val desc: String,
    val href: String
)

@Entity(tableName = "hidden_builtins")
data class HiddenBuiltinEntity(
    @PrimaryKey val id: String
)

@Entity(tableName = "admin_settings")
data class AdminSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "qr_history")
data class QrHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val fg: String,
    val bg: String,
    val timestamp: String
)

@Dao
interface AppDao {
    @Query("SELECT * FROM comments ORDER BY id DESC")
    fun getAllComments(): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteComment(id: Int)

    @Query("DELETE FROM comments")
    suspend fun deleteAllComments()


    @Query("SELECT * FROM custom_tools")
    fun getAllCustomTools(): Flow<List<CustomToolEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomTool(tool: CustomToolEntity)

    @Query("DELETE FROM custom_tools WHERE id = :id")
    suspend fun deleteCustomTool(id: String)


    @Query("SELECT * FROM hidden_builtins")
    fun getAllHiddenBuiltins(): Flow<List<HiddenBuiltinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenBuiltin(hidden: HiddenBuiltinEntity)

    @Query("DELETE FROM hidden_builtins WHERE id = :id")
    suspend fun deleteHiddenBuiltin(id: String)


    @Query("SELECT value FROM admin_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AdminSettingEntity)


    @Query("SELECT * FROM qr_history ORDER BY id DESC LIMIT 5")
    fun getQrHistory(): Flow<List<QrHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQrHistory(qr: QrHistoryEntity)

    @Query("DELETE FROM qr_history WHERE text = :text")
    suspend fun deleteQrHistoryByText(text: String)

    @Query("DELETE FROM qr_history")
    suspend fun clearQrHistory()
}

@Database(
    entities = [
        CommentEntity::class,
        CustomToolEntity::class,
        HiddenBuiltinEntity::class,
        AdminSettingEntity::class,
        QrHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cloud_tools_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
