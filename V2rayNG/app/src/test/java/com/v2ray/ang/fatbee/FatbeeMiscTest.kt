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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FatbeeMiscTest {

    // 暗号原文不写进任何文件，这里只测“错的、太短的都不算”
    @Test
    fun wrongOrShortAdminCodeIsRejected() {
        assertFalse(FatbeePrefs.isAdminCode(""))
        assertFalse(FatbeePrefs.isAdminCode("12345"))
        assertFalse(FatbeePrefs.isAdminCode("japan"))
        assertFalse(FatbeePrefs.isAdminCode("fatbeecloud"))
    }

    @Test
    fun quickLoginUrlUsesAppPanelAddress() {
        assertEquals(
            "https://panel.example.com/#/login?verify=abc&redirect=plan",
            XboardApi.rewriteQuickLoginUrl("https://app.example.com/#/login?verify=abc&redirect=plan", "https://panel.example.com/")
        )
    }

    @Test
    fun quickLoginUrlWithoutHashIsKept() {
        assertEquals("https://x.com/login", XboardApi.rewriteQuickLoginUrl("https://x.com/login", "https://panel.example.com"))
    }

    @Test
    fun quickLoginParse() {
        assertEquals(
            "https://p/#/login?verify=1&redirect=plan",
            XboardApi.parseQuickLoginUrl("""{"status":"success","data":"https://p/#/login?verify=1&redirect=plan"}""")
        )
        assertThrows<XboardException> { XboardApi.parseQuickLoginUrl("""{"status":"fail","message":"未登录","data":null}""") }
    }
}
