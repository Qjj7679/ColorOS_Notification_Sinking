package com.op.notification.sinking

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.op.notification.sinking.hook.MainHook

@InjectYukiHookWithXposed(
    sourcePath = "src/main",
    modulePackageName = "com.op.notification.sinking",
    entryClassName = "HookEntryXposed",
    isUsingResourcesHook = true
)
object HookEntry : IYukiHookXposedInit {
    override fun onHook() = encase(MainHook)
}
