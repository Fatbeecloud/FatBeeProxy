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

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 登录页（第 3 步的简化版；第 5 步按草图再美化）。
 * 账号框里只显示会员自己输入的内容，点“登录”那一刻才偷偷补后缀。
 */
class LoginActivity : BaseComponentActivity() {

    companion object {
        const val EXTRA_MESSAGE = "fatbee_message"
    }

    @Composable
    override fun ScreenContent() {
        val notice = intent.getStringExtra(EXTRA_MESSAGE)
        LoginScreen(
            initialAccount = FatbeeSession.account,
            initialError = notice,
            onLogin = ::doLogin,
            onRegister = { FatbeeWeb.openUrl(this, FatbeeConfig.webUrl("register")) },
            onForgot = { FatbeeWeb.openUrl(this, FatbeeConfig.webUrl("forgetpassword")) },
        )
    }

    /** 返回 null 表示成功（已跳转），否则返回要显示的错误 */
    private suspend fun doLogin(account: String, password: String): String? {
        if (account.isBlank() || password.isEmpty()) return getString(R.string.fatbee_error_empty)
        val result = withContext(Dispatchers.IO) {
            try {
                FatbeeSync.login(account, password)
            } catch (e: XboardException) {
                return@withContext e.message ?: getString(R.string.fatbee_error_network)
            } catch (e: IOException) {
                return@withContext getString(R.string.fatbee_error_network)
            }
        }
        when (result) {
            is String -> return result
            is FatbeeSync.Result.Ok -> if (result.nodeCount == 0) toast(R.string.fatbee_no_nodes, long = true)
            is FatbeeSync.Result.Failed -> toast(R.string.fatbee_error_network, long = true)
            FatbeeSync.Result.SessionExpired -> return getString(R.string.fatbee_session_expired)
        }
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
        return null
    }
}

@Composable
private fun inkTextButtonColors() = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)

@Composable
private fun LoginScreen(
    initialAccount: String,
    initialError: String?,
    onLogin: suspend (String, String) -> String?,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
) {
    var account by rememberSaveable { mutableStateOf(initialAccount) }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(initialError) }
    var busy by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    val submit: () -> Unit = {
        if (!busy) {
            focus.clearFocus()
            busy = true
            error = null
            scope.launch {
                error = onLogin(account, password)
                busy = false
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_fatbee_logo),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.fatbee_login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text(stringResource(R.string.fatbee_account_hint)) },
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        // 让手机的密码管理器把这里当“用户名”，存下来的是不带后缀的账号
                        .semantics { contentType = ContentType.Username },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.fatbee_password_hint)) },
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }, colors = inkTextButtonColors()) {
                            Text(
                                stringResource(
                                    if (showPassword) R.string.fatbee_password_hide else R.string.fatbee_password_show
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password },
                )

                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = submit,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.fatbee_logging_in), fontWeight = FontWeight.Bold)
                    } else {
                        Text(stringResource(R.string.fatbee_login_button), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onRegister, colors = inkTextButtonColors()) { Text(stringResource(R.string.fatbee_register)) }
                    TextButton(onClick = onForgot, colors = inkTextButtonColors()) { Text(stringResource(R.string.fatbee_forgot)) }
                }
                Text(
                    text = stringResource(R.string.fatbee_register_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                )
            }
        }
    }
}
