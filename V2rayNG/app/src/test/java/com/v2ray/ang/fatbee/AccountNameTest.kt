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

class AccountNameTest {
    private val sfx = "@example.com"

    @Test
    fun usernameGetsSuffixWhenLoggingIn() {
        assertEquals("bee2026@example.com", AccountName.toLogin("bee2026", sfx))
    }

    @Test
    fun spacesAroundAreTrimmed() {
        assertEquals("bee2026@example.com", AccountName.toLogin("  bee2026 \n", sfx))
        assertEquals("abc@gmail.com", AccountName.toLogin(" abc@gmail.com ", sfx))
    }

    @Test
    fun realEmailIsSentAsIs() {
        assertEquals("Abc@Gmail.com", AccountName.toLogin("Abc@Gmail.com", sfx))
    }

    @Test
    fun fullOwnAccountIsNotDoubled() {
        assertEquals("bee@example.com", AccountName.toLogin("bee@example.com", sfx))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", AccountName.toLogin("   ", sfx))
    }

    @Test
    fun noSuffixConfiguredMeansNoChange() {
        assertEquals("bee", AccountName.toLogin("bee", ""))
    }

    @Test
    fun ownSuffixIsHiddenWhenShown() {
        assertEquals("bee2026", AccountName.display("bee2026@example.com", sfx))
        assertEquals("bee2026", AccountName.display("bee2026@Example.COM", sfx))
    }

    @Test
    fun otherEmailsAreShownUntouched() {
        assertEquals("abc@gmail.com", AccountName.display("abc@gmail.com", sfx))
        assertEquals("abc@example.com.cn", AccountName.display("abc@example.com.cn", sfx))
    }

    @Test
    fun displayNeverReturnsTheBareSuffix() {
        assertEquals("@example.com", AccountName.display("@example.com", sfx))
        assertEquals("", AccountName.display(null, sfx))
    }

    @Test
    fun roundTripKeepsWhatTheMemberTyped() {
        for (typed in listOf("bee2026", "abc@gmail.com")) {
            assertEquals(typed, AccountName.display(AccountName.toLogin(typed, sfx), sfx))
        }
    }
}
