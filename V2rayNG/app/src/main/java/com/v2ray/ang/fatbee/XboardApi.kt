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

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 面板返回的错误。httpCode 403 = 没登录或登录已过期。 */
class XboardException(message: String, val httpCode: Int = 0) : IOException(message) {
    val isUnauthorized: Boolean get() = httpCode == 403
}

data class LoginResult(
    val authData: String,
    val isAdmin: Boolean,
)

/** /api/v1/user/getSubscribe 里 App 用得到的部分。流量单位是字节，时间是 unix 秒。 */
data class SubscribeInfo(
    val subscribeUrl: String,
    val email: String,
    val planName: String?,
    val upload: Long,
    val download: Long,
    val total: Long,
    val expiredAt: Long?,
    val resetDay: Int?,
    /** 订阅凭证（检查更新接口用）。旧版本存下的信息里没有这个字段 */
    val token: String? = null,
) {
    val remaining: Long get() = (total - upload - download).coerceAtLeast(0)
}

/** 面板公告。content 是 Markdown（App 里转成纯文字显示） */
data class Notice(
    val id: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val createdAt: Long,
) {
    /** 后台给公告打上“弹窗”标签：打开 App 时弹出一次（跟 Xboard 网页规则一样） */
    val isPopup: Boolean get() = tags.any { it.trim() == "弹窗" }
}

/** 面板后台填的安卓版本号和下载地址 */
data class AppVersion(val version: String, val downloadUrl: String)

/**
 * Xboard 原版用户接口（登录、订阅信息、公告、邀请码、检查更新）。
 * 都是阻塞调用，必须在后台线程里用。
 */
object XboardApi {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun language(): String =
        if (Locale.getDefault().language.equals("zh", ignoreCase = true)) "zh-CN" else "en-US"

    @Throws(IOException::class)
    fun login(email: String, password: String): LoginResult {
        val body = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()
        val request = Request.Builder()
            .url("${FatbeeConfig.PANEL_URL}/api/v1/passport/auth/login")
            .post(body)
            .header("Accept", "application/json")
            .header("Content-Language", language())
            .build()
        return execute(request, ::parseLogin)
    }

    @Throws(IOException::class)
    fun getSubscribe(authData: String): SubscribeInfo {
        val request = Request.Builder()
            .url("${FatbeeConfig.PANEL_URL}/api/v1/user/getSubscribe")
            .get()
            .header("Accept", "application/json")
            .header("Content-Language", language())
            .header("Authorization", authData)
            .build()
        return execute(request, ::parseSubscribe)
    }

    /**
     * 拿一个 60 秒有效的“免密登录网页”链接，打开后直接进入会员中心的 page 页。
     * 面板返回的链接用的是后台设置的站点地址，这里统一换成 App 配置的面板地址，避免后台地址没设对时打不开。
     */
    @Throws(IOException::class)
    fun getQuickLoginUrl(authData: String, page: String): String {
        val body = FormBody.Builder().add("redirect", page).build()
        val request = Request.Builder()
            .url("${FatbeeConfig.PANEL_URL}/api/v1/passport/auth/getQuickLoginUrl")
            .post(body)
            .header("Accept", "application/json")
            .header("Authorization", authData)
            .build()
        return execute(request) { rewriteQuickLoginUrl(parseQuickLoginUrl(it), FatbeeConfig.PANEL_URL) }
    }

