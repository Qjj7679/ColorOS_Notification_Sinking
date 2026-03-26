package com.op.notification.sinking.ui

import android.content.ContentValues
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.op.notification.sinking.data.SinkingConfig
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MiuixTheme {
                ConfigScreen(
                    initialDp = readDpFromProvider(),
                    onSave = { value ->
                        val safe = value.coerceIn(SinkingConfig.MIN_DP, SinkingConfig.MAX_DP)
                        runCatching {
                            val values = ContentValues().apply { put("value", safe) }
                            contentResolver.update(SinkingConfig.CONTENT_URI, values, null, null)
                        }
                    }
                )
            }
        }
    }

    private fun readDpFromProvider(): Float {
        return runCatching {
            contentResolver.query(SinkingConfig.CONTENT_URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.toFloatOrNull()?.coerceIn(SinkingConfig.MIN_DP, SinkingConfig.MAX_DP)
                        ?: SinkingConfig.DEFAULT_DP
                } else SinkingConfig.DEFAULT_DP
            } ?: SinkingConfig.DEFAULT_DP
        }.getOrDefault(SinkingConfig.DEFAULT_DP)
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ConfigScreen(initialDp: Float, onSave: (Float) -> Unit) {
    var currentValue by remember {
        mutableFloatStateOf(initialDp.coerceIn(SinkingConfig.MIN_DP, SinkingConfig.MAX_DP).toInt().toFloat())
    }
    var textValue by remember { mutableFloatStateOf(currentValue) }
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(Unit) {
        snapshotFlow { currentValue }
            .debounce(300)
            .distinctUntilChanged()
            .collect { value ->
                currentOnSave(value)
            }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "ColorOS 16 通知下沉"
            )
        }
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("调节 锁屏界面通知底部边距 的 dp 值（数值越大越靠下）")
            Text("当前值：${currentValue.toInt()}dp")
            Slider(
                value = currentValue,
                onValueChange = {
                    val safe = it.toInt().coerceIn(
                        SinkingConfig.MIN_DP.toInt(),
                        SinkingConfig.MAX_DP.toInt()
                    ).toFloat()
                    currentValue = safe
                    textValue = safe
                },
                valueRange = SinkingConfig.MIN_DP..SinkingConfig.MAX_DP,
                steps = (SinkingConfig.MAX_DP - SinkingConfig.MIN_DP - 1).toInt(),
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = textValue.toInt().toString(),
                onValueChange = {
                    val parsed = it.toIntOrNull()
                    if (parsed != null) {
                        val safe = parsed.coerceIn(
                            SinkingConfig.MIN_DP.toInt(),
                            SinkingConfig.MAX_DP.toInt()
                        ).toFloat()
                        textValue = safe
                        currentValue = safe
                    } else if (it.isEmpty()) {
                        textValue = SinkingConfig.MIN_DP
                        currentValue = SinkingConfig.MIN_DP
                    }
                },
                label = "输入 dp 值",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("提示：调整后需重启 SystemUI 或重启设备，使新参数生效。")
        }
    }
}
