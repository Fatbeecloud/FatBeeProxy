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

import com.tencent.mmkv.MMKV
import com.v2ray.ang.util.JsonUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 登录状态：只存在这部手机的 App 私有存储里，不存密码。
 * account 存的是会员自己输入的内容（不带我们的后缀）。
 */
object FatbeeSession {
    private const val KEY_AUTH = "auth_data"
    private const val KEY_ACCOUNT = "account"
    private const val KEY_SUB_ID = "sub_id"
    private const val KEY_INFO = "subscribe_info"
    private const val KEY_INFO_TIME = "subscribe_info_time"

    private val kv by lazy { MMKV.mmkvWithID("fatbee_session", MMKV.MULTI_PROCESS_MODE) }

    val authData: String? get() = kv.decodeString(KEY_AUTH)?.takeIf { it.isNotBlank() }

    /** 上次登录时会员输入的账号（用来下次自动填好），不带后缀 */
    val account: String get() = kv.decodeString(KEY_ACCOUNT).orEmpty()

    var subscriptionId: String?
        get() = kv.decodeString(KEY_SUB_ID)?.takeIf { it.isNotBlank() }
        set(value) {
            if (value == null) kv.removeValueForKey(KEY_SUB_ID) else kv.encode(KEY_SUB_ID, value)
        }

    fun isLoggedIn(): Boolean = authData != null

    fun saveLogin(authData: String, typedAccount: String) {
        kv.encode(KEY_AUTH, authData)
        kv.encode(KEY_ACCOUNT, AccountName.display(typedAccount))
    }

    fun saveInfo(info: SubscribeInfo) {
        kv.encode(KEY_INFO, JsonUtil.toJson(info))
        kv.encode(KEY_INFO_TIME, System.currentTimeMillis())
        _info.value = info
    }

    private val _info by lazy { MutableStateFlow(lastInfo()) }
    /** 流量 / 到期信息，同步后界面自动刷新 */
    val info: StateFlow<SubscribeInfo?> get() = _info.asStateFlow()

    /** 最近一次从面板拿到的流量 / 到期信息（首页显示用） */
    fun lastInfo(): SubscribeInfo? =
        kv.decodeString(KEY_INFO)?.let { JsonUtil.fromJsonSafe(it, SubscribeInfo::class.java) }

    /** 退出登录：清掉通行证和缓存信息，保留上次输入的账号方便再次登录 */
    fun clear() {
        kv.removeValuesForKeys(arrayOf(KEY_AUTH, KEY_SUB_ID, KEY_INFO, KEY_INFO_TIME))
        _info.value = null
    }
}
