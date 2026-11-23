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
package com.v2ray.ang.fatbee.ui

import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import com.v2ray.ang.fatbee.XboardApi
import com.v2ray.ang.fatbee.XboardException
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.enums.Language
import com.v2ray.ang.fatbee.FatbeeConfig
import com.v2ray.ang.fatbee.FatbeeMode
import com.v2ray.ang.fatbee.FatbeeNews
import com.v2ray.ang.fatbee.FatbeeText
import com.v2ray.ang.fatbee.Notice
import com.v2ray.ang.fatbee.AppVersion
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.fatbee.AccountName
import com.v2ray.ang.fatbee.FatbeePrefs
import com.v2ray.ang.fatbee.FatbeeSession
import com.v2ray.ang.fatbee.FatbeeWeb
import com.v2ray.ang.fatbee.SubscribeInfo
import com.v2ray.ang.ui.main.MainAction
import com.v2ray.ang.ui.main.MainViewModel
import com.v2ray.ang.ui.main.ServerRowUiModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// 胖蜜蜂配色（深浅模式通用的固定色；其余颜色跟 MaterialTheme 走）
private val Honey = Color(0xFFF5B800)
private val HoneyPale = Color(0xFFFFF4CC)
private val Ink = Color(0xFF14130F)
private val Good = Color(0xFF16A34A)
private val Warn = Color(0xFFD97706)
private val Bad = Color(0xFFDC2626)

private enum class FbTab { Home, Nodes, Me }

/**
 * FatBeeCloud 新界面（首页 / 节点 / 我的）。连接、测速、选节点都用原版 MainViewModel，
 * 这里只负责“长什么样”。界面草图：docs/design/界面草图-v1.png
 */
@Composable
internal fun FatbeeMainScreen(
    viewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (FbScreen) -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupFlow = remember(uiState.selectedGroupId) { viewModel.serverGroupState(uiState.selectedGroupId) }
    val groupState by groupFlow.collectAsState()
    val rows = groupState.rows
    val info by FatbeeSession.info.collectAsState()
    var tab by rememberSaveable { mutableStateOf(FbTab.Home) }
    val notices by FatbeeNews.notices.collectAsState()
    val seenId by FatbeeNews.seenId.collectAsState()
    val update by FatbeeNews.update.collectAsState()
    var noticeDialog by remember { mutableStateOf<Notice?>(null) }
    var noticeList by remember { mutableStateOf(false) }
    var updateDialog by remember { mutableStateOf<AppVersion?>(null) }

    // 带“弹窗”标签、还没弹过的公告：打开 App 弹一次
    LaunchedEffect(notices) {
        FatbeeNews.nextPopup()?.let { n ->
            FatbeeNews.markPopped(n.id)
            noticeDialog = n
        }
    }
    // 有新版本：自动提示一次（会员点“以后再说”后这个版本不再自动弹）
    LaunchedEffect(update) {
        update?.let { v -> if (FatbeeNews.shouldPromptUpdate(v)) updateDialog = v }
    }

    BackHandler(enabled = tab != FbTab.Home) { tab = FbTab.Home }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                listOf(
                    Triple(FbTab.Home, R.drawable.ic_fb_home, R.string.fatbee_tab_home),
                    Triple(FbTab.Nodes, R.drawable.ic_fb_nodes, R.string.fatbee_tab_nodes),
                    Triple(FbTab.Me, R.drawable.ic_fb_me, R.string.fatbee_tab_me),
                ).forEach { (t, icon, label) ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(painterResource(icon), contentDescription = null) },
                        label = { Text(stringResource(label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = Honey,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                FbTab.Home -> HomeTab(
                    isRunning = uiState.isRunning,
                    statusText = viewModel.formatStatus(uiState.status),
                    current = rows.firstOrNull { it.guid == uiState.selectedGuid },
                    info = info,
                    onToggle = { onAction(MainAction.ToggleService) },
                    onTestCurrent = { onAction(MainAction.TestCurrentServer) },
                    onOpenNodes = { tab = FbTab.Nodes },
                    latestNotice = notices.firstOrNull(),
                    hasUnread = (notices.maxOfOrNull { it.id } ?: 0L) > seenId,
                    onOpenNotices = {
                        FatbeeNews.markAllSeen()
                        noticeList = true
                    },
                    onOpenNotice = { noticeDialog = it },
                )

                FbTab.Nodes -> NodesTab(
                    rows = rows,
                    selectedGuid = uiState.selectedGuid,
                    isTesting = uiState.isTesting,
                    onSelect = { guid -> onAction(MainAction.SelectServer(guid)) },
                    onTest = { tcp -> onAction(if (tcp) MainAction.TestAllServers else MainAction.TestRealAllServers) },
                    onRefresh = onRefresh,
                )

                FbTab.Me -> MeTab(
                    info = info, isRunning = uiState.isRunning, update = update,
                    onUpdate = { updateDialog = it }, onOpen = onOpen, onLogout = onLogout,
                )
            }
        }
    }

    noticeDialog?.let { n -> NoticeDialog(n, onDismiss = { noticeDialog = null }) }
    if (noticeList) {
        NoticeListDialog(
            notices = notices,
            onOpen = { noticeList = false; noticeDialog = it },
            onDismiss = { noticeList = false },
        )
    }
    updateDialog?.let { v -> UpdateDialog(v, onDismiss = { FatbeeNews.dismissUpdate(v); updateDialog = null }) }
}

