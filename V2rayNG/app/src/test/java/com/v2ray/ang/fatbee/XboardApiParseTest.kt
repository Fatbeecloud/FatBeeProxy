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
import org.junit.jupiter.api.assertThrows

/** 样例 JSON 按 Xboard 源码（commit d826a3b）的返回格式写，全是假数据 */
class XboardApiParseTest {

    @Test
    fun loginSuccess() {
        val r = XboardApi.parseLogin(
            """{"status":"success","message":"操作成功","data":{"token":"0123456789abcdef0123456789abcdef","auth_data":"Bearer 1|fakeTokenForTest","is_admin":false},"error":null}"""
        )
        assertEquals("Bearer 1|fakeTokenForTest", r.authData)
        assertFalse(r.isAdmin)
    }

    @Test
    fun loginWithoutAuthDataFails() {
        assertThrows<XboardException> { XboardApi.parseLogin("""{"status":"success","data":{}}""") }
    }

    @Test
    fun errorMessageFromPanel() {
        assertEquals("邮箱或密码错误", XboardApi.parseErrorMessage("""{"status":"fail","message":"邮箱或密码错误","data":null}"""))
    }

    @Test
    fun validationErrorPrefersFieldMessage() {
        assertEquals(
            "密码至少 8 位",
            XboardApi.parseErrorMessage("""{"message":"The given data was invalid.","errors":{"password":["密码至少 8 位"]}}""")
        )
    }

    @Test
    fun nonJsonErrorGivesNull() {
        assertNull(XboardApi.parseErrorMessage("<html>502 Bad Gateway</html>"))
    }

    @Test
    fun subscribeInfo() {
        val info = XboardApi.parseSubscribe(
            """{"status":"success","data":{"plan_id":3,"token":"x","expired_at":1798761600,"u":1073741824,"d":2147483648,
               "transfer_enable":107374182400,"email":"bee@example.com","uuid":"u","device_limit":null,"speed_limit":null,
               "next_reset_at":null,"plan":{"id":3,"name":"标准套餐","transfer_enable":100},
               "subscribe_url":"https://panel.example.com/s/0123456789abcdef0123456789abcdef","reset_day":9}}"""
        )
        assertEquals("https://panel.example.com/s/0123456789abcdef0123456789abcdef", info.subscribeUrl)
        assertEquals("标准套餐", info.planName)
        assertEquals(107374182400L - 3221225472L, info.remaining)
        assertEquals(1798761600L, info.expiredAt)
        assertEquals(9, info.resetDay)
    }

    @Test
    fun longTermPlanHasNoExpiry() {
        val info = XboardApi.parseSubscribe(
            """{"data":{"expired_at":null,"u":0,"d":0,"transfer_enable":0,"email":"a@b.com","subscribe_url":"https://p/s/t","reset_day":null}}"""
        )
        assertNull(info.expiredAt)
        assertNull(info.planName)
        assertNull(info.resetDay)
        assertEquals(0L, info.remaining)
    }

    @Test
    fun usedMoreThanTotalNeverGoesNegative() {
        val info = XboardApi.parseSubscribe("""{"data":{"u":10,"d":10,"transfer_enable":5,"subscribe_url":"https://p/s/t"}}""")
        assertEquals(0L, info.remaining)
    }

    @Test
    fun missingSubscribeUrlFails() {
        assertThrows<XboardException> { XboardApi.parseSubscribe("""{"data":{"u":0}}""") }
    }

    @Test
    fun only403MeansSignedOut() {
        assertTrue(XboardException("x", 403).isUnauthorized)
        assertFalse(XboardException("x", 400).isUnauthorized)
        assertFalse(XboardException("x", 429).isUnauthorized)
    }
}
