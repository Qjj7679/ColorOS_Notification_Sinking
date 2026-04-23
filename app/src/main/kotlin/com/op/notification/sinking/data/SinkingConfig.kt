package com.op.notification.sinking.data

import android.net.Uri

object SinkingConfig {
    const val AUTHORITY = "com.op.notification.sinking.config"
    const val PATH_DP = "bottom_margin_dp"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_DP")

    const val ACTION_CONFIG_CHANGED = "com.op.notification.sinking.CONFIG_CHANGED"

    const val DEFAULT_DP = 300f
    const val MIN_DP = 24f
    const val MAX_DP = 480f
}
