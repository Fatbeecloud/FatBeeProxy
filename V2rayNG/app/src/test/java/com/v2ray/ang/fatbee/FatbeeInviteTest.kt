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
import org.junit.jupiter.api.Test

/** 样例按 Xboard /user/invite/fetch 的返回格式写，全是假数据 */
class FatbeeInviteTest {
    @Test
    fun unusedCodesOnly() {
        val codes = XboardApi.parseInviteCodes(
            """{"status":"success","data":{"codes":[
              {"user_id":1,"code":"Ab12Cd34","pv":0,"status":0,"created_at":1763700000},
              {"user_id":1,"code":"Used0000","pv":3,"status":1,"created_at":1763600000}
            ],"stat":[0,0,0,10,0]}}"""
        )
        assertEquals(listOf("Ab12Cd34"), codes)
    }

    @Test
    fun noCodes() {
        assertEquals(emptyList<String>(), XboardApi.parseInviteCodes("""{"status":"success","data":{"codes":[],"stat":[0,0,0,10,0]}}"""))
    }
}
