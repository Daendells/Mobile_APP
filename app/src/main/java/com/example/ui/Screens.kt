package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AssetViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val apiLogs by viewModel.apiLogs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Display Toast Messages safely via Snackbar
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    var showApiConsole by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        snackbarHostStatus = snackbarHostState,
        bottomBar = {
            if (currentUser != null) {
                BottomNavigationWidget(
                    currentScreen = currentScreen,
                    userRole = currentUser?.role ?: "viewer",
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        },
        floatingActionButton = {
            // Floating Console Button so the user can inspect simulated PHP/Laravel REST payloads at any time
            FloatingActionButton(
                onClick = { showApiConsole = !showApiConsole },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.testTag("api_console_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Simulated PHP API Monitor"
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switching using smooth state animations
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (currentScreen) {
                    Screen.LOGIN -> LoginScreen(viewModel)
                    Screen.DASHBOARD -> DashboardScreen(viewModel, isTablet)
                    Screen.ASSET_LIST -> AssetListScreen(viewModel, isTablet)
                    Screen.ASSET_DETAIL -> AssetDetailScreen(viewModel)
                    Screen.USER_MANAGEMENT -> UserManagementScreen(viewModel)
                    Screen.INVITATION_CODES -> InvitationCodesScreen(viewModel)
                    Screen.ACTIVITY_LOGS -> ActivityLogsScreen(viewModel)
                    Screen.API_CONSOLE -> ApiConsoleContent(apiLogs) { viewModel.navigateTo(Screen.DASHBOARD) }
                }
            }
        }

        // Full Screen Overlay for API inspection
        if (showApiConsole) {
            Dialog(onDismissRequest = { showApiConsole = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                    color = Color(0xFF1E1E1E) // Dark developer terminal background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2C2C2C))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Php,
                                        contentDescription = "Laravel backend Logo",
                                        tint = Color(0xFFFF2D20), // Laravel Red
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Laravel / PHP Native REST API Simulator",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Capturing client HTTP calls natively in real-time",
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                                IconButton(onClick = { showApiConsole = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Terminal",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Body API Logs
                            ApiConsoleContent(apiLogs = apiLogs, onBack = null)
                        }
                    }
                }
            }
        }
    }
}

// Wrapper to prevent older compose-bom issues
@Composable
fun Scaffold(
    snackbarHostStatus: SnackbarHostState,
    bottomBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostStatus) },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        content = content
    )
}

// --- DYNAMIC STATE BOTTOM NAVIGATION ---
@Composable
fun BottomNavigationWidget(
    currentScreen: Screen,
    userRole: String,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        tonalElevation = 6.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { onNavigate(Screen.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
            label = { Text("Dashboard") },
            modifier = Modifier.testTag("nav_dashboard")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.ASSET_LIST || currentScreen == Screen.ASSET_DETAIL,
            onClick = { onNavigate(Screen.ASSET_LIST) },
            icon = { Icon(Icons.Default.Inventory, "Assets") },
            label = { Text("Assets") },
            modifier = Modifier.testTag("nav_assets")
        )
        if (userRole == "admin") {
            NavigationBarItem(
                selected = currentScreen == Screen.USER_MANAGEMENT,
                onClick = { onNavigate(Screen.USER_MANAGEMENT) },
                icon = { Icon(Icons.Default.People, "Users") },
                label = { Text("Users") },
                modifier = Modifier.testTag("nav_users")
            )
            NavigationBarItem(
                selected = currentScreen == Screen.INVITATION_CODES,
                onClick = { onNavigate(Screen.INVITATION_CODES) },
                icon = { Icon(Icons.Default.VpnKey, "Recovery") },
                label = { Text("Code Admin") },
                modifier = Modifier.testTag("nav_invite_codes")
            )
            NavigationBarItem(
                selected = currentScreen == Screen.ACTIVITY_LOGS,
                onClick = { onNavigate(Screen.ACTIVITY_LOGS) },
                icon = { Icon(Icons.Default.History, "Audit") },
                label = { Text("AuditLogs") },
                modifier = Modifier.testTag("nav_audit_logs")
            )
        }
    }
}

