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
import com.v2ray.ang.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/** 这部手机上的 FatBeeCloud 设置（跟账号无关） */
object FatbeePrefs {
    private const val KEY_ADMIN = "admin_unlocked"
    private const val KEY_ORIGINAL_UI = "original_ui"
    private const val KEY_CONNECTED_AT = "connected_at"

    /**
     * 管理员暗号的 SHA-256 指纹：编译时从 gradle 属性 fatbeeAdminCodeSha256 传入（正式版放在 GitHub Secrets），
     * 代码和仓库里都没有。暗号原文只有站长知道；留空就不启用管理员模式。
     */
    private val ADMIN_CODE_SHA256: String get() = BuildConfig.FATBEE_ADMIN_CODE_SHA256.trim().lowercase()

    private val kv by lazy { MMKV.mmkvWithID("fatbee_prefs", MMKV.MULTI_PROCESS_MODE) }

    private val _admin by lazy { MutableStateFlow(kv.decodeBool(KEY_ADMIN, false)) }
    val adminUnlocked: StateFlow<Boolean> get() = _admin.asStateFlow()

    private val _originalUi by lazy { MutableStateFlow(kv.decodeBool(KEY_ORIGINAL_UI, false) && kv.decodeBool(KEY_ADMIN, false)) }
    /** 管理员可以切回原版 v2rayNG 界面 */
    val originalUi: StateFlow<Boolean> get() = _originalUi.asStateFlow()
    fun setOriginalUi(on: Boolean) {
        val value = on && _admin.value
        kv.encode(KEY_ORIGINAL_UI, value)
        _originalUi.value = value
    }

    /** 输入的是管理员暗号就切换管理员模式，返回切换后的状态；不是暗号返回 null */
    fun tryAdminCode(input: String): Boolean? {
        if (!isAdminCode(input)) return null
        val now = !_admin.value
        kv.encode(KEY_ADMIN, now)
        _admin.value = now
        if (!now) setOriginalUi(false)
        return now
    }

    internal fun isAdminCode(input: String): Boolean {
        val t = input.trim()
        if (t.length < 6 || ADMIN_CODE_SHA256.length != 64) return false
        val hex = MessageDigest.getInstance("SHA-256").digest(t.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return hex == ADMIN_CODE_SHA256
    }

    /** 本次连接开始的时间（用来显示“已连接 00:12:36”） */
    var connectedAt: Long
        get() = kv.decodeLong(KEY_CONNECTED_AT, 0L)
        set(value) {
            kv.encode(KEY_CONNECTED_AT, value)
        }

    /** 本次打开 App 是否已经跟面板同步过一次（同步会刷新节点列表，自动测速要等它完成） */
    val syncDone = MutableStateFlow(false)
}
