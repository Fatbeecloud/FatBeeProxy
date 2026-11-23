/*
 * FatBeeProxy — Copyright (C) 2026 FatBeeCloud
 * https://github.com/Fatbeecloud/FatBeeProxy
 *
 * This file is part of FatBeeProxy, which is based on v2rayNG (https://github.com/2dust/v2rayNG).
 * Licensed under the GNU General Public License v3.0 with additional terms under Section 7
 * (see LICENSE and ADDITIONAL_TERMS.md): modified versions must keep this notice and the
 * "Based on FatBeeProxy by FatBeeCloud" attribution in the app's About screen, and must not
 * use the FatBeeCloud / FatBeeProxy names or bee logo or present themselves as FatBeeCloud.
 */
package com.v2ray.ang.fatbee

import android.content.Context
import com.tencent.mmkv.MMKV
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.enums.RoutingType
import com.v2ray.ang.handler.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 连接模式（参考小火箭的“全局路由”，只给会员两个选项）：
 * - 智能：原版“绕过大陆”规则，国内网站 / App 直接连，国外走 VPN（原版刚装好时默认就是这个）
 * - 全局：原版“全局”规则，所有流量走 VPN
 * 切换就是导入原版对应的预定义规则；管理员在“分流规则”里手动改过的规则会被覆盖（锁定的规则保留）。
 */
object FatbeeMode {
    enum class Mode(val routing: RoutingType) { SMART(RoutingType.WHITE), GLOBAL(RoutingType.GLOBAL) }

    private const val KEY_MODE = "connection_mode"
    private val kv by lazy { MMKV.mmkvWithID("fatbee_prefs", MMKV.MULTI_PROCESS_MODE) }

    private val _mode by lazy {
        MutableStateFlow(runCatching { Mode.valueOf(kv.decodeString(KEY_MODE) ?: "") }.getOrDefault(Mode.SMART))
    }
    val mode: StateFlow<Mode> get() = _mode.asStateFlow()

    /** 后台线程调用。isRunning 为 true 时切换后自动重连，让新规则马上生效 */
    fun apply(context: Context, newMode: Mode, isRunning: Boolean) {
        SettingsManager.resetRoutingRulesetsFromPresets(context, newMode.routing)
        kv.encode(KEY_MODE, newMode.name)
        _mode.value = newMode
        if (isRunning) LauncherManager.restartService(context)
    }
}
