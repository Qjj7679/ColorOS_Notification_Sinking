package com.op.notification.sinking.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.UserManager

class SinkingConfigProvider : ContentProvider() {

    companion object {
        private const val PREFS_NAME = "sinking_config"
        private const val KEY_DP = "keyguard_status_view_bottom_margin_dp"

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(SinkingConfig.AUTHORITY, SinkingConfig.PATH_DP, 1)
        }
    }

    private fun readDp(): Float {
        val deviceContext = context?.createDeviceProtectedStorageContext() ?: context ?: return SinkingConfig.DEFAULT_DP
        migrateOldDataIfNeeded()
        val prefs = deviceContext.getSharedPreferences(PREFS_NAME, 0) ?: return SinkingConfig.DEFAULT_DP
        return prefs.getFloat(KEY_DP, SinkingConfig.DEFAULT_DP)
            .coerceIn(SinkingConfig.MIN_DP, SinkingConfig.MAX_DP)
    }

    private fun writeDp(value: Float) {
        val safe = value.coerceIn(SinkingConfig.MIN_DP, SinkingConfig.MAX_DP)
        val deviceContext = context?.createDeviceProtectedStorageContext() ?: context ?: return
        val prefs = deviceContext.getSharedPreferences(PREFS_NAME, 0) ?: return
        prefs.edit().putFloat(KEY_DP, safe).commit()
        // Send broadcast after saving
        context?.sendBroadcast(Intent(SinkingConfig.ACTION_CONFIG_CHANGED))
    }

    private fun migrateOldDataIfNeeded() {
        val ctx = context ?: return
        val deviceContext = ctx.createDeviceProtectedStorageContext()
        val devicePrefs = deviceContext.getSharedPreferences(PREFS_NAME, 0)

        // 如果新存储中已经有值，不需要迁移
        if (devicePrefs.contains(KEY_DP)) return

        // 如果用户尚未解锁，无法访问旧存储 (CE)，跳过迁移，等下次解锁后调用再尝试
        val userManager = ctx.getSystemService(UserManager::class.java)
        if (userManager != null && !userManager.isUserUnlocked) return

        // 尝试从旧存储（CE）读取
        val oldPrefs = ctx.getSharedPreferences(PREFS_NAME, 0)
        if (oldPrefs.contains(KEY_DP)) {
            val oldVal = oldPrefs.getFloat(KEY_DP, SinkingConfig.DEFAULT_DP)
            devicePrefs.edit().putFloat(KEY_DP, oldVal).commit()
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (matcher.match(uri) != 1) return null
        val cursor = MatrixCursor(arrayOf("value"))
        cursor.addRow(arrayOf(readDp().toString()))
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (matcher.match(uri) != 1) return null
        val value = values?.getAsFloat("value")
            ?: values?.getAsString("value")?.toFloatOrNull()
            ?: SinkingConfig.DEFAULT_DP
        writeDp(value)
        context?.contentResolver?.notifyChange(uri, null)
        return uri
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (matcher.match(uri) != 1) return 0
        val value = values?.getAsFloat("value")
            ?: values?.getAsString("value")?.toFloatOrNull()
            ?: SinkingConfig.DEFAULT_DP
        writeDp(value)
        context?.contentResolver?.notifyChange(uri, null)
        return 1
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = if (matcher.match(uri) == 1)
        "vnd.android.cursor.item/vnd.${SinkingConfig.AUTHORITY}.${SinkingConfig.PATH_DP}"
    else null
}
