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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 样例按 Xboard 源码的返回格式写，全是假数据 */
class FatbeeNewsTest {

    @Test
    fun noticesParsed() {
        val list = XboardApi.parseNotices(
            """{"data":[
              {"id":12,"title":"黑五特惠","content":"**全部套餐 7 折**","show":true,"tags":["弹窗","活动"],"sort":0,"created_at":1763700000},
              {"id":9,"title":"维护通知","content":"今晚维护","show":true,"tags":null,"created_at":1763600000}
            ],"total":2}"""
        )
        assertEquals(2, list.size)
        assertEquals(12L, list[0].id)
        assertTrue(list[0].isPopup)
        assertFalse(list[1].isPopup)
        assertEquals(emptyList<String>(), list[1].tags)
    }

    @Test
    fun emptyNotices() {
        assertEquals(emptyList<Notice>(), XboardApi.parseNotices("""{"data":[],"total":0}"""))
    }

    @Test
    fun versionParsed() {
        val v = XboardApi.parseAppVersion(
            """{"status":"success","data":{"windows_version":"","windows_download_url":"","android_version":"0.2.0","android_download_url":"https://example.com/app.apk"}}"""
        )
        assertEquals(AppVersion("0.2.0", "https://example.com/app.apk"), v)
    }

    @Test
    fun versionMissingMeansNoUpdate() {
        assertNull(XboardApi.parseAppVersion("""{"status":"success","data":{"android_version":null,"android_download_url":null}}"""))
        assertNull(XboardApi.parseAppVersion("""{"status":"success","data":{"android_version":"0.2.0","android_download_url":""}}"""))
    }

    @Test
    fun versionCompare() {
        assertTrue(XboardApi.isNewer("0.2.0", "0.1.0"))
        assertTrue(XboardApi.isNewer("1.2.10", "1.2.9"))
        assertTrue(XboardApi.isNewer("v1.0.1", "1.0"))
        assertFalse(XboardApi.isNewer("0.1.0", "0.1.0"))
        assertFalse(XboardApi.isNewer("0.1", "0.1.0"))
        assertFalse(XboardApi.isNewer("0.0.9", "0.1.0"))
        assertFalse(XboardApi.isNewer("1.0.0-beta", "1.0.0"))
    }

    @Test
    fun markdownToPlain() {
        assertEquals("全部套餐 7 折", FatbeeText.plain("**全部套餐 7 折**"))
        assertEquals("标题\n• 第一条\n• 第二条", FatbeeText.plain("## 标题\n- 第一条\n- 第二条"))
        assertEquals("官网（https://example.com）", FatbeeText.plain("[官网](https://example.com)"))
        assertEquals("第一行\n第二行", FatbeeText.plain("<p>第一行</p><p>第二行</p>"))
        assertEquals("A & B", FatbeeText.plain("A &amp; B"))
    }
}
