package com.example.videodownloader.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Data Models for Database Entities
// ---------------------------------------------------------------------------

data class UserEntity(
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val age: Int,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
)

data class BookmarkEntity(
    val id: Long = 0,
    val title: String,
    val url: String,
    val format: String = "MP4",
    val duration: String = "--:--",
    val dateAdded: String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
)

data class DownloadHistoryEntity(
    val jobId: String,
    val title: String,
    val url: String,
    val fileType: String,
    val format: String,
    val totalBytes: Long,
    val localPath: String?,
    val status: String,
    val dateAdded: String
)

// ---------------------------------------------------------------------------
// Native SQLite Database Helper
// Implements complete database connectivity with ACID transactions,
// prepared queries, indices, and CRUD operations.
// ---------------------------------------------------------------------------

class AppDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "AppDatabaseHelper"
        private const val DATABASE_NAME = "video_downloader.db"
        private const val DATABASE_VERSION = 1

        // ── Users Table ──
        const val TABLE_USERS = "users"
        const val COL_USER_ID = "id"
        const val COL_USER_NAME = "full_name"
        const val COL_USER_EMAIL = "email"
        const val COL_USER_PASSWORD = "password_hash"
        const val COL_USER_AGE = "age"
        const val COL_USER_CREATED_AT = "created_at"

        // ── Bookmarks Table ──
        const val TABLE_BOOKMARKS = "bookmarks"
        const val COL_BM_ID = "id"
        const val COL_BM_TITLE = "title"
        const val COL_BM_URL = "url"
        const val COL_BM_FORMAT = "format"
        const val COL_BM_DURATION = "duration"
        const val COL_BM_DATE = "date_added"

        // ── Download History Table ──
        const val TABLE_DOWNLOADS = "download_history"
        const val COL_DL_ID = "job_id"
        const val COL_DL_TITLE = "title"
        const val COL_DL_URL = "url"
        const val COL_DL_FILE_TYPE = "file_type"
        const val COL_DL_FORMAT = "format"
        const val COL_DL_TOTAL_BYTES = "total_bytes"
        const val COL_DL_LOCAL_PATH = "local_path"
        const val COL_DL_STATUS = "status"
        const val COL_DL_DATE = "date_added"

        @Volatile
        private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context).also { instance = it }
            }
        }

        fun hashPassword(password: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Users Table
        val createUsersSql = """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_NAME TEXT NOT NULL,
                $COL_USER_EMAIL TEXT UNIQUE NOT NULL,
                $COL_USER_PASSWORD TEXT NOT NULL,
                $COL_USER_AGE INTEGER NOT NULL,
                $COL_USER_CREATED_AT TEXT NOT NULL
            )
        """.trimIndent()

        // Create Bookmarks Table
        val createBookmarksSql = """
            CREATE TABLE $TABLE_BOOKMARKS (
                $COL_BM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BM_TITLE TEXT NOT NULL,
                $COL_BM_URL TEXT NOT NULL,
                $COL_BM_FORMAT TEXT NOT NULL,
                $COL_BM_DURATION TEXT NOT NULL,
                $COL_BM_DATE TEXT NOT NULL
            )
        """.trimIndent()

        // Create Download History Table
        val createDownloadsSql = """
            CREATE TABLE $TABLE_DOWNLOADS (
                $COL_DL_ID TEXT PRIMARY KEY,
                $COL_DL_TITLE TEXT NOT NULL,
                $COL_DL_URL TEXT NOT NULL,
                $COL_DL_FILE_TYPE TEXT NOT NULL,
                $COL_DL_FORMAT TEXT NOT NULL,
                $COL_DL_TOTAL_BYTES INTEGER NOT NULL,
                $COL_DL_LOCAL_PATH TEXT,
                $COL_DL_STATUS TEXT NOT NULL,
                $COL_DL_DATE TEXT NOT NULL
            )
        """.trimIndent()

        db.execSQL(createUsersSql)
        db.execSQL(createBookmarksSql)
        db.execSQL(createDownloadsSql)

        // Add indices for fast lookup
        db.execSQL("CREATE INDEX idx_user_email ON $TABLE_USERS ($COL_USER_EMAIL);")
        db.execSQL("CREATE INDEX idx_bookmark_url ON $TABLE_BOOKMARKS ($COL_BM_URL);")

        Log.d(TAG, "Database tables created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOADS")
        onCreate(db)
    }

    // =======================================================================
    // User Authentication & Registration (CRUD)
    // =======================================================================

    /**
     * Inserts a newly registered user into the SQLite database.
     * @return ID of inserted user or -1 on failure
     */
    fun registerUser(user: UserEntity): Long {
        val db = writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COL_USER_NAME, user.fullName.trim())
                put(COL_USER_EMAIL, user.email.trim().lowercase())
                put(COL_USER_PASSWORD, user.passwordHash)
                put(COL_USER_AGE, user.age)
                put(COL_USER_CREATED_AT, user.createdAt)
            }
            db.insert(TABLE_USERS, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user: ${user.email}", e)
            -1L
        }
    }

    /**
     * Checks if email already exists in database.
     */
    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                TABLE_USERS,
                arrayOf(COL_USER_ID),
                "$COL_USER_EMAIL = ?",
                arrayOf(email.trim().lowercase()),
                null, null, null
            )
            cursor.moveToFirst()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking email", e)
            false
        } finally {
            cursor?.close()
        }
    }

    /**
     * Authenticates user against SQLite database.
     * Returns UserEntity if valid credentials, null otherwise.
     */
    fun authenticateUser(email: String, rawPassword: String): UserEntity? {
        val db = readableDatabase
        val passwordHash = hashPassword(rawPassword)
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                TABLE_USERS,
                arrayOf(COL_USER_ID, COL_USER_NAME, COL_USER_EMAIL, COL_USER_PASSWORD, COL_USER_AGE, COL_USER_CREATED_AT),
                "$COL_USER_EMAIL = ? AND $COL_USER_PASSWORD = ?",
                arrayOf(email.trim().lowercase(), passwordHash),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                UserEntity(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                    fullName = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL)),
                    passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PASSWORD)),
                    age = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_AGE)),
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CREATED_AT))
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication query error", e)
            null
        } finally {
            cursor?.close()
        }
    }

    fun getUserByEmail(email: String): UserEntity? {
        val db = readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                TABLE_USERS,
                arrayOf(COL_USER_ID, COL_USER_NAME, COL_USER_EMAIL, COL_USER_PASSWORD, COL_USER_AGE, COL_USER_CREATED_AT),
                "$COL_USER_EMAIL = ?",
                arrayOf(email.trim().lowercase()),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                UserEntity(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                    fullName = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL)),
                    passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PASSWORD)),
                    age = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_AGE)),
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CREATED_AT))
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by email", e)
            null
        } finally {
            cursor?.close()
        }
    }

    // =======================================================================
    // Bookmarks Module (CRUD)
    // =======================================================================

    fun addBookmark(bookmark: BookmarkEntity): Long {
        val db = writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COL_BM_TITLE, bookmark.title)
                put(COL_BM_URL, bookmark.url)
                put(COL_BM_FORMAT, bookmark.format)
                put(COL_BM_DURATION, bookmark.duration)
                put(COL_BM_DATE, bookmark.dateAdded)
            }
            db.insert(TABLE_BOOKMARKS, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting bookmark", e)
            -1L
        }
    }

    fun getAllBookmarks(): List<BookmarkEntity> {
        val list = mutableListOf<BookmarkEntity>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                TABLE_BOOKMARKS,
                null, null, null, null, null,
                "$COL_BM_ID DESC"
            )
            while (cursor.moveToNext()) {
                list.add(
                    BookmarkEntity(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_BM_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_BM_TITLE)),
                        url = cursor.getString(cursor.getColumnIndexOrThrow(COL_BM_URL)),
                        format = cursor.getString(cursor.getColumnIndexOrThrow(COL_BM_FORMAT)),
                        duration = cursor.getString(cursor.getColumnIndexOrThrow(COL_BM_DURATION)),
                        dateAdded = cursor.getString(cursor.getColumnIndexOrThrow(COL_BM_DATE))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bookmarks", e)
        } finally {
            cursor?.close()
        }
        return list
    }

    fun deleteBookmark(id: Long): Boolean {
        val db = writableDatabase
        return try {
            val rows = db.delete(TABLE_BOOKMARKS, "$COL_BM_ID = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting bookmark", e)
            false
        }
    }

    fun isUrlBookmarked(url: String): Boolean {
        val db = readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                TABLE_BOOKMARKS,
                arrayOf(COL_BM_ID),
                "$COL_BM_URL = ?",
                arrayOf(url.trim()),
                null, null, null
            )
            cursor.moveToFirst()
        } catch (e: Exception) {
            false
        } finally {
            cursor?.close()
        }
    }

    // =======================================================================
    // Download History (CRUD)
    // =======================================================================

    fun saveDownloadRecord(record: DownloadHistoryEntity): Long {
        val db = writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COL_DL_ID, record.jobId)
                put(COL_DL_TITLE, record.title)
                put(COL_DL_URL, record.url)
                put(COL_DL_FILE_TYPE, record.fileType)
                put(COL_DL_FORMAT, record.format)
                put(COL_DL_TOTAL_BYTES, record.totalBytes)
                put(COL_DL_LOCAL_PATH, record.localPath)
                put(COL_DL_STATUS, record.status)
                put(COL_DL_DATE, record.dateAdded)
            }
            db.insertWithOnConflict(TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving download record", e)
            -1L
        }
    }

    fun getAllDownloadRecords(): List<DownloadHistoryEntity> {
        val list = mutableListOf<DownloadHistoryEntity>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.query(TABLE_DOWNLOADS, null, null, null, null, null, "$COL_DL_DATE DESC")
            while (cursor.moveToNext()) {
                list.add(
                    DownloadHistoryEntity(
                        jobId = cursor.getString(cursor.getColumnIndexOrThrow(COL_DL_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_DL_TITLE)),
                        url = cursor.getString(cursor.getColumnIndexOrThrow(COL_DL_URL)),
                        fileType = cursor.getString(cursor.getColumnIndexOrThrow(COL_DL_FILE_TYPE)),
                        format = cursor.getString(cursor.getColumnIndexOrThrow(COL_DL_FORMAT)),
                        totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DL_TOTAL_BYTES)),
                        localPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_DL_LOCAL_PATH)),
                        status = cursor.getString(cursor.getColumnIndexOrThrow(COL_DL_STATUS)),
                        dateAdded = cursor.getString(cursor.getColumnIndexOrThrow(COL_DL_DATE))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching download records", e)
        } finally {
            cursor?.close()
        }
        return list
    }
}