// ============================== 首页 ==============================

@Composable
private fun HomeTab(
    isRunning: Boolean,
    statusText: String,
    current: ServerRowUiModel?,
    info: SubscribeInfo?,
    onToggle: () -> Unit,
    onTestCurrent: () -> Unit,
    onOpenNodes: () -> Unit,
    latestNotice: Notice?,
    hasUnread: Boolean,
    onOpenNotices: () -> Unit,
    onOpenNotice: (Notice) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showInvite by remember { mutableStateOf(false) }
    if (showInvite) InviteDialog(onDismiss = { showInvite = false })

    // “正在连接…”：点了连接、服务还没起来的这段时间
    var pending by remember { mutableStateOf(false) }
    LaunchedEffect(isRunning) {
        pending = false
        if (isRunning && FatbeePrefs.connectedAt == 0L) FatbeePrefs.connectedAt = System.currentTimeMillis()
        if (!isRunning) FatbeePrefs.connectedAt = 0L
    }
    LaunchedEffect(pending) {
        if (pending) {
            delay(20_000)
            pending = false
        }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_fatbee_logo), contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.app_name), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = onOpenNotices) {
                    Icon(painterResource(R.drawable.ic_fb_bell), contentDescription = stringResource(R.string.fatbee_notices))
                }
                if (hasUnread) {
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(9.dp).clip(CircleShape).background(Bad))
                }
            }
        }

        // 大连接按钮
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConnectButton(
                isRunning = isRunning,
                pending = pending && !isRunning,
                onClick = {
                    if (!isRunning) pending = true
                    onToggle()
                },
            )
            Spacer(Modifier.height(14.dp))
            val connectedAt = FatbeePrefs.connectedAt
            val sub = when {
                isRunning && connectedAt > 0 -> stringResource(R.string.fatbee_connected_for, formatDuration(now - connectedAt))
                isRunning -> stringResource(R.string.fatbee_connected)
                pending -> stringResource(R.string.fatbee_connecting)
                else -> stringResource(R.string.fatbee_disconnected)
            }
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }

        AccountWarning(info) { FatbeeWeb.openMemberPage(context, scope, "plan") }

        // 当前节点
        Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onOpenNodes)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        current?.remarks ?: stringResource(R.string.fatbee_no_nodes_short),
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        current?.let { protocolLabel(it) }.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                    )
                }
                if (current != null) {
                    DelayText(
                        delay = current.testDelayMillis,
                        modifier = Modifier.clickable(enabled = isRunning, onClick = onTestCurrent).padding(8.dp),
                    )
                }
                Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isRunning && statusText.isNotBlank()) {
            Text(
                statusText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp).clickable(onClick = onTestCurrent),
            )
        }

        // 剩余流量 + 到期时间
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.weight(1f)) {
                Label(stringResource(R.string.fatbee_remaining))
                Text(info?.let { gb(it.remaining) } ?: "—", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                val used = info?.let { if (it.total > 0) ((it.upload + it.download).toFloat() / it.total).coerceIn(0f, 1f) else 0f } ?: 0f
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 1f - used },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)),
                    color = Honey,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(6.dp))
                if (info != null) {
                    Label(
                        if (info.resetDay != null) stringResource(R.string.fatbee_total_reset, gb(info.total), info.resetDay)
                        else stringResource(R.string.fatbee_total, gb(info.total))
                    )
                }
            }
            Card(Modifier.weight(1f)) {
                Label(stringResource(R.string.fatbee_expiry))
                val exp = info?.expiredAt
                if (info != null && exp == null) {
                    Text(stringResource(R.string.fatbee_long_term), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                } else if (exp != null) {
                    val date = Instant.ofEpochSecond(exp).atZone(ZoneId.systemDefault()).toLocalDate()
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
                    Text(date.format(DateTimeFormatter.ofPattern("MM-dd")), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Label(
                        if (days < 0) stringResource(R.string.fatbee_expired)
                        else stringResource(R.string.fatbee_days_left, date.year.toString(), days.toInt())
                    )
                } else {
                    Text("—", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                info?.planName?.let {
                    Spacer(Modifier.height(4.dp))
                    Label(it)
                }
            }
        }

        // 快捷按钮
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickButton(stringResource(R.string.fatbee_buy), "🛒", highlight = true, modifier = Modifier.weight(1f)) {
                FatbeeWeb.openMemberPage(context, scope, "plan")
            }
            QuickButton(stringResource(R.string.fatbee_invite), "🎟️", modifier = Modifier.weight(1f)) {
                showInvite = true
            }
            QuickButton(stringResource(R.string.fatbee_support), "💬", modifier = Modifier.weight(1f)) {
                FatbeeWeb.openMemberPage(context, scope, "ticket")
            }
        }

        // 最新公告（没有公告就不显示）
        latestNotice?.let { n ->
            Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).clickable { onOpenNotice(n) }) {
                Text("📢 " + stringResource(R.string.fatbee_notices), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                if (n.title.isNotBlank()) Text(n.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    FatbeeText.plain(n.content), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 生成邀请码：跟网站“我的邀请”用同一个接口，每人每天 1 个、同时 1 个未使用的限制由面板统一检查 */
@Composable
private fun InviteDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var code by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val networkError = stringResource(R.string.fatbee_error_network)

    fun run(generate: Boolean) {
        val auth = FatbeeSession.authData ?: return
        loading = true
        error = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (generate) XboardApi.generateInviteCode(auth)
                    XboardApi.fetchInviteCodes(auth).firstOrNull()
                }
            }
            result.onSuccess { code = it }.onFailure { e ->
                error = (e as? XboardException)?.message ?: networkError
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { run(generate = false) }

    fun copy(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("FatBeeProxy", text))
        context.toast(R.string.fatbee_copied)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fatbee_invite_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                val c = code
                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Honey)
                    }
                    c != null -> {
                        Text(
                            c, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(HoneyPale)
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center, color = Ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        Label(stringResource(R.string.fatbee_invite_link_label))
                        Text(XboardApi.inviteLink(c), fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SmallAction(stringResource(R.string.fatbee_invite_copy_code), Modifier.weight(1f)) { copy(c) }
                            SmallAction(stringResource(R.string.fatbee_invite_copy_link), Modifier.weight(1f)) { copy(XboardApi.inviteLink(c)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        SmallAction(stringResource(R.string.fatbee_invite_share), Modifier.fillMaxWidth(), highlight = true) {
                            val text = context.getString(R.string.fatbee_invite_share_text, c, XboardApi.inviteLink(c))
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                            context.startActivity(Intent.createChooser(send, null))
                        }
                    }
                    else -> {
                        Text(stringResource(R.string.fatbee_invite_none), fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))
                        SmallAction(stringResource(R.string.fatbee_invite_generate), Modifier.fillMaxWidth(), highlight = true) { run(generate = true) }
                    }
                }
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.fatbee_invite_rules), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.fatbee_close), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

@Composable
private fun SmallAction(text: String, modifier: Modifier = Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) Honey else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = if (highlight) Ink else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
        )
    }
}

