package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Asset::class,
        InvitationCode::class,
        ActivityLog::class,
        AssetTimeline::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun assetDao(): AssetDao
    abstract fun invitationCodeDao(): InvitationCodeDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun assetTimelineDao(): AssetTimelineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "asset_management_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Trigger async seeding on database creation
                        INSTANCE?.let { database ->
                            scope.launch(Dispatchers.IO) {
                                seedInitialData(database)
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            val userDao = db.userDao()
            val assetDao = db.assetDao()
            val inviteDao = db.invitationCodeDao()
            val logDao = db.activityLogDao()
            val timelineDao = db.assetTimelineDao()

            // 1. Seed Users
            val userViewer = User(
                username = "viewer",
                passwordHash = "viewer", // Easy to test plain password
                role = "viewer",
                email = "viewer@enterprise.com"
            )
            val userStaff = User(
                username = "staff",
                passwordHash = "staff",
                role = "staff",
                email = "staff@enterprise.com"
            )
            val userAdmin = User(
                username = "admin",
                passwordHash = "admin",
                role = "admin",
                email = "admin@enterprise.com"
            )
            userDao.insertUser(userViewer)
            userDao.insertUser(userStaff)
            userDao.insertUser(userAdmin)

            // 2. Seed Assets
            val assets = listOf(
                Asset(
                    assetCode = "AST-2026-001",
                    assetName = "MacBook Pro 16\" M3 Max",
                    category = "Electronics",
                    location = "HQ Office - Room 302",
                    purchaseDate = "2026-01-15",
                    status = "active",
                    assignedTo = "staff",
                    description = "M3 Max processor, 64GB RAM, 2TB SSD initialized for iOS/Android software development.",
                    imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=400&q=80",
                    createdBy = "system"
                ),
                Asset(
                    assetCode = "AST-2026-002",
                    assetName = "Dell UltraSharp 32\" 4K Screen",
                    category = "Electronics",
                    location = "HQ Office - Open Space",
                    purchaseDate = "2026-02-12",
                    status = "maintenance",
                    assignedTo = null,
                    description = "4K professional designer monitor. Screen undergoing backlight flicker inspection.",
                    imageUrl = "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=400&q=80",
                    createdBy = "system"
                ),
                Asset(
                    assetCode = "AST-2026-003",
                    assetName = "Herman Miller Aeron Chair",
                    category = "Furniture",
                    location = "HQ Office - Lead Studio",
                    purchaseDate = "2025-08-10",
                    status = "active",
                    assignedTo = "viewer",
                    description = "Ergonomic workspace office mesh chair, graphite color size B.",
                    imageUrl = null,
                    createdBy = "system"
                ),
                Asset(
                    assetCode = "AST-2026-004",
                    assetName = "Sony WH-1000XM5 Headphones",
                    category = "Audio Equipment",
                    location = "Remote Supply Cabinet B",
                    purchaseDate = "2026-03-01",
                    status = "borrowed",
                    assignedTo = "staff",
                    description = "Active noise cancelling wireless business headphones.",
                    imageUrl = null,
                    createdBy = "system"
                ),
                Asset(
                    assetCode = "AST-2026-005",
                    assetName = "Epson L3210 Printer",
                    category = "Office Equipment",
                    location = "HQ Reception Hall",
                    purchaseDate = "2024-11-20",
                    status = "broken",
                    assignedTo = null,
                    description = "Color inkjet multifunction printer. Printhead clogged; scheduled for retirement review.",
                    imageUrl = null,
                    createdBy = "system"
                )
            )
            for (asset in assets) {
                assetDao.insertAsset(asset)
                timelineDao.insertTimeline(
                    AssetTimeline(
                        assetCode = asset.assetCode,
                        action = "created",
                        changedBy = "system",
                        detail = "Initial database asset initialization."
                    )
                )
            }

            // 3. Seed Invitation Codes
            inviteDao.insertInvitationCode(
                InvitationCode(
                    code = "RECOVER-ADMIN-99",
                    expiresAt = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30, // 30 days
                    usageLimit = 5,
                    usageCount = 0,
                    isActive = true
                )
            )
            inviteDao.insertInvitationCode(
                InvitationCode(
                    code = "AISTUDIO-500",
                    expiresAt = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 10, // 10 days
                    usageLimit = 1,
                    usageCount = 1, // Already used, should fails usageLimit if 1
                    isActive = true
                )
            )
            inviteDao.insertInvitationCode(
                InvitationCode(
                    code = "EXPIRED-TOKEN-12",
                    expiresAt = System.currentTimeMillis() - 1000L * 60, // Expired 1 minute ago
                    usageLimit = 10,
                    usageCount = 0,
                    isActive = true
                )
            )

            // 4. Seed Activity Logs
            val initialLogs = listOf(
                ActivityLog(
                    username = "system",
                    role = "admin",
                    action = "INITIALIZE_SYSTEM",
                    detail = "Asset Management System database context initialized."
                ),
                ActivityLog(
                    username = "system",
                    role = "admin",
                    action = "SEED_DEMO",
                    detail = "Prepopulated 3 user accounts (admin, staff, viewer) and 5 sample tracking assets."
                )
            )
            for (log in initialLogs) {
                logDao.insertLog(log)
            }
        }
    }
}
