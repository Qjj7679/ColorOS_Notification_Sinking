package com.op.notification.sinking.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.util.TypedValue
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.op.notification.sinking.data.SinkingConfig

object MainHook : YukiBaseHooker() {

    private const val TARGET_DIMEN_NAME = "keyguard_status_view_bottom_margin"
    private const val QUERY_RETRY_COUNT = 4
    private const val QUERY_RETRY_DELAY_MS = 120L
    private const val FALLBACK_PREFS_NAME = "sinking_config_fallback"
    private const val FALLBACK_KEY_DP = "keyguard_status_view_bottom_margin_dp"
    private var cachedTargetDimenId = 0
    @Volatile
    private var cachedReplacementDp: Float? = null

    // BroadcastReceiver to listen for configuration changes
    private val configChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SinkingConfig.ACTION_CONFIG_CHANGED) {
                cachedReplacementDp = null
            }
        }
    }

    override fun onHook() {
        loadApp(name = "com.android.systemui") {
            // Register BroadcastReceiver when the hook is loaded
            currentAppContext?.registerReceiver(
                configChangeReceiver,
                IntentFilter(SinkingConfig.ACTION_CONFIG_CHANGED)
            )

            Resources::class.java.method {
                name = "getDimensionPixelSize"
                param(Int::class.java)
            }.hook {
                after {
                    val requestId = args(0).int()
                    val res = instance as? Resources ?: return@after

                    if (cachedTargetDimenId == 0) {
                        cachedTargetDimenId = res.getIdentifier(TARGET_DIMEN_NAME, "dimen", packageName)
                    }
                    if (cachedTargetDimenId == 0 || requestId != cachedTargetDimenId) return@after

                    val replacementDp = cachedReplacementDp ?: run {
                        val appContext = currentAppContext ?: return@after
                        val value = readConfigWithRetry(appContext) ?: readFallbackDp(appContext)
                        value?.also {
                            cachedReplacementDp = it
                            saveFallbackDp(appContext, it)
                        }
                    }

                    if (replacementDp == null) return@after

                    val modifiedPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        replacementDp,
                        res.displayMetrics
                    ).toInt()

                    result = modifiedPx
                }
            }
        }
    }

    private fun readConfigWithRetry(context: Context): Float? {
        repeat(QUERY_RETRY_COUNT) { index ->
            val value = runCatching {
                context.contentResolver.query(SinkingConfig.CONTENT_URI, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(0)?.toFloatOrNull()?.coerceIn(
                            SinkingConfig.MIN_DP,
                            SinkingConfig.MAX_DP
                        )
                    } else null
                }
            }.getOrNull()
            if (value != null) return value
            if (index < QUERY_RETRY_COUNT - 1) Thread.sleep(QUERY_RETRY_DELAY_MS)
        }
        return null
    }

    private fun readFallbackDp(context: Context): Float? {
        return runCatching {
            context.createDeviceProtectedStorageContext()
                .getSharedPreferences(FALLBACK_PREFS_NAME, 0)
                .takeIf { it.contains(FALLBACK_KEY_DP) }
                ?.getFloat(FALLBACK_KEY_DP, SinkingConfig.DEFAULT_DP)
                ?.coerceIn(SinkingConfig.MIN_DP, SinkingConfig.MAX_DP)
        }.getOrNull()
    }

    private fun saveFallbackDp(context: Context, value: Float) {
        runCatching {
            context.createDeviceProtectedStorageContext()
                .getSharedPreferences(FALLBACK_PREFS_NAME, 0)
                .edit()
                .putFloat(FALLBACK_KEY_DP, value.coerceIn(SinkingConfig.MIN_DP, SinkingConfig.MAX_DP))
                .apply()
        }
    }

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            val appGlobalsClass = Class.forName("android.app.AppGlobals")
            appGlobalsClass.getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
}