/** 套餐到期 / 流量用完 / 快到期 / 流量快用完：首页显示一条醒目提示，带“去续费” */
@Composable
private fun AccountWarning(info: SubscribeInfo?, onRenew: () -> Unit) {
    info ?: return
    val nowSec = System.currentTimeMillis() / 1000
    val exp = info.expiredAt
    val daysLeft = exp?.let { ((it - nowSec) / 86400).toInt() }
    val expired = exp != null && exp <= nowSec
    val noData = info.total > 0 && info.remaining <= 0
    val (text, strong) = when {
        expired -> stringResource(R.string.fatbee_warn_expired) to true
        noData -> stringResource(R.string.fatbee_warn_no_traffic) to true
        daysLeft != null && daysLeft <= 3 -> stringResource(R.string.fatbee_warn_expiring, daysLeft.coerceAtLeast(0)) to false
        info.total > 0 && info.remaining < info.total / 10 -> stringResource(R.string.fatbee_warn_low_traffic, gb(info.remaining)) to false
        else -> return
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (strong) Color(0xFFFDECEC) else HoneyPale,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                color = if (strong) Color(0xFF9B1C1C) else Color(0xFF3D2E00),
            )
            TextButton(onClick = onRenew) {
                Text(stringResource(R.string.fatbee_renew_now), color = Ink, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NoticeDialog(n: Notice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(n.title.ifBlank { stringResource(R.string.fatbee_notices) }, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (n.createdAt > 0) {
                    Text(
                        Instant.ofEpochSecond(n.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(FatbeeText.plain(n.content), fontSize = 14.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.fatbee_close), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

@Composable
private fun NoticeListDialog(notices: List<Notice>, onOpen: (Notice) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fatbee_notices), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (notices.isEmpty()) {
                    Text(stringResource(R.string.fatbee_no_notices), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                notices.forEachIndexed { i, n ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.fillMaxWidth().clickable { onOpen(n) }.padding(vertical = 10.dp)) {
                        Text(n.title.ifBlank { FatbeeText.plain(n.content).take(30) }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (n.createdAt > 0) {
                            Text(
                                Instant.ofEpochSecond(n.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.fatbee_close), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

@Composable
private fun UpdateDialog(v: AppVersion, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fatbee_update_title, v.version), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.fatbee_update_body, BuildConfig.VERSION_NAME)) },
        confirmButton = {
            TextButton(onClick = { FatbeeWeb.openUrl(context, v.downloadUrl); onDismiss() }) {
                Text(stringResource(R.string.fatbee_update_download), color = Ink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.fatbee_update_later), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

@Composable
private fun ConnectButton(isRunning: Boolean, pending: Boolean, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminanceLow()
    val ring = if (isRunning) HoneyPale.copy(alpha = if (dark) 0.25f else 1f) else MaterialTheme.colorScheme.surfaceVariant
    val fill = if (isRunning) Honey else MaterialTheme.colorScheme.surfaceContainerLow
    val content = if (isRunning) Ink else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(184.dp)
            .shadow(if (isRunning) 18.dp else 6.dp, CircleShape, ambientColor = Honey, spotColor = if (isRunning) Honey else Color.Black)
            .clip(CircleShape)
            .background(ring)
            .padding(10.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(enabled = !pending, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (pending) {
                CircularProgressIndicator(color = Honey, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
            } else {
                PowerIcon(color = content, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    when {
                        isRunning -> R.string.fatbee_connected
                        pending -> R.string.fatbee_connecting
                        else -> R.string.fatbee_tap_to_connect
                    }
                ),
                color = content, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/** 电源图标（画出来的，不依赖字体） */
@Composable
private fun PowerIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.11f
        val inset = stroke / 2 + size.minDimension * 0.06f
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = Offset(size.width / 2, size.height * 0.04f),
            end = Offset(size.width / 2, size.height * 0.46f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun QuickButton(text: String, emoji: String, highlight: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminanceLow()
    // 浅色模式用墨黑高亮；深色模式下墨黑会和背景混在一起，改用蜂蜜黄
    val hiBg = if (dark) Honey else Ink
    val hiFg = if (dark) Ink else Color.White
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (highlight) hiBg else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (highlight) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = if (highlight) hiFg else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ============================== 节点页 ==============================

@Composable
private fun NodesTab(
    rows: List<ServerRowUiModel>,
    selectedGuid: String?,
    isTesting: Boolean,
    onSelect: (String) -> Unit,
    onTest: (tcp: Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("") }
    var testMenu by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val categories = remember(rows) { rows.map { protocolCategory(it) }.distinct() }
    val shown = remember(rows, query, filter) {
        val tested = rows.any { it.testDelayMillis != 0L }
        rows
            .filter { filter.isEmpty() || protocolCategory(it) == filter }
            .filter { query.isBlank() || it.remarks.contains(query.trim(), ignoreCase = true) }
            .let { list ->
                // 测过速就按快慢排（连不上的放最后），没测过保持站长后台的顺序
                if (!tested) list else list.sortedBy { if (it.testDelayMillis > 0) it.testDelayMillis else Long.MAX_VALUE }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.fatbee_nodes_title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh) {
                    Icon(painterResource(R.drawable.ic_refresh_24dp), contentDescription = stringResource(R.string.fatbee_refresh))
                }
                Box {
                    TextButton(onClick = { testMenu = true }, enabled = !isTesting) {
                        Text(
                            if (isTesting) stringResource(R.string.fatbee_testing) else stringResource(R.string.fatbee_test) + " ▾",
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                        )
                    }
                    DropdownMenu(expanded = testMenu, onDismissRequest = { testMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.fatbee_test_real)) }, onClick = { testMenu = false; onTest(false) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.fatbee_test_tcp)) }, onClick = { testMenu = false; onTest(true) })
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.fatbee_search_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search_24dp), contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                // 输入管理员暗号后按回车：切换管理员模式，并清空搜索框（暗号不留在屏幕上）
                keyboardActions = KeyboardActions(onSearch = {
                    val admin = FatbeePrefs.tryAdminCode(query)
                    if (admin != null) {
                        query = ""
                        context.toast(if (admin) R.string.fatbee_admin_on else R.string.fatbee_admin_off)
                    }
                    focus.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (categories.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                ) {
                    (listOf("") + categories).forEach { c ->
                        val on = filter == c
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (on) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent)
                                .clickable { filter = c }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (c.isEmpty()) stringResource(R.string.fatbee_filter_all) else c, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                shown.forEachIndexed { i, row ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NodeLine(
                        selected = row.guid == selectedGuid,
                        title = row.remarks,
                        subtitle = protocolLabel(row),
                        delay = row.testDelayMillis,
                        onClick = { onSelect(row.guid) },
                    )
                }
                if (rows.isEmpty()) {
                    Text(
                        stringResource(R.string.fatbee_no_nodes_short),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.fatbee_nodes_footer),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

@Composable
private fun NodeLine(
    selected: Boolean,
    title: String,
    subtitle: String,
    delay: Long?,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(if (selected) 6.dp else 2.dp, if (selected) Honey else MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A6500),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(HoneyPale).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (delay != null) DelayText(delay, Modifier.padding(start = 8.dp))
    }
}

// ============================== 我的 ==============================

/** “我的”页要打开的原版页面，由 MainActivity 负责打开（返回时它会处理需要重连的设置） */
internal enum class FbScreen { PerAppProxy, Settings, Routing }

@Composable
private fun MeTab(
    info: SubscribeInfo?,
    isRunning: Boolean,
    update: AppVersion?,
    onUpdate: (AppVersion) -> Unit,
    onOpen: (FbScreen) -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val admin by FatbeePrefs.adminUnlocked.collectAsState()
    val mode by FatbeeMode.mode.collectAsState()
    val account = AccountName.display(info?.email?.takeIf { it.isNotBlank() } ?: FatbeeSession.account)
    var dialog by remember { mutableStateOf<String?>(null) }

    val modeName = stringResource(if (mode == FatbeeMode.Mode.SMART) R.string.fatbee_mode_smart else R.string.fatbee_mode_global)
    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val langName = when {
        currentLang.isBlank() -> stringResource(R.string.fatbee_lang_auto)
        currentLang.startsWith("zh-TW") || currentLang.startsWith("zh-Hant") -> "繁體中文"
        currentLang.startsWith("zh") -> "简体中文"
        currentLang.startsWith("en") -> "English"
        else -> Locale.forLanguageTag(currentLang).getDisplayName(Locale.forLanguageTag(currentLang))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Text(
            stringResource(R.string.fatbee_tab_me), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
        Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(HoneyPale), contentAlignment = Alignment.Center) {
                    Text("🐝", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(account, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val plan = info?.planName
                    val exp = info?.expiredAt?.let {
                        Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    val line = listOfNotNull(plan, exp?.let { stringResource(R.string.fatbee_me_expires, it) }).joinToString(" · ")
                    if (line.isNotEmpty()) Text(line, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            MeLine(stringResource(R.string.fatbee_buy), "↗") { FatbeeWeb.openMemberPage(context, scope, "plan") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MeLine(stringResource(R.string.fatbee_my_orders), "↗") { FatbeeWeb.openMemberPage(context, scope, "order") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MeLine(stringResource(R.string.fatbee_recovery_code), "↗") { FatbeeWeb.openPanelPath(context, "/fatbee/recovery") }
        }
        Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            MeLine(stringResource(R.string.fatbee_connection_mode), "$modeName ›") { dialog = "mode" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MeLine(stringResource(R.string.fatbee_per_app), "›", subtitle = stringResource(R.string.fatbee_per_app_desc)) { onOpen(FbScreen.PerAppProxy) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MeLine(stringResource(R.string.fatbee_language), "$langName ›") { dialog = "lang" }
        }
        if (update != null) {
            Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                MeLine("🆕 " + stringResource(R.string.fatbee_update_title, update.version), "›") { onUpdate(update) }
            }
        }
        Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            MeLine(stringResource(R.string.fatbee_advanced), "›", subtitle = stringResource(R.string.fatbee_advanced_desc)) { onOpen(FbScreen.Settings) }
            if (admin) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MeLine(stringResource(R.string.fatbee_routing_rules), "›") { onOpen(FbScreen.Routing) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MeLine(stringResource(R.string.fatbee_original_ui), "›") { FatbeePrefs.setOriginalUi(true) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MeLine(stringResource(R.string.fatbee_about), stringResource(R.string.fatbee_version, BuildConfig.VERSION_NAME) + " ›") { dialog = "about" }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).clickable(onClick = onLogout),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                stringResource(R.string.fatbee_logout), color = Bad, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(14.dp), textAlign = TextAlign.Center,
            )
        }
    }

    when (dialog) {
        "mode" -> ChoiceDialog(
            title = stringResource(R.string.fatbee_connection_mode),
            options = listOf(
                Triple(FatbeeMode.Mode.SMART, stringResource(R.string.fatbee_mode_smart), stringResource(R.string.fatbee_mode_smart_desc)),
                Triple(FatbeeMode.Mode.GLOBAL, stringResource(R.string.fatbee_mode_global), stringResource(R.string.fatbee_mode_global_desc)),
            ),
            selected = mode,
            onDismiss = { dialog = null },
            onPick = { picked ->
                dialog = null
                if (picked != mode) {
                    val name = context.getString(if (picked == FatbeeMode.Mode.SMART) R.string.fatbee_mode_smart else R.string.fatbee_mode_global)
                    scope.launch {
                        withContext(Dispatchers.IO) { FatbeeMode.apply(context.applicationContext, picked, isRunning) }
                        context.toast(context.getString(R.string.fatbee_mode_changed, name))
                    }
                }
            },
        )

        "lang" -> ChoiceDialog(
            title = stringResource(R.string.fatbee_language),
            options = listOf(
                Triple(Language.AUTO, stringResource(R.string.fatbee_lang_auto), null),
                Triple(Language.CHINA, "简体中文", null),
                Triple(Language.TRADITIONAL_CHINESE, "繁體中文", null),
                Triple(Language.ENGLISH, "English", null),
            ),
            selected = when {
                currentLang.isBlank() -> Language.AUTO
                currentLang.startsWith("zh-TW") || currentLang.startsWith("zh-Hant") -> Language.TRADITIONAL_CHINESE
                currentLang.startsWith("zh") -> Language.CHINA
                currentLang.startsWith("en") -> Language.ENGLISH
                else -> null
            },
            onDismiss = { dialog = null },
            onPick = { picked ->
                dialog = null
                AppLocaleManager.setApplicationLanguage(picked.code)
            },
        )

        "about" -> AboutDialog(onDismiss = { dialog = null })
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Triple<T, String, String?>>,
    selected: T?,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (value, label, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(value) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val on = value == selected
                        Box(
                            Modifier.size(20.dp).clip(CircleShape)
                                .border(if (on) 6.dp else 2.dp, if (on) Honey else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (desc != null) Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.fatbee_close), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }
    if (showLicenses) {
        // 开源许可证：原版自带的清单（GPL 要求保留）
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(stringResource(R.string.fatbee_oss_license)) },
            text = {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    factory = { ctx -> WebView(ctx).apply { loadUrl("file:///android_asset/open_source_licenses.html") } },
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) { Text(stringResource(R.string.fatbee_close), color = MaterialTheme.colorScheme.onSurface) }
            },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Image(painterResource(R.drawable.ic_fatbee_logo), contentDescription = null, modifier = Modifier.size(56.dp)) },
        title = { Text("${stringResource(R.string.app_name)}  ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // 署名（ADDITIONAL_TERMS.md 要求改装版保留，不要删改）
                Text(stringResource(R.string.fatbee_attribution), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.fatbee_about_based), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                MeLine(stringResource(R.string.fatbee_original_project), "↗") { FatbeeWeb.openUrl(context, FatbeeConfig.ORIGINAL_PROJECT_URL) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (FatbeeConfig.WEBSITE_URL.isNotBlank()) {
                    MeLine(stringResource(R.string.fatbee_website), "↗") { FatbeeWeb.openUrl(context, FatbeeConfig.WEBSITE_URL) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    MeLine(stringResource(R.string.fatbee_privacy), "↗") { FatbeeWeb.openUrl(context, FatbeeConfig.WEBSITE_URL + "/privacy") }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (FatbeeConfig.SOURCE_URL.isNotBlank()) {
                    MeLine(stringResource(R.string.fatbee_source_code), "↗") { FatbeeWeb.openUrl(context, FatbeeConfig.SOURCE_URL) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                MeLine(stringResource(R.string.fatbee_oss_license), "›") { showLicenses = true }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.fatbee_close), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

@Composable
private fun MeLine(title: String, trailing: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

// ============================== 小部件 ==============================

@Composable
private fun Card(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(padding)) { content() }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DelayText(delay: Long, modifier: Modifier = Modifier) {
    val (text, color) = when {
        delay > 0 -> "${delay}ms" to when {
            delay < 300 -> Good
            delay < 800 -> Warn
            else -> Bad
        }
        delay < 0 -> stringResource(R.string.fatbee_timeout) to Bad
        else -> "" to Color.Unspecified
    }
    if (text.isNotEmpty()) Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = modifier)
}

private fun protocolCategory(row: ServerRowUiModel): String {
    val p = row.profile
    return when {
        p.security.equals("reality", ignoreCase = true) -> "Reality"
        p.configType == EConfigType.HYSTERIA2 -> "Hysteria2"
        else -> p.configType.name.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}

private fun protocolLabel(row: ServerRowUiModel): String {
    val p = row.profile
    return when {
        p.configType == EConfigType.VLESS && p.security.equals("reality", ignoreCase = true) -> "VLESS Reality"
        p.configType == EConfigType.HYSTERIA2 -> "Hysteria2"
        else -> row.typeDescription
    }
}

private fun gb(bytes: Long): String = String.format(Locale.ROOT, "%.1f GB", bytes / 1073741824.0)

private fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

private fun Color.luminanceLow(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f
