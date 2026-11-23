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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FatbeeNodeFilterTest {
    @Test
    fun xboardInfoNodesAreDetected() {
        assertTrue(FatbeeNodeFilter.isInfoNode("剩余流量：86.4 GB"))
        assertTrue(FatbeeNodeFilter.isInfoNode("套餐到期：2026-12-31"))
        assertTrue(FatbeeNodeFilter.isInfoNode("距离下次重置剩余：9 天"))
        assertTrue(FatbeeNodeFilter.isInfoNode("过滤掉3条线路"))
    }

    @Test
    fun realNodesAreKept() {
        assertFalse(FatbeeNodeFilter.isInfoNode("Tokyo 03 Japan"))
        assertFalse(FatbeeNodeFilter.isInfoNode("Hong Kong BeeLine 香港特快专线"))
        assertFalse(FatbeeNodeFilter.isInfoNode("日本 流量倍率 0.5"))
        assertFalse(FatbeeNodeFilter.isInfoNode(null))
    }
}
