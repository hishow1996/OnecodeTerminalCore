package com.ai.assistance.onecode.terminal.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.onecode.terminal.TerminalEnv
import com.ai.assistance.onecode.terminal.utils.TerminalFontConfigManager
import com.ai.assistance.onecode.terminal.view.SyntaxColors
import com.ai.assistance.onecode.terminal.view.canvas.CanvasTerminalScreen

private val ShellBlack = Color(0xFF0B0D0E)
private val BarBlack = Color(0xFF101314)
private val KeyBlack = Color(0xFF171B1C)
private val Selected = Color(0xFF1A1F20)
private val Muted = Color(0xFF8C9698)
private val TextPrimary = Color(0xFFE6ECEB)
private val Green = Color(0xFF8CCF7E)

private fun ctrl(code: Int): String = String(charArrayOf(code.toChar()))
private const val ESC = "\u001B"

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerminalHomeV2(
    env: TerminalEnv,
    onNavigateToSetup: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val fontConfigManager = remember { TerminalFontConfigManager.getInstance(context) }
    var fontConfig by remember { mutableStateOf(fontConfigManager.loadRenderConfig()) }

    LaunchedEffect(Unit) { fontConfig = fontConfigManager.loadRenderConfig() }

    val currentSession = env.sessions.firstOrNull { it.id == env.currentSessionId }
    val currentPty = remember(env.currentSessionId, env.sessions) {
        env.sessions.firstOrNull { it.id == env.currentSessionId }?.pty
    }

    Column(Modifier.fillMaxSize().background(ShellBlack).imePadding()) {
        TopBar(env, currentSession?.title?.ifBlank { "Ubuntu" } ?: "Ubuntu", env::onNewSession, onNavigateToSettings)
        CanvasTerminalScreen(
            emulator = env.terminalEmulator,
            modifier = Modifier.weight(1f),
            config = fontConfig,
            pty = currentPty,
            onInput = { env.onSendInput(it, false) },
            sessionId = env.currentSessionId,
            onScrollOffsetChanged = { id, offset -> env.saveScrollOffset(id, offset) },
            getScrollOffset = { id -> env.getScrollOffset(id) }
        )
        QuickBar(onInput = { env.onSendInput(it, false) }, onCommand = { env.onSendInput(it, true) }, onInterrupt = env::onInterrupt)
        CommandBar(
            command = env.command,
            prompt = env.currentDirectory.ifEmpty { "~" },
            onCommandChange = env::onCommandChange,
            onSend = { env.onSendInput(env.command, true); keyboard?.hide() },
            onKeyboard = { focusRequester.requestFocus(); keyboard?.show() },
            focusRequester = focusRequester
        )
    }
}

@Composable
private fun TopBar(env: TerminalEnv, sessionTitle: String, onNewSession: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.background(BarBlack)) {
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ubuntu", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                Text(sessionTitle, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 1)
            }
            Box(Modifier.size(7.dp).background(Green, RoundedCornerShape(50)))
            Spacer(Modifier.width(7.dp))
            IconButton(onClick = onNewSession, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Add, "New session", tint = TextPrimary) }
            IconButton(onClick = onSettings, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Settings, "Settings", tint = Muted) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            env.sessions.forEach { session ->
                SessionChip(session.title.ifBlank { "Ubuntu" }, session.id == env.currentSessionId, env.sessions.size > 1, { env.onSwitchSession(session.id) }, { env.onCloseSession(session.id) })
            }
        }
    }
}

@Composable
private fun SessionChip(title: String, selected: Boolean, canClose: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    Row(Modifier.background(if (selected) Selected else BarBlack, RoundedCornerShape(7.dp)).clickable(onClick = onClick).padding(start = 11.dp, end = if (canClose) 3.dp else 11.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = if (selected) TextPrimary else Muted, fontSize = 12.sp)
        if (canClose) IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Close", tint = Muted, modifier = Modifier.size(15.dp)) }
    }
}

@Composable
private fun QuickBar(onInput: (String) -> Unit, onCommand: (String) -> Unit, onInterrupt: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(BarBlack).horizontalScroll(rememberScrollState()).padding(horizontal = 9.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        QuickKey("Ctrl+C", onInterrupt)
        QuickKey("Ctrl+D") { onInput(ctrl(4)) }
        QuickKey("Ctrl+L") { onInput(ctrl(12)) }
        QuickKey("Ctrl+A") { onInput(ctrl(1)) }
        QuickKey("Ctrl+E") { onInput(ctrl(5)) }
        QuickKey("Ctrl+U") { onInput(ctrl(21)) }
        QuickKey("Ctrl+K") { onInput(ctrl(11)) }
        QuickKey("Ctrl+W") { onInput(ctrl(23)) }
        QuickKey("↑") { onInput(ESC + "[A") }
        QuickKey("↓") { onInput(ESC + "[B") }
        QuickKey("Tab") { onInput("\t") }
        QuickKey("ls") { onCommand("ls -la") }
        QuickKey("pwd") { onCommand("pwd") }
        QuickKey("top") { onCommand("top") }
        QuickKey("clear") { onCommand("clear") }
        QuickKey("Home") { onCommand("cd ~") }
    }
}

@Composable
private fun QuickKey(label: String, onClick: () -> Unit) {
    Text(label, color = Color(0xFFC3CCCD), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.background(KeyBlack, RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp))
}

@Composable
private fun CommandBar(command: String, prompt: String, onCommandChange: (String) -> Unit, onSend: () -> Unit, onKeyboard: () -> Unit, focusRequester: FocusRequester) {
    Row(Modifier.fillMaxWidth().background(ShellBlack).padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("›", color = Green, fontFamily = FontFamily.Monospace, fontSize = 19.sp)
        Spacer(Modifier.width(6.dp))
        Text(prompt.takeLast(24), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.width(7.dp))
        BasicTextField(value = command, onValueChange = onCommandChange, modifier = Modifier.weight(1f).focusRequester(focusRequester), singleLine = true, textStyle = TextStyle(color = SyntaxColors.commandDefault, fontFamily = FontFamily.Monospace, fontSize = 13.sp), cursorBrush = SolidColor(Green), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { onSend() }))
        Spacer(Modifier.width(8.dp))
        Text("⌨", color = Muted, fontSize = 17.sp, modifier = Modifier.clickable(onClick = onKeyboard).padding(5.dp))
        Text("↵", color = Green, fontFamily = FontFamily.Monospace, fontSize = 17.sp, modifier = Modifier.clickable(onClick = onSend).padding(start = 8.dp, end = 3.dp, top = 5.dp, bottom = 5.dp))
    }
}
