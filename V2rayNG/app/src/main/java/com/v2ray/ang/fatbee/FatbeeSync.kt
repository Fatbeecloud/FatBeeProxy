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
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SubscriptionUpdater
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import java.io.IOException

/**
 * 把 Xboard 和 v2rayNG 接起来：登录 → 拿订阅链接 → 交给 v2rayNG 原有的订阅更新。
 * 都是阻塞调用，必须在后台线程里用。
 */
object FatbeeSync {

    sealed interface Result {
        data class Ok(val info: SubscribeInfo, val nodeCount: Int) : Result
        /** 通行证失效（被踢下线 / 改了密码 / 过期），要重新登录 */
        data object SessionExpired : Result
        data class Failed(val message: String?) : Result
    }

    /** 登录并马上同步一次节点。登录失败直接抛 XboardException（带面板给的提示）。 */
    @Throws(IOException::class)
    fun login(typedAccount: String, password: String): Result {
        val login = XboardApi.login(AccountName.toLogin(typedAccount), password)
        FatbeeSession.saveLogin(login.authData, typedAccount)
        return sync()
    }

    /** 用已保存的通行证同步：更新流量 / 到期信息，更新节点列表 */
    fun sync(): Result {
        val auth = FatbeeSession.authData ?: return Result.SessionExpired
        val info = try {
            XboardApi.getSubscribe(auth)
        } catch (e: XboardException) {
            if (e.isUnauthorized) {
                FatbeeSession.clear()
                return Result.SessionExpired
            }
            return Result.Failed(e.message)
        } catch (e: IOException) {
            LogUtil.e(AppConfig.TAG, "FatBee: getSubscribe failed", e)
            return Result.Failed(e.message)
        }
        FatbeeSession.saveInfo(info)
        // 公告和新版本：拿不到也不影响节点同步
        runCatching { FatbeeNews.saveNotices(XboardApi.fetchNotices(auth)) }
            .onFailure { LogUtil.e(AppConfig.TAG, "FatBee: notices failed", it) }
        info.token?.let { token ->
            runCatching { FatbeeNews.saveVersion(XboardApi.getAndroidVersion(token)) }
                .onFailure { LogUtil.e(AppConfig.TAG, "FatBee: version check failed", it) }
        }

        val subId = upsertSubscription(info.subscribeUrl)
        val item = MmkvManager.decodeSubscription(subId) ?: return Result.Failed(null)
        // 套餐过期 / 流量用完时面板不给节点，这里会是 0 个，旧节点保留不动
        val result = AngConfigManager.updateConfigViaSub(SubscriptionCache(subId, item))
        FatbeeNodeFilter.clean(subId)
        ensureSelection(subId)
        SubscriptionUpdater.syncOne(subId = subId)
        return Result.Ok(info, result.configCount)
    }

    /** 退出登录：断开 VPN，删掉 App 管理的订阅和它的节点，清掉通行证 */
    fun logout(context: Context) {
        runCatching { LauncherManager.stopService(context) }
        FatbeeSession.subscriptionId?.let { subId ->
            runCatching { SubscriptionUpdater.cancelOne(context, subId) }
            MmkvManager.removeSubscription(subId)
        }
        FatbeeSession.clear()
    }

    /** App 自己管理的那一条订阅：没有就新建，有就更新链接（面板重置订阅后链接会变） */
    private fun upsertSubscription(subscribeUrl: String): String {
        val existingId = FatbeeSession.subscriptionId
        val existing = existingId?.let { MmkvManager.decodeSubscription(it) }
        val subId = if (existingId != null && existing != null) existingId else Utils.getUuid()
        val item = (existing ?: SubscriptionItem()).apply {
            remarks = FatbeeConfig.SUBSCRIPTION_REMARKS
            url = subscribeUrl
            enabled = true
            userAgent = FatbeeConfig.SUBSCRIPTION_USER_AGENT
            autoUpdate = true
            updateInterval = FatbeeConfig.SUBSCRIPTION_UPDATE_MINUTES
        }
        MmkvManager.encodeSubscription(subId, item)
        FatbeeSession.subscriptionId = subId
        // 排到分组的最左边，并且打开 App 直接显示这个分组（原版默认停在空的 Default 分组）
        val order = MmkvManager.decodeSubsList()
        if (order.firstOrNull() != subId) {
            order.remove(subId)
            order.add(0, subId)
            MmkvManager.encodeSubsList(order)
        }
        MmkvManager.encodeSettings(AppConfig.CACHE_SUBSCRIPTION_ID, subId)
        return subId
    }

    /** 当前选中的节点不在列表里了（站长删了 / 换了），就自动选第一个 */
    private fun ensureSelection(subId: String) {
        val servers = MmkvManager.decodeServerList(subId)
        val selected = MmkvManager.getSelectServer()
        if (servers.isNotEmpty() && (selected == null || MmkvManager.decodeServerConfig(selected) == null)) {
            MmkvManager.setSelectServer(servers.first())
        }
    }
}
