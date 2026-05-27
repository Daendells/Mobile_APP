package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("UPDATE users SET role = :newRole WHERE id = :userId")
    suspend fun updateUserRole(userId: Int, newRole: String)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun getAllAssetsFlow(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: Int): Asset?

    @Query("SELECT * FROM assets WHERE assetCode = :code LIMIT 1")
    suspend fun getAssetByCode(code: String): Asset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: Asset): Long

    @Update
    suspend fun updateAsset(asset: Asset)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: Int)

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getAssetCount(): Int
}

@Dao
interface InvitationCodeDao {
    @Query("SELECT * FROM invitation_codes ORDER BY createdAt DESC")
    fun getAllInvitationCodesFlow(): Flow<List<InvitationCode>>

    @Query("SELECT * FROM invitation_codes WHERE code = :code LIMIT 1")
    suspend fun getInvitationCode(code: String): InvitationCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitationCode(code: InvitationCode): Long

    @Update
    suspend fun updateInvitationCode(code: InvitationCode)

    @Query("DELETE FROM invitation_codes WHERE id = :id")
    suspend fun deleteInvitationCodeById(id: Int)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog): Long
}

@Dao
interface AssetTimelineDao {
    @Query("SELECT * FROM asset_timelines WHERE assetCode = :assetCode ORDER BY timestamp DESC")
    fun getTimelineForAsset(assetCode: String): Flow<List<AssetTimeline>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeline(timeline: AssetTimeline): Long
}
