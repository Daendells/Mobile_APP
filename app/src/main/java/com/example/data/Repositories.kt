package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ApiLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val method: String,
    val endpoint: String,
    val status: String,
    val requestBody: String?,
    val responseBody: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.S", Locale.getDefault()).format(Date(timestamp))
}

class AssetRepository(
    private val userDao: UserDao,
    private val assetDao: AssetDao,
    private val invitationCodeDao: InvitationCodeDao,
    private val logDao: ActivityLogDao,
    private val timelineDao: AssetTimelineDao
) {
    // API Call Logs visible in the in-app Developer drawer
    private val _apiLogs = MutableStateFlow<List<ApiLogEntry>>(emptyList())
    val apiLogs: StateFlow<List<ApiLogEntry>> = _apiLogs.asStateFlow()

    private fun addApiLog(method: String, endpoint: String, status: String, req: String?, resp: String?) {
        val entry = ApiLogEntry(
            method = method,
            endpoint = endpoint,
            status = status,
            requestBody = req,
            responseBody = resp
        )
        _apiLogs.value = listOf(entry) + _apiLogs.value
    }

    // --- USER / AUTH ACTIONS ===

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun getInitialCount(): Int = userDao.getUserCount()

    suspend fun login(username: String, passwordHash: String): User? {
        val user = userDao.getUserByUsername(username)
        val requestJson = "{\n  \"username\": \"$username\",\n  \"password\": \"********\"\n}"
        if (user != null && user.passwordHash == passwordHash) {
            val responseJson = "{\n  \"status\": \"success\",\n  \"token\": \"token_${UUID.randomUUID().toString().take(16)}\",\n  \"user\": {\n    \"id\": ${user.id},\n    \"username\": \"${user.username}\",\n    \"role\": \"${user.role}\",\n    \"email\": \"${user.email}\"\n  }\n}"
            addApiLog("POST", "/login", "200 OK", requestJson, responseJson)
            
            // Log audit
            logDao.insertLog(
                ActivityLog(
                    username = user.username,
                    role = user.role,
                    action = "LOGIN",
                    detail = "User logged in successfully with role '${user.role}'."
                )
            )
            return user
        } else {
            val responseJson = "{\n  \"status\": \"error\",\n  \"message\": \"Invalid database credentials, authentication failed\"\n}"
            addApiLog("POST", "/login", "401 Unauthorized", requestJson, responseJson)
            return null
        }
    }

    suspend fun registerUser(username: String, passwordHash: String, email: String): User? {
        val existing = userDao.getUserByUsername(username)
        val requestJson = "{\n  \"username\": \"$username\",\n  \"email\": \"$email\"\n}"
        if (existing != null) {
            val responseJson = "{\n  \"error\": \"User with username '$username' already exists\"\n}"
            addApiLog("POST", "/register", "400 Bad Request", requestJson, responseJson)
            return null
        }
        val newUser = User(username = username, passwordHash = passwordHash, role = "viewer", email = email)
        val id = userDao.insertUser(newUser)
        val seededUser = newUser.copy(id = id.toInt())
        val responseJson = "{\n  \"success\": true,\n  \"user_id\": $id,\n  \"role\": \"viewer\"\n}"
        addApiLog("POST", "/register", "201 Created", requestJson, responseJson)

        logDao.insertLog(
            ActivityLog(
                username = username,
                role = "viewer",
                action = "REGISTRATION",
                detail = "New user registered. Assigned default role 'viewer'."
            )
        )
        return seededUser
    }

    suspend fun updateUserRole(actor: User, targetUserId: Int, targetUsername: String, newRole: String): Boolean {
        userDao.updateUserRole(targetUserId, newRole)
        val requestJson = "{\n  \"role\": \"$newRole\"\n}"
        val responseJson = "{\n  \"success\": true,\n  \"updated_user_id\": $targetUserId,\n  \"new_role\": \"$newRole\"\n}"
        addApiLog("PUT", "/users/$targetUserId/role", "200 OK", requestJson, responseJson)

        logDao.insertLog(
            ActivityLog(
                username = actor.username,
                role = actor.role,
                action = "ROLE_PROMOTION_DEMOTION",
                detail = "${actor.username} updated $targetUsername's role to '$newRole'."
            )
        )
        return true
    }

    // --- ASSET ACTIONS ===

    fun getAllAssetsFlow(): Flow<List<Asset>> = assetDao.getAllAssetsFlow()

    suspend fun getAssetByCode(code: String): Asset? {
        val asset = assetDao.getAssetByCode(code)
        val response = if (asset != null) {
            "{\n  \"asset_code\": \"${asset.assetCode}\",\n  \"name\": \"${asset.assetName}\",\n  \"status\": \"${asset.status}\"\n}"
        } else "{\n  \"error\": \"Asset not found\"\n}"
        addApiLog("GET", "/assets/code/$code", if (asset != null) "200 OK" else "404 Not Found", null, response)
        return asset
    }

    suspend fun createAsset(actor: User, asset: Asset): Boolean {
        // Prevent duplicate asset code
        val target = assetDao.getAssetByCode(asset.assetCode)
        val requestJson = "{\n  \"asset_code\": \"${asset.assetCode}\",\n  \"asset_name\": \"${asset.assetName}\",\n  \"category\": \"${asset.category}\",\n  \"location\": \"${asset.location}\",\n  \"status\": \"${asset.status}\"\n}"
        if (target != null) {
            val responseJson = "{\n  \"error\": \"Duplicate asset code ${asset.assetCode} detected!\"\n}"
            addApiLog("POST", "/assets", "409 Conflict", requestJson, responseJson)
            return false
        }
        val id = assetDao.insertAsset(asset)
        val responseJson = "{\n  \"success\": true,\n  \"asset_id\": $id,\n  \"asset_code\": \"${asset.assetCode}\"\n}"
        addApiLog("POST", "/assets", "201 Created", requestJson, responseJson)

        // Log actions
        logDao.insertLog(
            ActivityLog(
                username = actor.username,
                role = actor.role,
                action = "CREATE_ASSET",
                detail = "Created asset ${asset.assetCode} (${asset.assetName})."
            )
        )
        timelineDao.insertTimeline(
            AssetTimeline(
                assetCode = asset.assetCode,
                action = "created",
                changedBy = actor.username,
                detail = "Asset created by ${actor.username} with status '${asset.status}'."
            )
        )
        return true
    }

    suspend fun updateAsset(actor: User, asset: Asset): Boolean {
        assetDao.updateAsset(asset)
        val requestJson = "{\n  \"id\": ${asset.id},\n  \"asset_code\": \"${asset.assetCode}\",\n  \"asset_name\": \"${asset.assetName}\",\n  \"status\": \"${asset.status}\",\n  \"assigned_to\": \"${asset.assignedTo ?: ""}\"\n}"
        val responseJson = "{\n  \"success\": true,\n  \"message\": \"Asset updated successfully\"\n}"
        addApiLog("PUT", "/assets/${asset.id}", "200 OK", requestJson, responseJson)

        logDao.insertLog(
            ActivityLog(
                username = actor.username,
                role = actor.role,
                action = "UPDATE_ASSET",
                detail = "Updated asset ${asset.assetCode} values."
            )
        )
        timelineDao.insertTimeline(
            AssetTimeline(
                assetCode = asset.assetCode,
                action = "updated",
                changedBy = actor.username,
                detail = "Asset details modified."
            )
        )
        return true
    }

    suspend fun deleteAsset(actor: User, asset: Asset): Boolean {
        assetDao.deleteAssetById(asset.id)
        val responseJson = "{\n  \"success\": true,\n  \"deleted_asset_id\": ${asset.id},\n  \"asset_code\": \"${asset.assetCode}\"\n}"
        addApiLog("DELETE", "/assets/${asset.id}", "200 OK", null, responseJson)

        logDao.insertLog(
            ActivityLog(
                username = actor.username,
                role = actor.role,
                action = "DELETE_ASSET",
                detail = "Deleted asset ${asset.assetCode}."
            )
        )
        return true
    }

    suspend fun assignAsset(actor: User, assetCode: String, targetHolder: String, notes: String): Boolean {
        val asset = assetDao.getAssetByCode(assetCode) ?: return false
        val updated = asset.copy(assignedTo = targetHolder, status = "borrowed")
        assetDao.updateAsset(updated)

        val requestJson = "{\n  \"asset_code\": \"$assetCode\",\n  \"assignee\": \"$targetHolder\",\n  \"notes\": \"$notes\"\n}"
        val responseJson = "{\n  \"success\": true,\n  \"message\": \"Assigned successfully to $targetHolder\"\n}"
        addApiLog("POST", "/assets/assign", "200 OK", requestJson, responseJson)

        logDao.insertLog(
            ActivityLog(
                username = actor.username,
                role = actor.role,
                action = "ASSIGN_ASSET",
                detail = "Assigned asset $assetCode to user '$targetHolder'."
            )
        )
        timelineDao.insertTimeline(
            AssetTimeline(
                assetCode = assetCode,
                action = "assigned",
                changedBy = actor.username,
                detail = "Asset assigned to $targetHolder. $notes"
            )
        )
        return true
    }

    suspend fun returnAsset(actor: User, assetCode: String, returnStatus: String): Boolean {
        val asset = assetDao.getAssetByCode(assetCode) ?: return false
        val updated = asset.copy(assignedTo = null, status = returnStatus)
        assetDao.updateAsset(updated)

        val requestJson = "{\n  \"asset_code\": \"$assetCode\",\n  \"return_status\": \"$returnStatus\"\n}"
        val responseJson = "{\n  \"success\": true,\n  \"message\": \"Asset returned to pool in status $returnStatus\"\n}"
        addApiLog("POST", "/assets/return", "200 OK", requestJson, responseJson)

        logDao.insertLog(
            ActivityLog(
                username = actor.username,
                role = actor.role,
                action = "RETURN_ASSET",
                detail = "Returned asset $assetCode. Updated status to '$returnStatus'."
            )
        )
        timelineDao.insertTimeline(
            AssetTimeline(
                assetCode = assetCode,
                action = "returned",
                changedBy = actor.username,
                detail = "Asset returned in '$returnStatus' status."
            )
        )
        return true
    }

    // --- EMERGENCY ADMIN RECOVERY SYSTEM ---

    fun getAllInvitationCodesFlow(): Flow<List<InvitationCode>> = invitationCodeDao.getAllInvitationCodesFlow()

    suspend fun createInvitationCode(actor: User, codeText: String, expiresAt: Long, limit: Int): Boolean {
        val codeObj = InvitationCode(
            code = codeText,
            expiresAt = expiresAt,
            usageLimit = limit,
            usageCount = 0,
            isActive = true
        )
        invitationCodeDao.insertInvitationCode(codeObj)
        
        logDao.insertLog(
            ActivityLog(
                username = actor.username,
                role = actor.role,
                action = "CREATE_INVITATION_CODE",
                detail = "Generated recovery invitation code '$codeText' (limit: $limit)."
            )
        )
        return true
    }

    suspend fun deleteInvitationCode(actor: User, codeId: Int, codeText: String) {
        invitationCodeDao.deleteInvitationCodeById(codeId)
        logDao.insertLog(
            ActivityLog(
                username = actor.username,
                role = actor.role,
                action = "DELETE_INVITATION_CODE",
                detail = "Deleted recovery code '$codeText'."
            )
        )
    }

    suspend fun validateInvitationCode(userId: Int, username: String, code: String): Pair<Boolean, String> {
        val codeObj = invitationCodeDao.getInvitationCode(code)
        val requestJson = "{\n  \"user_id\": $userId,\n  \"username\": \"$username\",\n  \"code\": \"$code\"\n}"
        
        if (codeObj == null) {
            val responseJson = "{\n  \"success\": false,\n  \"error\": \"Invitation code is completely invalid and does not exist in the database!\"\n}"
            addApiLog("POST", "/invite-code/validate", "404 Not Found", requestJson, responseJson)
            return Pair(false, "Invitation code not found!")
        }

        if (!codeObj.isActive) {
            val responseJson = "{\n  \"success\": false,\n  \"error\": \"This invitation code has been manually revoked/disabled!\"\n}"
            addApiLog("POST", "/invite-code/validate", "410 Gone", requestJson, responseJson)
            return Pair(false, "Invitation code is disabled!")
        }

        if (System.currentTimeMillis() > codeObj.expiresAt) {
            val responseJson = "{\n  \"success\": false,\n  \"error\": \"This invitation code has expired on ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(codeObj.expiresAt))}\"\n}"
            addApiLog("POST", "/invite-code/validate", "400 Bad Request", requestJson, responseJson)
            return Pair(false, "Invitation code is expired!")
        }

        if (codeObj.usageCount >= codeObj.usageLimit) {
            val responseJson = "{\n  \"success\": false,\n  \"error\": \"This invitation code usage limit (${codeObj.usageLimit}/${codeObj.usageLimit}) has been exceeded!\"\n}"
            addApiLog("POST", "/invite-code/validate", "403 Forbidden", requestJson, responseJson)
            return Pair(false, "Invitation code usage limit exceeded!")
        }

        // Elevate user to admin
        userDao.updateUserRole(userId, "admin")
        
        // Update usage count
        val updatedCode = codeObj.copy(usageCount = codeObj.usageCount + 1)
        invitationCodeDao.updateInvitationCode(updatedCode)

        val responseJson = "{\n  \"success\": true,\n  \"message\": \"User '$username' promoted to Admin!\",\n  \"usage_stats\": \"${updatedCode.usageCount}/${updatedCode.usageLimit}\"\n}"
        addApiLog("POST", "/invite-code/validate", "200 OK", requestJson, responseJson)

        logDao.insertLog(
            ActivityLog(
                username = username,
                role = "admin",
                action = "EMERGENCY_RECOVERY",
                detail = "User elevated to Admin via Emergency Invitation Code '$code'. Usage count is now ${updatedCode.usageCount}/${updatedCode.usageLimit}."
            )
        )
        return Pair(true, "Emergency Recovery Successful! You are now promoted to ADMIN.")
    }

    // --- ACTIVITY LOGS & TIMELINES ---

    fun getAllActivityLogs(): Flow<List<ActivityLog>> = logDao.getAllActivityLogs()

    fun getTimelineForAsset(assetCode: String): Flow<List<AssetTimeline>> = timelineDao.getTimelineForAsset(assetCode)
}
