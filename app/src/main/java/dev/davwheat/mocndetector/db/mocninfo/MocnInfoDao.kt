package dev.davwheat.mocndetector.db.mocninfo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MocnInfoDao {
    @Insert
    suspend fun insert(mocnInfo: MocnInfo)

    @Query("SELECT * FROM mocninfo")
    suspend fun getAll(): List<MocnInfo>

    @Query("SELECT * FROM mocninfo ORDER BY checkedAt DESC LIMIT 1000")
    fun watchAll(): Flow<List<MocnInfo>>

    @Query("SELECT * FROM mocninfo WHERE id > :id ORDER BY id ASC LIMIT 1000")
    suspend fun getAboveId(id: Long): List<MocnInfo>

    @Query("DELETE FROM mocninfo")
    suspend fun deleteAll()
}
