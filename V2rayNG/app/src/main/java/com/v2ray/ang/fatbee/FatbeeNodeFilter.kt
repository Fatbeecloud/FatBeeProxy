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

import com.v2ray.ang.handler.MmkvManager

/**
 * Xboard 后台开了“在订阅里显示用户信息”时，会在节点列表前面插几个只用来显示文字的假节点
 * （剩余流量 / 距离下次重置 / 套餐到期 / 过滤掉 N 条线路）。它们不能连，从列表里去掉。
 */
object FatbeeNodeFilter {

    private val INFO_NODE = Regex("^(剩余流量：|套餐到期：|距离下次重置剩余：|过滤掉\\d+条线路)")

    fun isInfoNode(remarks: String?): Boolean = remarks != null && INFO_NODE.containsMatchIn(remarks.trim())

    /** 只处理 App 自己管理的那条订阅 */
    fun clean(subscriptionId: String) {
        if (subscriptionId != FatbeeSession.subscriptionId) return
        val fakes = MmkvManager.decodeServerList(subscriptionId).filter { guid ->
            isInfoNode(MmkvManager.decodeServerConfig(guid)?.remarks)
        }
        if (fakes.isNotEmpty()) MmkvManager.removeServers(fakes, subscriptionId)
    }
}