    private fun <T> execute(request: Request, parse: (String) -> T): T {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw XboardException(parseErrorMessage(text) ?: "HTTP ${response.code}", response.code)
            }
            return parse(text)
        }
    }

    // ---------- 解析（单独拿出来，方便单元测试） ----------

    internal fun parseLogin(text: String): LoginResult {
        val data = dataObject(text)
        val authData = data.str("auth_data")
        if (authData.isNullOrBlank()) throw XboardException(parseErrorMessage(text) ?: "Login failed")
        return LoginResult(
            authData = authData,
            isAdmin = data.get("is_admin")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
        )
    }

    internal fun parseSubscribe(text: String): SubscribeInfo {
        val data = dataObject(text)
        val url = data.str("subscribe_url")
        if (url.isNullOrBlank()) throw XboardException("No subscription")
        val plan = data.get("plan")?.takeIf { it.isJsonObject }?.asJsonObject
        return SubscribeInfo(
            subscribeUrl = url,
            email = data.str("email").orEmpty(),
            planName = plan?.str("name"),
            upload = data.long("u") ?: 0,
            download = data.long("d") ?: 0,
            total = data.long("transfer_enable") ?: 0,
            expiredAt = data.long("expired_at")?.takeIf { it > 0 },
            resetDay = data.long("reset_day")?.toInt(),
            token = data.str("token"),
        )
    }

    @Throws(IOException::class)
    fun fetchNotices(authData: String): List<Notice> {
        val request = Request.Builder()
            .url("${FatbeeConfig.PANEL_URL}/api/v1/user/notice/fetch?current=1")
            .get()
            .header("Accept", "application/json")
            .header("Content-Language", language())
            .header("Authorization", authData)
            .build()
        return execute(request, ::parseNotices)
    }

    /** 面板后台“APP 设置”里的安卓版本号和下载地址；没填就返回 null */
    @Throws(IOException::class)
    fun getAndroidVersion(subscriptionToken: String): AppVersion? {
        val url = "${FatbeeConfig.PANEL_URL}/api/v1/client/app/getVersion".toHttpUrl().newBuilder()
            .addQueryParameter("token", subscriptionToken)
            .build()
        val request = Request.Builder().url(url).get().header("Accept", "application/json").build()
        return execute(request, ::parseAppVersion)
    }

    /**
     * 这个账号目前没用掉的邀请码（Xboard 原版 /user/invite/fetch，只返回未使用的）。
     * 跟网站“我的邀请”是同一个接口。
     */
    @Throws(IOException::class)
    fun fetchInviteCodes(authData: String): List<String> {
        val request = Request.Builder()
            .url("${FatbeeConfig.PANEL_URL}/api/v1/user/invite/fetch")
            .get()
            .header("Accept", "application/json")
            .header("Content-Language", language())
            .header("Authorization", authData)
            .build()
        return execute(request, ::parseInviteCodes)
    }

    /**
     * 生成邀请码（Xboard 原版 /user/invite/save）。
     * 面板可以在生成时加限制（例如每人每天 1 个），超出时抛出带面板提示的 XboardException。
     */
    @Throws(IOException::class)
    fun generateInviteCode(authData: String) {
        val request = Request.Builder()
            .url("${FatbeeConfig.PANEL_URL}/api/v1/user/invite/save")
            .get()
            .header("Accept", "application/json")
            .header("Content-Language", language())
            .header("Authorization", authData)
            .build()
        execute(request) { }
    }

    internal fun parseInviteCodes(text: String): List<String> {
        val codes = dataObject(text).get("codes")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        return codes.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val status = o.long("status") ?: 0L
            o.str("code")?.takeIf { it.isNotBlank() && status == 0L }
        }
    }

    /** 注册链接，跟网站“我的邀请”生成的一样 */
    fun inviteLink(code: String): String = FatbeeConfig.webUrl("register?code=$code")

    /** 公告列表：{"data":[{id,title,content,tags,created_at,...}],"total":n}（外面没有 status 包装） */
    internal fun parseNotices(text: String): List<Notice> {
        val root = try {
            JsonParser.parseString(text).asJsonObject
        } catch (e: Exception) {
            throw XboardException("Bad response")
        }
        val arr = root.get("data")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val id = o.long("id") ?: return@mapNotNull null
            val tags = o.get("tags")?.takeIf { it.isJsonArray }?.asJsonArray
                ?.mapNotNull { t -> t.takeIf { it.isJsonPrimitive }?.asString }.orEmpty()
            Notice(
                id = id,
                title = o.str("title").orEmpty(),
                content = o.str("content").orEmpty(),
                tags = tags,
                createdAt = o.long("created_at") ?: 0L,
            )
        }
    }

    internal fun parseAppVersion(text: String): AppVersion? {
        val data = runCatching { dataObject(text) }.getOrNull() ?: return null
        val version = data.str("android_version")?.trim().orEmpty()
        val url = data.str("android_download_url")?.trim().orEmpty()
        if (version.isEmpty() || url.isEmpty()) return null
        return AppVersion(version, url)
    }

    /** 比较版本号：1.2.10 比 1.2.9 新；前面带 v、后面带 -beta 之类的都能比 */
    internal fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V")
            .split('.', '-', '_').map { p -> p.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val a = parts(remote)
        val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    internal fun parseQuickLoginUrl(text: String): String {
        val root = try {
            JsonParser.parseString(text).asJsonObject
        } catch (e: Exception) {
            throw XboardException("Bad response")
        }
        val url = root.str("data")
        if (url.isNullOrBlank()) throw XboardException(root.str("message") ?: "Bad response")
        return url
    }

    /** 把 https://任意地址/#/login?verify=... 换成 面板地址/#/login?verify=... */
    internal fun rewriteQuickLoginUrl(url: String, panelUrl: String): String {
        val i = url.indexOf("/#/")
        return if (i >= 0 && panelUrl.isNotBlank()) panelUrl.trimEnd('/') + url.substring(i) else url
    }

    /** 失败时面板一般回 {"message": "..."}；表单校验失败回 {"message": "...", "errors": {...}} */
    internal fun parseErrorMessage(text: String): String? = try {
        val obj = JsonParser.parseString(text).asJsonObject
        val errors = obj.get("errors")?.takeIf { it.isJsonObject }?.asJsonObject
        val firstError = errors?.entrySet()?.firstOrNull()?.value?.let { v ->
            if (v.isJsonArray && v.asJsonArray.size() > 0) v.asJsonArray[0].asString else null
        }
        firstError ?: obj.str("message")?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun dataObject(text: String): JsonObject {
        val root = try {
            JsonParser.parseString(text).asJsonObject
        } catch (e: Exception) {
            throw XboardException("Bad response")
        }
        return root.get("data")?.takeIf { it.isJsonObject }?.asJsonObject
            ?: throw XboardException(root.str("message") ?: "Bad response")
    }

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf(JsonElement::isJsonPrimitive)?.asString

    private fun JsonObject.long(key: String): Long? =
        get(key)?.takeIf(JsonElement::isJsonPrimitive)?.let { runCatching { it.asLong }.getOrNull() }
}