// --- SCREEN 1: THE LOGIN & REGISTER PORTAL ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AssetViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Premium Brand Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = "Asset Logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ASSET LOOP",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Interactive Asset Tracking Ecosystem",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Authentication Card
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegisterMode) "Create Free Account" else "Sign In to Workspace",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Person, "User") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, "Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (loading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank() || (isRegisterMode && email.isBlank())) {
                                    viewModel.showToast("Please fill all fields!")
                                    return@Button
                                }
                                loading = true
                                if (isRegisterMode) {
                                    viewModel.register(username.trim().lowercase(), email.trim(), password) {
                                        loading = false
                                    }
                                } else {
                                    viewModel.login(username.trim().lowercase(), password) {
                                        loading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isRegisterMode) "Register Default Viewer" else "Authenticate Securely",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch Mode Text
                    TextButton(
                        onClick = { isRegisterMode = !isRegisterMode }
                    ) {
                        Text(
                            text = if (isRegisterMode) "Already have an account? Sign In" else "New User? Register viewer account",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Easy Credentials Prefill Pane - EXTREMELY HIGH VALUE PORTFOLIO UX
            if (!isRegisterMode) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Demo Credentials (Instant Prefill)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "admin" to MaterialTheme.colorScheme.errorContainer,
                        "staff" to Color(0xFFFFF3CD),
                        "viewer" to MaterialTheme.colorScheme.secondaryContainer
                    ).forEach { (role, color) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = color),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    username = role
                                    password = role
                                },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = role.uppercase(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 2: THE MODERN ENTERPRISE DASHBOARD ===
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: AssetViewModel, isTablet: Boolean) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val assets by viewModel.allAssets.collectAsStateWithLifecycle()
    val logs by viewModel.activityLogs.collectAsStateWithLifecycle()

    var showEmergencyCodeDialog by remember { mutableStateOf(false) }

    // Statistics formulas
    val totalAssets = assets.size
    val activeAssets = assets.count { it.status == "active" }
    val borrowedAssets = assets.count { it.status == "borrowed" }
    val maintenanceAssets = assets.count { it.status == "maintenance" }
    val brokenAssets = assets.count { it.status == "broken" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Widget
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Hello, ${currentUser?.username ?: "Corporate User"}!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Ecosystem Role: ",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            BadgeRole(role = currentUser?.role ?: "viewer")
                        }
                    }

                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, "Exit")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Out")
                    }
                }
            }
        }

        // Metrics Summary Panels in Grid
        item {
            Text(
                text = "Operational Asset Statistics",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val boxWidth = if (isTablet) 120.dp else 100.dp
                StatCard(title = "Total Assets", count = totalAssets, color = MaterialTheme.colorScheme.secondary, icon = Icons.Default.Inventory, modifier = Modifier.weight(1f))
                StatCard(title = "Active", count = activeAssets, color = Color(0xFF4CAF50), icon = Icons.Default.CheckCircle, modifier = Modifier.weight(1f))
                StatCard(title = "Borrowed", count = borrowedAssets, color = Color(0xFF2196F3), icon = Icons.Default.DirectionsCar, modifier = Modifier.weight(1f))
                StatCard(title = "Maintenance", count = maintenanceAssets, color = Color(0xFFFFC107), icon = Icons.Default.Build, modifier = Modifier.weight(1f))
                StatCard(title = "Broken", count = brokenAssets, color = Color(0xFFF44336), icon = Icons.Default.ReportProblem, modifier = Modifier.weight(1f))
            }
        }

        // Emergency Recovery Action Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security Alert",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Emergency Admin Recovery System",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "If no admin workspace accounts are accessible, insert a recovery activation token. Validation will elevate your role instantly to Admin.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showEmergencyCodeDialog = true },
                        modifier = Modifier.testTag("emergency_recovery_button")
                    ) {
                        Text("Insert Recovery Token")
                    }
                }
            }
        }

        // Quick Navigation Links
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Direct Shortcuts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.ASSET_LIST) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Search, "Find")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Browse Assets", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (currentUser?.role == "admin") {
                            Button(
                                onClick = { viewModel.navigateTo(Screen.USER_MANAGEMENT) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Security, "Shield")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Promote User", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Audited Platform Activity Log Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Operations Log",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (currentUser?.role == "admin") {
                    TextButton(onClick = { viewModel.navigateTo(Screen.ACTIVITY_LOGS) }) {
                        Text("View Audit Logs")
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities registered in system ledger.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        logs.take(5).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (log.action) {
                                        "LOGIN" -> Icons.Default.Login
                                        "CREATE_ASSET" -> Icons.Default.AddBox
                                        "UPDATE_ASSET" -> Icons.Default.Edit
                                        "DELETE_ASSET" -> Icons.Default.Delete
                                        "ROLE_PROMOTION_DEMOTION" -> Icons.Default.TrendingUp
                                        "EMERGENCY_RECOVERY" -> Icons.Default.VerifiedUser
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = "Event icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = log.detail, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${log.username} (${log.role.uppercase()}) • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    // Interactive Emergency recovery Code modal dialogue
    if (showEmergencyCodeDialog) {
        var tokenInput by remember { mutableStateOf("") }
        var inProgress by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!inProgress) showEmergencyCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, "Key", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Elevation")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter the dynamic key (e.g., 'RECOVER-ADMIN-99' generated by default seeding) to bypass access controls. This acts as a database-verified security reset.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Invitation / Recovery Code") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tokenInput.isBlank()) return@Button
                        inProgress = true
                        viewModel.validateEmergencyRecoveryCode(tokenInput) {
                            inProgress = false
                            showEmergencyCodeDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_recovery_button")
                ) {
                    Text("Verify & Elevate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyCodeDialog = false }) {
                    Text("Back")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.height(105.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = count.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- SCREEN 3: ASSET MASTER LIST AND FILTERS ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetListScreen(viewModel: AssetViewModel, isTablet: Boolean) {
    val assets by viewModel.filteredAssets.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }

    val categories = listOf("Electronics", "Furniture", "Audio Equipment", "Office Equipment")
    val statuses = listOf("active", "maintenance", "borrowed", "broken", "retired")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Enterprise Asset Registry",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar Component
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Search by asset name, code, description...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("asset_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Categories Selector Panel
            Text(text = "Filter Categories", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // ALL filter item
                FilterChip(
                    selected = categoryFilter.isEmpty(),
                    onClick = { viewModel.categoryFilter.value = "" },
                    label = { Text("All", fontSize = 11.sp) },
                    modifier = Modifier.testTag("category_all_chip")
                )
                categories.take(3).forEach { category ->
                    FilterChip(
                        selected = categoryFilter == category,
                        onClick = { viewModel.categoryFilter.value = category },
                        label = { Text(category, fontSize = 11.sp) },
                        modifier = Modifier.testTag("category_${category.lowercase().replace(" ","_")}_chip")
                    )
                }
            }

            // Life cycle stages Filter Layout
            Text(text = "Filter Lifecycle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                FilterChip(
                    selected = statusFilter.isEmpty(),
                    onClick = { viewModel.statusFilter.value = "" },
                    label = { Text("All", fontSize = 11.sp) }
                )
                statuses.take(4).forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { viewModel.statusFilter.value = status },
                        label = { Text(status.uppercase(), fontSize = 11.sp) }
                    )
                }
            }

            // Sort Selector dropdown simulator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Found ${assets.size} matches",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sort: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    listOf(
                        "newest" to "Newest",
                        "name_asc" to "A-Z"
                    ).forEach { (sortKey, label) ->
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (sortBy == sortKey) FontWeight.Bold else FontWeight.Normal,
                            color = if (sortBy == sortKey) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clickable { viewModel.sortBy.value = sortKey }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main List Render
            if (assets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, "No Item", modifier = Modifier.size(56.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No assets match current filter attributes.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(assets) { asset ->
                        AssetItemCard(asset = asset) {
                            viewModel.selectAssetAndNavigate(asset.assetCode)
                        }
                    }
                }
            }
        }

        // Add Asset float button for Staff & Admin roles only
        if (currentUser?.role != "viewer") {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp) // Offset above navigation bars
                    .testTag("add_asset_fab")
            ) {
                Icon(Icons.Default.Add, "Add Asset")
            }
        }
    }

    // Modal dialogue to create custom Assets
    if (showCreateDialog) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Electronics") }
        var location by remember { mutableStateOf("") }
        var purchaseDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var description by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Register New Corporate Asset") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Asset Code (Unique)") },
                            placeholder = { Text("e.g. AST-XYZ-123") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_asset_code")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Asset Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_asset_name")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Text("Category selection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.take(2).forEach { cardOption ->
                                FilterChip(
                                    selected = category == cardOption,
                                    onClick = { category = cardOption },
                                    label = { Text(cardOption, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.drop(2).forEach { cardOption ->
                                FilterChip(
                                    selected = category == cardOption,
                                    onClick = { category = cardOption },
                                    label = { Text(cardOption, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Physical Location") },
                            placeholder = { Text("e.g. HQ 3rd Floor, Cabinet-2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        OutlinedTextField(
                            value = purchaseDate,
                            onValueChange = { purchaseDate = it },
                            label = { Text("Purchase Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Asset Spec / Remark Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Custom image URL (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (code.isBlank() || name.isBlank()) {
                            viewModel.showToast("Code and Name fields are mandatory!")
                            return@Button
                        }
                        viewModel.createAsset(
                            assetCode = code,
                            name = name,
                            category = category,
                            location = location,
                            purchaseDate = purchaseDate,
                            description = description,
                            imageUrl = imageUrl
                        ) { success ->
                            if (success) showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_create_asset")
                ) {
                    Text("Register Asset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AssetItemCard(asset: Asset, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("asset_card_${asset.assetCode}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant category glyph placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (asset.category.lowercase()) {
                        "electronics" -> Icons.Default.LaptopMac
                        "furniture" -> Icons.Default.Chair
                        "audio equipment" -> Icons.Default.Headphones
                        else -> Icons.Default.Hardware
                    },
                    contentDescription = asset.category,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = asset.assetCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    BadgeStatus(status = asset.status)
                }

                Text(
                    text = asset.assetName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(Icons.Default.LocationOn, "Loc", tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = asset.location,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (asset.assignedTo != null) {
                    Text(
                        text = "Assigned to: @${asset.assignedTo}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// --- SCREEN 4: ASSET INDEPTH PROPERTY SUMMARY & REGISTRY TIMELINE ===
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssetDetailScreen(viewModel: AssetViewModel) {
    val asset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val timeline by viewModel.selectedAssetTimeline.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showAssignDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }

    if (asset == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Retrieving asset ledger detail...")
            }
        }
        return
    }

    val currentAsset = asset!!

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back toolbar navigation header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.ASSET_LIST) }) {
                    Icon(Icons.Default.ArrowBack, "Back list")
                }
                Column {
                    Text(
                        text = "Asset Specifications",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(text = currentAsset.assetCode, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Core visual presentation & primary details
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentAsset.assetName,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Category: ${currentAsset.category}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        BadgeStatus(status = currentAsset.status)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = currentAsset.description, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location / Purchase info grid
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            LabelValue(label = "Physical Location", value = currentAsset.location)
                            Spacer(modifier = Modifier.height(8.dp))
                            LabelValue(label = "Purchase Date", value = currentAsset.purchaseDate)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            LabelValue(
                                label = "Current Holder",
                                value = if (currentAsset.assignedTo != null) "@${currentAsset.assignedTo}" else "Unassigned (In Pool)"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LabelValue(label = "Created By", value = currentAsset.createdBy)
                        }
                    }
                }
            }
        }

        // Assignment Execution Buttons (RBAC Protected: Viewers are not allowed)
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Security Assignment Controls", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser?.role == "viewer") {
                        Text(
                            text = "🔒 Your 'Viewer' privileges do not allow editing or assigning corporate resources.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Assign action button triggers if operational
                            Button(
                                onClick = { showAssignDialog = true },
                                enabled = currentAsset.status == "active" || currentAsset.status == "maintenance",
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("action_assign")
                            ) {
                                Icon(Icons.Default.AssignmentInd, "Handover")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Handover (Assign)")
                            }

                            // Return action button triggers if borrowed
                            Button(
                                onClick = { showReturnDialog = true },
                                enabled = currentAsset.assignedTo != null,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.testTag("action_return")
                            ) {
                                Icon(Icons.Default.AssignmentReturn, "Return")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Return Asset")
                            }
                        }
                    }
                }
            }
        }

        // System Asset Timeline Ledger Tracking (Audit log)
        item {
            Text(
                text = "Tracking History & Audit Ledger",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (timeline.isEmpty()) {
                Text("No previous transfers recorded on this asset.", color = Color.Gray, fontSize = 12.sp)
            } else {
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        timeline.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .align(Alignment.Top)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.action.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = event.detail, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                                    Text(
                                        text = "By ${event.changedBy} • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(event.timestamp))}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // Admin Only Danger Zone (Remove asset ledger entirely)
        if (currentUser?.role == "admin") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Admin Danger Zone",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Removing this element takes it completely out of DB inventory catalog records. This action cannot be undone.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.deleteAsset(currentAsset) {
                                    viewModel.navigateTo(Screen.ASSET_LIST)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("action_delete_asset")
                        ) {
                            Icon(Icons.Default.DeleteForever, "Remove")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Asset Globally")
                        }
                    }
                }
            }
        }
    }

    // Assign Dialog
    if (showAssignDialog) {
        var assigneeText by remember { mutableStateOf("") }
        var remarkText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assign/Handover Asset") },
            text = {
                Column {
                    Text(
                        text = "Designate a staff member or user in charge of tracking this device.",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = assigneeText,
                        onValueChange = { assigneeText = it },
                        label = { Text("Assignee Username") },
                        placeholder = { Text("e.g. staff") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("assignee_input_field")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        label = { Text("Remarks (Notes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (assigneeText.isBlank()) return@Button
                        viewModel.assignAsset(currentAsset.assetCode, assigneeText.trim(), remarkText.trim()) { success ->
                            if (success) showAssignDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_assign_button")
                ) {
                    Text("Confirm Handover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Return Dialog
    if (showReturnDialog) {
        var statusAfterReturn by remember { mutableStateOf("active") }

        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Process Asset Return") },
            text = {
                Column {
                    Text("The asset was delivered back. Please evaluate the physical operational status:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        "active" to "Active (Ready to go)",
                        "maintenance" to "Maintenance Required",
                        "broken" to "Broken / Damaged"
                    ).forEach { (stat, labelText) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { statusAfterReturn = stat }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = statusAfterReturn == stat, onClick = { statusAfterReturn = stat })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(labelText, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.returnAsset(currentAsset.assetCode, statusAfterReturn) { success ->
                            if (success) showReturnDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_return_button")
                ) {
                    Text("Confirm Return")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- SCREEN 5: ADMIN WORKSPACE — INTERACTIVE ROLE PROMOTIONS ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(viewModel: AssetViewModel) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    if (currentUser?.role != "admin") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Block, "Restricted", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Text("RESTRICTED: Admin Only Workspace.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }

    val filteredList = users.filter {
        it.username.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "User Credentials & Role Promoted Consoles",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = "Modify operational clearances of enterprise staff instantly", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search users to promote...") },
            leadingIcon = { Icon(Icons.Default.Search, "Find user") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No user matches standard criteria")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { member ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = member.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = member.email, fontSize = 11.sp, color = Color.Gray)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text("Current Security: ", fontSize = 10.sp)
                                    BadgeRole(role = member.role)
                                }
                            }

                            // Dynamic Switch Role options
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("viewer", "staff", "admin").forEach { roleVal ->
                                    val isSelected = member.role == roleVal
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (member.id == currentUser?.id) {
                                                    viewModel.showToast("Cannot demote your own active account state!")
                                                    return@clickable
                                                }
                                                viewModel.promoteUser(member.id, member.username, roleVal)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("btn_to_${member.username}_${roleVal}")
                                    ) {
                                        Text(
                                            text = roleVal.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 6: RECOVERY INVITATION CODES MANAGER ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvitationCodesScreen(viewModel: AssetViewModel) {
    val inviteCodes by viewModel.invitationCodes.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var customCodeText by remember { mutableStateOf("") }
    var expirationDays by remember { mutableStateOf("30") }
    var maxUsageLimit by remember { mutableStateOf("5") }

    if (currentUser?.role != "admin") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Block, "Restricted", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Text("RESTRICTED: Admin Only Workspace.", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Recovery Token Configuration Console",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Configure backup invitation codes to recover loss of admin instances.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // Generate invitation code card
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Generate Custom Recovery Code", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCodeText,
                        onValueChange = { customCodeText = it },
                        label = { Text("Code Key (e.g., RESTORE-ADMIN)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gen_code_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expirationDays,
                            onValueChange = { expirationDays = it },
                            label = { Text("Active Duration (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxUsageLimit,
                            onValueChange = { maxUsageLimit = it },
                            label = { Text("Uses Allowed") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gen_usage_limit_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (customCodeText.isBlank()) {
                                viewModel.showToast("Code Key string cannot be blank!")
                                return@Button
                            }
                            val days = expirationDays.toIntOrNull() ?: 30
                            val lim = maxUsageLimit.toIntOrNull() ?: 5
                            viewModel.createRecoveryInvitationCode(customCodeText, days, lim)
                            customCodeText = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_submit_invite_code")
                    ) {
                        Text("Register Code in Database")
                    }
                }
            }
        }

        // List active invitation codes
        item {
            Text("Active Keys in Database", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (inviteCodes.isEmpty()) {
            item {
                Text("No invitation recovery keys stored.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            items(inviteCodes) { codeObj ->
                val isExpired = System.currentTimeMillis() > codeObj.expiresAt
                val limitReached = codeObj.usageCount >= codeObj.usageLimit
                val formatPattern = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = codeObj.code,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Status tags
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isExpired) {
                                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                        Text("Expired", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 9.sp, modifier = Modifier.padding(2.dp))
                                    }
                                } else if (limitReached) {
                                    Badge(containerColor = Color.LightGray) {
                                        Text("Used Up", fontSize = 9.sp, modifier = Modifier.padding(2.dp))
                                    }
                                } else {
                                    Badge(containerColor = Color(0xFFE8F5E9)) {
                                        Text("Operational", color = Color(0xFF2E7D32), fontSize = 9.sp, modifier = Modifier.padding(2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Expires: ${formatPattern.format(Date(codeObj.expiresAt))}", fontSize = 11.sp)
                                Text(text = "Redemptions: ${codeObj.usageCount} / ${codeObj.usageLimit} Max uses", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = { viewModel.deleteRecoveryInvitationCode(codeObj.id, codeObj.code) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, "Revoke")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 7: SYSTEM AUDIT LEDGERS ===
@Composable
fun ActivityLogsScreen(viewModel: AssetViewModel) {
    val databaseLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    if (currentUser?.role != "admin") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("RESTRICTED: Administrators Only.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workspace Security Audit Logs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { viewModel.navigateTo(Screen.DASHBOARD) }) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        }
        Text(text = "Complete list of system activities recorded in physical ledger", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(12.dp))

        if (databaseLogs.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Ledger is completely empty.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(databaseLogs) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (log.action) {
                                            "LOGIN" -> Color(0xFFE3F2FD)
                                            "EMERGENCY_RECOVERY" -> Color(0xFFFFEBEE)
                                            "CREATE_ASSET" -> Color(0xFFE8F5E9)
                                            else -> Color(0xFFF5F5F5)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (log.action) {
                                        "LOGIN" -> Icons.Default.LockOpen
                                        "EMERGENCY_RECOVERY" -> Icons.Default.VerifiedUser
                                        "CREATE_ASSET" -> Icons.Default.Add
                                        "DELETE_ASSET" -> Icons.Default.Delete
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = log.action,
                                    tint = when (log.action) {
                                        "LOGIN" -> Color(0xFF1E88E5)
                                        "EMERGENCY_RECOVERY" -> Color(0xFFE53935)
                                        "CREATE_ASSET" -> Color(0xFF43A047)
                                        else -> Color.DarkGray
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = log.detail, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "By @${log.username} (${log.role.uppercase()})",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TERMINAL WINDOW: THE NATIVE LARAVEL / PHP API VISUAL LOGGER ===
@Composable
fun ApiConsoleContent(
    apiLogs: List<ApiLogEntry>,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Pure terminal dark
            .padding(12.dp)
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "API REST INTERCEPTOR LOGS",
                    color = Color(0xFF00FF00), // Lime Terminal Color
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.Green)
                }
            }
        }

        if (apiLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Listening on port :80... (Perform actions in-app to generate local Eloquent model traces)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(apiLogs) { log ->
                val isSuccessStatus = log.status.startsWith("20")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    // Method and Status bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Method badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = when (log.method) {
                                            "POST" -> Color(0xFF4CAF50)
                                            "PUT" -> Color(0xFFFF9800)
                                            "DELETE" -> Color(0xFFF44336)
                                            else -> Color(0xFF2196F3)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = log.method,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = log.endpoint,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // HTTP status indicator
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSuccessStatus) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = log.status,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Timestamp: ${log.formattedTime} • Payload logs schema:",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Request Data Body
                    if (log.requestBody != null) {
                        Text(text = "▶ REQUEST BODY (JSON)", color = Color(0xFFFF9800), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF151515))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = log.requestBody,
                                color = Color(0xFFECEFF1),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Response Data Body
                    if (log.responseBody != null) {
                        Text(text = "◀ PHP API RESPONSE JSON", color = Color(0xFF2196F3), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF151515))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = log.responseBody,
                                color = Color(0xFF00FF00),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SHARED HELPER CHIPS & BADGES ---

@Composable
fun BadgeRole(role: String) {
    val containerColor = when (role.trim().lowercase()) {
        "admin" -> MaterialTheme.colorScheme.errorContainer
        "staff" -> Color(0xFFFFF3CD) // Fallback Warning container
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (role.trim().lowercase()) {
        "admin" -> MaterialTheme.colorScheme.onErrorContainer
        "staff" -> Color(0xFF856404) // Fallback Warning content
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = role.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = contentColor
        )
    }
}

// Quick custom override since compiler might complain on certain custom color tokens
@Composable
fun BadgeStatus(status: String) {
    val colorPair = when (status.lowercase()) {
        "active" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)      // Green
        "maintenance" -> Color(0xFFFFFDE7) to Color(0xFFF57F17) // Yellow
        "borrowed" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)    // Blue
        "broken" -> Color(0xFFFFEBEE) to Color(0xFFC62828)      // Red
        else -> Color(0xFFEEEEEE) to Color(0xFF616161)          // Grey (retired)
    }

    Box(
        modifier = Modifier
            .border(width = 1.dp, color = colorPair.second, shape = RoundedCornerShape(12.dp))
            .background(color = colorPair.first, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = colorPair.second,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LabelValue(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
