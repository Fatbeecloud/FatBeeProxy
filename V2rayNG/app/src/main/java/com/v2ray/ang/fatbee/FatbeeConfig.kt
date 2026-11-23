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

import com.v2ray.ang.BuildConfig

/**
 * 胖蜜蜂（FatBeeCloud）的固定设置。面板地址和账号后缀来自 gradle.properties，不写死在代码里。
 */
object FatbeeConfig {
    /** 面板网址，例如 https://panel.example.com（末尾不带 /） */
    val PANEL_URL: String get() = BuildConfig.FATBEE_PANEL_URL.trimEnd('/')

    /** 只填账号名时自动补上的后缀，例如 @example.com；会员在界面上永远看不到它 */
    val ACCOUNT_SUFFIX: String get() = BuildConfig.FATBEE_ACCOUNT_SUFFIX

    /**
     * 拿订阅时自报的客户端身份。Xboard 按这个决定输出格式，并按版本号过滤节点
     * （例如 v2rayNG 低于 1.9.5 不给 Hysteria2）。本 App 的版本号是自己的 0.x，
     * 所以固定报底层 v2rayNG 的版本；合并原作者新版本时同步改这里。
     */
    const val SUBSCRIPTION_USER_AGENT = "v2rayNG/2.3.15"

    /** App 自动管理的那条订阅在列表里的名字 */
    const val SUBSCRIPTION_REMARKS = "FatBeeProxy"

    /** 后台自动更新订阅的间隔（分钟） */
    const val SUBSCRIPTION_UPDATE_MINUTES = 360L

    /** 官网首页，例如 https://example.com（末尾不带 /） */
    val WEBSITE_URL: String get() = BuildConfig.FATBEE_WEBSITE_URL.trimEnd('/')

    /** 本 App 源代码地址（GPL 要求在 App 里能找到） */
    val SOURCE_URL: String get() = BuildConfig.FATBEE_SOURCE_URL

    /**
     * 原版 FatBeeProxy 项目地址：署名链接，固定不可配置。
     * 按 ADDITIONAL_TERMS.md（GPL-3.0 第 7 条），改装版必须在“关于”页保留这条署名和链接。
     */
    const val ORIGINAL_PROJECT_URL = "https://github.com/Fatbeecloud/FatBeeProxy"

    fun webUrl(hashPath: String): String = "$PANEL_URL/#/$hashPath"
}
