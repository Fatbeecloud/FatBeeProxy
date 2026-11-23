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

/** 把面板公告的 Markdown / 简单 HTML 转成干净的纯文字（App 里显示用） */
object FatbeeText {

    fun plain(markdown: String): String {
        var s = markdown.replace("\r\n", "\n")
        s = s.replace(Regex("(?i)<br\\s*/?>"), "\n")
        s = s.replace(Regex("(?i)</p>"), "\n")
        s = s.replace(Regex("<[^>]+>"), "")                                  // 其他 HTML 标签
        s = s.replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")                     // 图片
        s = s.replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)"), "$1（$2）")             // 链接：文字（网址）
        s = s.replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")                    // 标题 #
        s = s.replace(Regex("(?m)^\\s{0,3}>\\s?"), "")                         // 引用 >
        s = s.replace(Regex("(?m)^\\s*[-*+]\\s+"), "• ")                       // 列表
        s = s.replace(Regex("(\\*\\*|__)(.+?)\\1"), "$2")                      // 粗体
        s = s.replace(Regex("(?<![\\w*])[*_](\\S(?:.*?\\S)?)[*_](?![\\w*])"), "$1") // 斜体
        s = s.replace(Regex("`{1,3}"), "")                                     // 代码
        s = s.replace(Regex("(?m)^\\s*([-*_]\\s*){3,}$"), "")                  // 分隔线
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
        s = s.replace(Regex("\\n{3,}"), "\n\n")
        return s.trim()
    }
}
