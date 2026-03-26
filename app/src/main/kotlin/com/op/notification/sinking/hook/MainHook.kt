package com.op.notification.sinking.hook

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.op.notification.sinking.data.SinkingConfig

object MainHook : YukiBaseHooker() {

    private const val TARGET_DIMEN_NAME = "keyguard_status_view_bottom_margin"
    private var cachedTargetDimenId = 0
    @Volatile
    private var cachedReplacementDp: Float? = null

    override fun onHook() {
        loadApp(name = "com.android.systemui") {
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

                    val replacementDp = cachedReplacementDp ?: runCatching {
                        val appContext = currentAppContext ?: return@runCatching SinkingConfig.DEFAULT_DP
                        appContext.contentResolver.query(SinkingConfig.CONTENT_URI, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                cursor.getString(0)?.toFloatOrNull()?.coerceIn(
                                    SinkingConfig.MIN_DP,
                                    SinkingConfig.MAX_DP
                                ) ?: SinkingConfig.DEFAULT_DP
                            } else SinkingConfig.DEFAULT_DP
                        } ?: SinkingConfig.DEFAULT_DP
                    }.getOrDefault(SinkingConfig.DEFAULT_DP).also { cachedReplacementDp = it }

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

    private val currentAppContext: Context?
        get() = runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            val appGlobalsClass = Class.forName("android.app.AppGlobals")
            appGlobalsClass.getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
}
