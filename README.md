# ColorOS Notification Sinking

![License](https://img.shields.io/github/license/Qjj7679/ColorOS_Notification_Sinking?style=flat-square)

为 ColorOS 16 的锁屏/AOD 通知提供“底部边距”可调的 Xposed 模块。提供可视化 UI 配置，参数通过 ContentProvider 供 SystemUI 读取。

## 功能
- 调节锁屏界面通知底部边距（dp）
- 设置范围：24–360 dp
- 变更后需重启 SystemUI 或重启设备生效

## 项目结构
```
app/src/main/kotlin/com/op/notification/sinking
├─ data/          # 配置与 Provider
│  ├─ SinkingConfig.kt
│  └─ SinkingConfigProvider.kt
├─ hook/          # SystemUI Hook
│  └─ MainHook.kt
├─ ui/            # Compose UI
│  └─ MainActivity.kt
└─ HookEntry.kt   # YukiHookAPI 入口
```

## 配置通道
- authority: `com.op.notification.sinking.config`
- path: `bottom_margin_dp`
- URI: `content://com.op.notification.sinking.config/bottom_margin_dp`

## 构建
```bash
./gradlew assembleDebug
```
APK 输出：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 备注
- Hook 侧已增加进程内缓存，首次读取后缓存值；重启 SystemUI 会重新读取。

## License
MIT
