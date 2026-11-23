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
import com.v2ray.ang.util.JsonUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 公告和新版本提示：同步时从面板拿，存在本机；记住哪些看过、哪些弹窗已经弹过 */
object FatbeeNews {
    private const val KEY_NOTICES = "notices"
    private const val KEY_SEEN_ID = "notice_seen_id"
    private const val KEY_POPPED = "notice_popped_ids"
    private const val KEY_VERSION = "app_version"
    private const val KEY_VERSION_DISMISSED = "app_version_dismissed"

    private val kv by lazy { MMKV.mmkvWithID("fatbee_prefs", MMKV.MULTI_PROCESS_MODE) }

    private val _notices by lazy {
        MutableStateFlow(
            kv.decodeString(KEY_NOTICES)?.let { JsonUtil.fromJsonSafe(it, Array<Notice>::class.java)?.toList() }.orEmpty()
        )
    }
    val notices: StateFlow<List<Notice>> get() = _notices.asStateFlow()

    private val _seenId by lazy { MutableStateFlow(kv.decodeLong(KEY_SEEN_ID, 0L)) }
    /** 看过的最新公告 id（有比它新的就显示小红点） */
    val seenId: StateFlow<Long> get() = _seenId.asStateFlow()

    private val _update by lazy {
        MutableStateFlow(kv.decodeString(KEY_VERSION)?.let { JsonUtil.fromJsonSafe(it, AppVersion::class.java) }?.takeIf(::isNewerThanApp))
    }
    /** 有比当前 App 新的版本就不是 null */
    val update: StateFlow<AppVersion?> get() = _update.asStateFlow()

    fun saveNotices(list: List<Notice>) {
        kv.encode(KEY_NOTICES, JsonUtil.toJson(list))
        _notices.value = list
    }

    fun markAllSeen() {
        val max = _notices.value.maxOfOrNull { it.id } ?: return
        if (max > _seenId.value) {
            kv.encode(KEY_SEEN_ID, max)
            _seenId.value = max
        }
    }

    /** 还没弹过的“弹窗”公告（最新的一条） */
    fun nextPopup(): Notice? {
        val popped = poppedIds()
        return _notices.value.filter { it.isPopup && it.id !in popped }.maxByOrNull { it.id }
    }

    fun markPopped(id: Long) {
        kv.encode(KEY_POPPED, (poppedIds() + id).joinToString(","))
    }

    private fun poppedIds(): Set<Long> =
        kv.decodeString(KEY_POPPED).orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()

    fun saveVersion(v: AppVersion?) {
        if (v == null) kv.removeValueForKey(KEY_VERSION) else kv.encode(KEY_VERSION, JsonUtil.toJson(v))
        _update.value = v?.takeIf(::isNewerThanApp)
    }

    /** 会员点了“以后再说”：这个版本不再自动弹，但“我的”页还看得到 */
    fun shouldPromptUpdate(v: AppVersion): Boolean = kv.decodeString(KEY_VERSION_DISMISSED) != v.version

    fun dismissUpdate(v: AppVersion) {
        kv.encode(KEY_VERSION_DISMISSED, v.version)
    }

    private fun isNewerThanApp(v: AppVersion): Boolean = XboardApi.isNewer(v.version, BuildConfig.VERSION_NAME)
}
