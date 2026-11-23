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

/**
 * 会员账号的“偷偷补后缀 / 偷偷藏后缀”，规则跟网页注册、登录页完全一样：
 * - 输入里没有 @：发给面板前补上后缀（会员只输了账号名）
 * - 输入里有 @：原样发送（真实邮箱，或完整账号）
 * - 显示时：去掉我们自己的后缀，别的邮箱一个字不动
 */
object AccountName {

    fun toLogin(input: String, suffix: String = FatbeeConfig.ACCOUNT_SUFFIX): String {
        val t = input.trim()
        if (t.isEmpty() || suffix.isEmpty() || t.contains('@')) return t
        return t + suffix
    }

    fun display(account: String?, suffix: String = FatbeeConfig.ACCOUNT_SUFFIX): String {
        val t = account?.trim().orEmpty()
        if (suffix.isNotEmpty() && t.length > suffix.length && t.endsWith(suffix, ignoreCase = true)) {
            return t.substring(0, t.length - suffix.length)
        }
        return t
    }
}
