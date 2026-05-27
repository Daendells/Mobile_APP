package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    LOGIN,
    DASHBOARD,
    ASSET_LIST,
    ASSET_DETAIL,
    USER_MANAGEMENT,
    INVITATION_CODES,
    ACTIVITY_LOGS,
    API_CONSOLE
}

class AssetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = AssetRepository(
        userDao = database.userDao(),
        assetDao = database.assetDao(),
        invitationCodeDao = database.invitationCodeDao(),
        logDao = database.activityLogDao(),
        timelineDao = database.assetTimelineDao()
    )

    // --- SCREEN NAVIGATION STATE ---
    private val _currentScreen = MutableStateFlow(Screen.LOGIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- AUTHENTICATION STATE ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // --- SEARCH, FILTER, AND SORTING FLOWS ---
    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow("") // "" means All
    val statusFilter = MutableStateFlow("") // "" means All
    val sortBy = MutableStateFlow("newest") // "newest", "oldest", "name_asc", "code_asc"

    // --- TRIGGER RE-SEED CHECKS ---
    init {
        viewModelScope.launch {
            // Trigger DB initialization immediately
            repository.getInitialCount()
        }
    }

    // --- REACTIVE DATA FEEDS ---
    val allAssets: StateFlow<List<Asset>> = repository.getAllAssetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAssets: StateFlow<List<Asset>> = combine(
        allAssets,
        searchQuery,
        categoryFilter,
        statusFilter,
        sortBy
    ) { assets, search, category, status, sort ->
        var list = assets

        // 1. Search Query
        if (search.isNotBlank()) {
            list = list.filter {
                it.assetName.contains(search, ignoreCase = true) ||
                it.assetCode.contains(search, ignoreCase = true) ||
                it.description.contains(search, ignoreCase = true)
            }
        }

        // 2. Category
        if (category.isNotBlank()) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        // 3. Status
        if (status.isNotBlank()) {
            list = list.filter { it.status.equals(status, ignoreCase = true) }
        }

        // 4. Sorting
        when (sort) {
            "newest" -> list.sortedByDescending { it.createdAt }
            "oldest" -> list.sortedBy { it.createdAt }
            "name_asc" -> list.sortedBy { it.assetName.lowercase() }
            "code_asc" -> list.sortedBy { it.assetCode.lowercase() }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLog>> = repository.getAllActivityLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invitationCodes: StateFlow<List<InvitationCode>> = repository.getAllInvitationCodesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiLogs: StateFlow<List<ApiLogEntry>> = repository.apiLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ASSET DETAILS TIMELINE STATE ---
    private val _selectedAssetCode = MutableStateFlow<String?>(null)
    val selectedAssetCode: StateFlow<String?> = _selectedAssetCode.asStateFlow()

    val selectedAsset: StateFlow<Asset?> = combine(allAssets, _selectedAssetCode) { assets, code ->
        if (code == null) null
        else assets.find { it.assetCode == code }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedAssetTimeline: StateFlow<List<AssetTimeline>> = _selectedAssetCode
        .flatMapLatest { code ->
            if (code == null) flowOf(emptyList())
            else repository.getTimelineForAsset(code)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI ERROR / SUCCESS ALERT FLOATS ---
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // --- ROUTING UTILITIES ---
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectAssetAndNavigate(code: String) {
        _selectedAssetCode.value = code
        _currentScreen.value = Screen.ASSET_DETAIL
    }

    // --- INTERACTIVE ACTIONS ---

    fun login(username: String, passwordHash: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.login(username, passwordHash)
            if (user != null) {
                _currentUser.value = user
                _currentScreen.value = Screen.DASHBOARD
                showToast("Welcome back, ${user.username}!")
                onResult(true)
            } else {
                showToast("Invalid credentials! Try: admin, staff, or viewer.")
                onResult(false)
            }
        }
    }

    fun register(username: String, email: String, passwordHash: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.registerUser(username, passwordHash, email)
            if (user != null) {
                _currentUser.value = user
                _currentScreen.value = Screen.DASHBOARD
                showToast("Successfully registered! Default role: VIEWER.")
                onResult(true)
            } else {
                showToast("Username already exists in database!")
                onResult(false)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = Screen.LOGIN
        showToast("Logged out successfully.")
    }

    fun promoteUser(targetUserId: Int, targetUsername: String, newRole: String) {
        val actor = _currentUser.value ?: return
        if (actor.role != "admin") {
            showToast("Access Denied: Only Admin can elevate/change roles.")
            return
        }
        viewModelScope.launch {
            repository.updateUserRole(actor, targetUserId, targetUsername, newRole)
            showToast("User '$targetUsername' role configured to $newRole")
        }
    }

    fun createAsset(assetCode: String, name: String, category: String, location: String, purchaseDate: String, description: String, imageUrl: String?, onResult: (Boolean) -> Unit) {
        val actor = _currentUser.value ?: return
        if (actor.role == "viewer") {
            showToast("Permission Denied: Viewer cannot add assets.")
            onResult(false)
            return
        }
        viewModelScope.launch {
            val asset = Asset(
                assetCode = assetCode.trim().uppercase(),
                assetName = name.trim(),
                category = category.trim(),
                location = location.trim(),
                purchaseDate = purchaseDate.trim(),
                status = "active",
                assignedTo = null,
                description = description.trim(),
                imageUrl = if (imageUrl.isNullOrBlank()) null else imageUrl.trim(),
                createdBy = actor.username
            )
            val success = repository.createAsset(actor, asset)
            if (success) {
                showToast("Asset ${asset.assetCode} created successfully!")
                onResult(true)
            } else {
                showToast("Asset Code ${asset.assetCode} already exists!")
                onResult(false)
            }
        }
    }

    fun updateAsset(asset: Asset, onResult: (Boolean) -> Unit) {
        val actor = _currentUser.value ?: return
        if (actor.role == "viewer") {
            showToast("Permission Denied: Viewer cannot update assets.")
            onResult(false)
            return
        }
        viewModelScope.launch {
            val success = repository.updateAsset(actor, asset)
            if (success) {
                showToast("Asset ${asset.assetCode} updated successfully.")
                // If it was the selected asset, refresh select reference
                if (_selectedAssetCode.value == asset.assetCode) {
                    _selectedAssetCode.value = asset.assetCode
                }
                onResult(true)
            } else {
                showToast("Failed to update asset.")
                onResult(false)
            }
        }
    }

    fun deleteAsset(asset: Asset, onCompleted: () -> Unit) {
        val actor = _currentUser.value ?: return
        if (actor.role != "admin") {
            showToast("Access Denied: Only Administrator is authorized to delete assets!")
            return
        }
        viewModelScope.launch {
            repository.deleteAsset(actor, asset)
            showToast("Asset ${asset.assetCode} removed successfully.")
            _selectedAssetCode.value = null
            onCompleted()
        }
    }

    fun assignAsset(assetCode: String, targetUsername: String, notes: String, onResult: (Boolean) -> Unit) {
        val actor = _currentUser.value ?: return
        if (actor.role == "viewer") {
            showToast("Permission Denied: Viewer cannot assign devices.")
            onResult(false)
            return
        }
        viewModelScope.launch {
            val success = repository.assignAsset(actor, assetCode, targetUsername, notes)
            if (success) {
                showToast("Asset assigned to $targetUsername.")
                onResult(true)
            } else {
                showToast("Failed to assign asset.")
                onResult(false)
            }
        }
    }

    fun returnAsset(assetCode: String, returnStatus: String, onResult: (Boolean) -> Unit) {
        val actor = _currentUser.value ?: return
        if (actor.role == "viewer") {
            showToast("Permission Denied: Viewer cannot process returns.")
            onResult(false)
            return
        }
        viewModelScope.launch {
            val success = repository.returnAsset(actor, assetCode, returnStatus)
            if (success) {
                showToast("Asset returned successfully.")
                onResult(true)
            } else {
                showToast("Failed to return asset.")
                onResult(false)
            }
        }
    }

    fun validateEmergencyRecoveryCode(code: String, onResult: (Boolean) -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val (success, message) = repository.validateInvitationCode(user.id, user.username, code.trim())
            showToast(message)
            if (success) {
                // Fetch the updated user and refresh the logged in role safely
                val updatedUser = database.userDao().getUserById(user.id)
                _currentUser.value = updatedUser
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun createRecoveryInvitationCode(codeText: String, daysOfValidity: Int, limit: Int) {
        val actor = _currentUser.value ?: return
        if (actor.role != "admin") {
            showToast("Access Denied!")
            return
        }
        viewModelScope.launch {
            val expiration = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * daysOfValidity)
            val success = repository.createInvitationCode(actor, codeText.trim().uppercase(), expiration, limit)
            if (success) {
                showToast("Emergency coupon code '$codeText' generated Successfully!")
            }
        }
    }

    fun deleteRecoveryInvitationCode(codeId: Int, codeText: String) {
        val actor = _currentUser.value ?: return
        if (actor.role != "admin") {
            showToast("Access Denied!")
            return
        }
        viewModelScope.launch {
            repository.deleteInvitationCode(actor, codeId, codeText)
            showToast("Recovery code revoked.")
        }
    }
}
