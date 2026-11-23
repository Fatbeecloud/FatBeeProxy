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
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 打开网页（会员中心、注册、找回账号等）。
 * 用安卓的“应用内浏览页”（Chrome Custom Tabs）：从 App 里滑出来，顶部只有 ✕，点 ✕ 或返回就回到 App，
 * 没有浏览器的“回首页”小房子。手机上的浏览器不支持时，退回普通浏览器打开。
 * （直接用意图参数唤起，不需要额外的依赖库。）
 */
object FatbeeWeb {

    private const val EXTRA_SESSION = "android.support.customtabs.extra.SESSION"
    private const val EXTRA_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR"
    private const val EXTRA_TITLE_VISIBILITY = "android.support.customtabs.extra.TITLE_VISIBILITY"
    private const val EXTRA_SHARE_STATE = "androidx.browser.customtabs.extra.SHARE_STATE"
    private const val SHOW_PAGE_TITLE = 1
    private const val SHARE_STATE_OFF = 2
    private const val TOOLBAR_COLOR = 0xFFF6F5F1.toInt()   // 跟 App 的暖白底一致

    /** 会员中心页面（Xboard 会员中心的 # 路由），例如 plan / order / ticket。已登录时免密进入 */
    fun openMemberPage(context: Context, scope: CoroutineScope, page: String) {
        scope.launch {
            val url = withContext(Dispatchers.IO) {
                FatbeeSession.authData?.let { auth ->
                    runCatching { XboardApi.getQuickLoginUrl(auth, page) }.getOrNull()
                }
            } ?: FatbeeConfig.webUrl(page)
            openUrl(context, url)
        }
    }

    /** 面板地址下的路径，例如 /fatbee/recovery */
    fun openPanelPath(context: Context, path: String) {
        openUrl(context, FatbeeConfig.PANEL_URL + path)
    }

    fun openUrl(context: Context, url: String) {
        val uri = url.toUri()
        val tab = Intent(Intent.ACTION_VIEW, uri).apply {
            putExtras(Bundle().apply { putBinder(EXTRA_SESSION, null) })   // 有这个参数，Chrome 就用应用内浏览页
            putExtra(EXTRA_TOOLBAR_COLOR, TOOLBAR_COLOR)
            putExtra(EXTRA_TITLE_VISIBILITY, SHOW_PAGE_TITLE)
            putExtra(EXTRA_SHARE_STATE, SHARE_STATE_OFF)
        }
        try {
            context.startActivity(tab)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "FatBee: open url failed", e)
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }
    }
}
