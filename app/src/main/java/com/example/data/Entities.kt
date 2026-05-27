package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String, // Plain for ease of study and verification
    val role: String, // "admin", "staff", "viewer"
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val assetCode: String,
    val assetName: String,
    val category: String,
    val location: String,
    val purchaseDate: String,
    val status: String, // "active", "maintenance", "borrowed", "broken", "retired"
    val assignedTo: String?, // Username of assignee
    val description: String,
    val imageUrl: String?,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invitation_codes")
data class InvitationCode(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val expiresAt: Long,
    val usageLimit: Int,
    val usageCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val username: String,
    val role: String,
    val action: String, // e.g. "CREATE_ASSET", "CHANGE_ROLE"
    val detail: String // e.g. "Admin changed User A role to Staff"
)

@Entity(tableName = "asset_timelines")
data class AssetTimeline(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val assetCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // "created", "assigned", "updated", "maintenance", "returned", "status_changed"
    val changedBy: String,
    val detail: String
)
