package com.rokid.relayhud

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    private lateinit var client: RelayClient
    private lateinit var voice: VoiceInput
    private val hud = HudState()
    private val conn = mutableStateOf("连接中…")
    private val audioGranted = mutableStateOf(false)
    @Volatile private var recording = false
    @Volatile private var running = false
    private var choiceState: ChoiceState? = null
    private val countdown = Handler(Looper.getMainLooper())
    private var secondsLeft = 60
    private var choiceTimeoutDefault = "拒绝"
    private val requestAudio =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { audioGranted.value = it }

    // 含 whisper 同音误识(会话/绘画/汇话 同音 huìhuà;对话/对画)
    private val newSessionPhrases = setOf(
        "新会话", "新对话", "开新对话", "新的对话", "开始新对话",
        "新绘画", "新绘话", "新汇话", "新对画", "心会话",
    )

    // 含 whisper 同音(退/推 tuì)
    private val exitPhrases = setOf("退出", "关闭", "退下", "推出", "退出程序", "关掉")

    /** 读 adb push 进来的外部配置文件;不存在/读失败则回退本地默认。 */
    private fun loadConfig(): AppConfig {
        return try {
            val f = java.io.File(getExternalFilesDir(null), "config.json")
            if (f.exists()) parseConfig(f.readText()) else DEFAULT_CONFIG
        } catch (_: Exception) {
            DEFAULT_CONFIG
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioGranted.value =
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        val cfg = loadConfig()
        client = RelayClient(
            url = buildWsUrl(cfg.serverUrl, cfg.token),
            onMessage = { msg ->
                if (msg is ServerMessage.Usage) {
                    hud.statusline = statuslineText(msg.model, msg.costUsd, msg.tokens)
                } else if (msg is ServerMessage.ModelRequest) {
                    enterModelChoice(msg)
                } else if (msg is ServerMessage.PermissionRequest) {
                    enterChoice(msg)
                } else if (msg is ServerMessage.Transcript) {
                    val t = msg.text.trim().trim('。', '，', '!', '！', '.', ' ')
                    when {
                        t.isEmpty() -> hud.status = "(没听到内容)"
                        newSessionPhrases.contains(t) -> {
                            client.newSession(); hud.clear(); hud.status = "🆕 新会话"
                        }
                        exitPhrases.contains(t) -> { hud.status = "👋 退出"; finish() }
                        else -> {
                            hud.add("▶ $t", Color(0xFF00AA77)); hud.status = "提交中…"; running = true; refreshKeepOn()
                            client.sendPrompt(t)
                        }
                    }
                } else {
                    if (msg is ServerMessage.RunEnd) { running = false; refreshKeepOn() }
                    handle(msg, hud)
                }
            },
            onStatus = { conn.value = it },
        )
        client.connect()

        voice = VoiceInput(
            context = this,
            onAudio = { b64 -> recording = false; hud.recording = false; hud.status = "转写中…"; refreshKeepOn(); client.sendAudio(b64) },
            onError = { recording = false; hud.recording = false; hud.status = it; refreshKeepOn() },
        )

        if (!audioGranted.value) requestAudio.launch(Manifest.permission.RECORD_AUDIO)

        setContent { HudScreen(state = hud, connStatus = conn.value) }
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (hud.blanked) {
            // 灭屏态:吞掉所有事件;仅"真手势"(单击/双击/滑动)才唤醒,避免预触发漏到 onTap
            if (Gestures.map(keyCode, android.view.KeyEvent.ACTION_UP) != null ||
                keyCode == android.view.KeyEvent.KEYCODE_BACK) setBlanked(false)
            return true
        }
        if (SystemClock.uptimeMillis() - wokeAt < 700) return true   // 唤醒余波:吞掉同一手势的后续事件
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) { setBlanked(true); return true }  // 双击=主动灭屏
        // 选择模式优先截胡:前/后滑移动高亮,单击确认。
        val p = hud.choice
        val cs = choiceState
        if (p != null && cs != null) {
            when (Gestures.map(keyCode, android.view.KeyEvent.ACTION_UP)) {
                GestureAction.SCROLL_UP -> { cs.move(-1); hud.choice = p.copy(highlight = cs.highlight) }
                GestureAction.SCROLL_DOWN -> { cs.move(1); hud.choice = p.copy(highlight = cs.highlight) }
                GestureAction.TAP -> submitChoice(cs.confirm())
                null -> return super.onKeyUp(keyCode, event)
            }
            return true
        }
        return when (Gestures.map(keyCode, android.view.KeyEvent.ACTION_UP)) {
            GestureAction.TAP -> { onTap(); true }
            GestureAction.SCROLL_UP -> { hud.scroll(-1); true }
            GestureAction.SCROLL_DOWN -> { hud.scroll(1); true }
            null -> super.onKeyUp(keyCode, event)   // 其它未映射键(滑动尾随键等)放行
        }
    }

    /** 收到权限请求 → 进选择模式 + 启动 60s 倒计时(超时=拒绝)。 */
    private fun enterChoice(msg: ServerMessage.PermissionRequest) {
        choiceState = ChoiceState(msg.options)
        choiceTimeoutDefault = "拒绝"
        hud.choice = PermissionPrompt(msg.id, msg.summary, msg.options, msg.allowKey, 0, 60)
        startCountdown()
    }

    /** 收到模型选择请求 → 进选择模式,默认高亮当前模型,超时=取消。 */
    private fun enterModelChoice(msg: ServerMessage.ModelRequest) {
        val cs = ChoiceState(msg.options)
        repeat(msg.current.coerceIn(0, msg.options.size - 1)) { cs.move(1) }
        choiceState = cs
        choiceTimeoutDefault = "取消"
        hud.choice = PermissionPrompt(msg.id, "切换模型", msg.options, "", cs.highlight, 60, title = "选择模型")
        startCountdown()
    }

    private fun startCountdown() {
        secondsLeft = 60
        countdown.removeCallbacksAndMessages(null)
        val tick = object : Runnable {
            override fun run() {
                val cur = hud.choice ?: return
                secondsLeft -= 1
                if (secondsLeft <= 0) { submitChoice(choiceTimeoutDefault); return }
                hud.choice = cur.copy(secondsLeft = secondsLeft)
                countdown.postDelayed(this, 1000)
            }
        }
        countdown.postDelayed(tick, 1000)
        refreshKeepOn()
    }

    /** 回发裁决、退出选择模式。 */
    private fun submitChoice(choice: String) {
        val p = hud.choice ?: return
        countdown.removeCallbacksAndMessages(null)
        client.sendDecision(p.id, choice, p.allowKey)
        hud.choice = null
        choiceState = null
        refreshKeepOn()
    }

    /** 活跃(录音/运行/待权限)且未灭屏 → 屏常亮;否则交系统超时灭屏。 */
    private fun refreshKeepOn() {
        val on = (recording || running || hud.choice != null) && !hud.blanked
        runOnUiThread {
            if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private var wokeAt = 0L

    private fun setBlanked(b: Boolean) {
        hud.blanked = b
        if (!b) { wokeAt = SystemClock.uptimeMillis(); hud.scroll(1) }   // 退出灭屏 → 记唤醒时刻 + 滚到最新
        refreshKeepOn()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && hud.blanked) setBlanked(false)  // 面板被唤醒即恢复显示
    }

    /** 单击语境感知:录音中→停发;Claude 运行中→打断;空闲→开录。 */
    private fun onTap() {
        when {
            recording -> { voice.stop(); recording = false /* onAudio 里会再确认 */ }
            running -> { client.stop(); hud.status = "⏹ 已停止"; running = false }
            else -> {
                if (audioGranted.value) {
                    voice.start(); recording = true; hud.recording = true; hud.status = "🎤 录音中…"
                } else {
                    hud.status = "麦克风未授权"; requestAudio.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        refreshKeepOn()
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
        voice.destroy()
    }
}

/** 把中继消息映射成 HUD 状态(对照 web/app.js)。Transcript 在 MainActivity onMessage 处理。 */
fun handle(msg: ServerMessage, state: HudState) {
    when (msg) {
        is ServerMessage.Sync -> msg.currentRun?.let { state.add("── 续接:${it.prompt} ──", Color(0xFF00AA77)) }
        is ServerMessage.RunEnd -> state.status = when (msg.status) {
            "interrupted" -> "⚠️ 已中断"; "error" -> "❌ 出错"; else -> "✅ 完成"
        }
        is ServerMessage.Event -> renderEvent(msg.event, state)
        is ServerMessage.Transcript -> {}
        is ServerMessage.PermissionRequest -> {}  // 在 onMessage 上游处理(进选择模式)
        is ServerMessage.Usage -> {}
        is ServerMessage.ModelRequest -> {}
        ServerMessage.Unknown -> {}
    }
}

fun renderEvent(ev: AgentEvent, state: HudState) {
    when (ev) {
        is AgentEvent.System -> state.status = "🟢 已就绪 · 思考中…"
        AgentEvent.Thinking -> state.status = "💭 思考中…"
        is AgentEvent.Text -> state.addText(ev.delta)
        is AgentEvent.ToolUse -> {
            val label = "⏳ ${ev.name}${if (ev.summary.isNotEmpty()) " — ${ev.summary}" else ""}"
            state.add(label)
            state.toolIndex[ev.id] = state.lines.size - 1
            state.status = "⏳ ${ev.name}…"
        }
        is AgentEvent.ToolResult -> {
            val idx = state.toolIndex[ev.id] ?: return
            val old = state.lines[idx].text.removePrefix("⏳ ")
            state.lines[idx] = HudLine((if (ev.isError) "❌ " else "✅ ") + old)
        }
        AgentEvent.Done -> state.status = "✅ 完成"
        is AgentEvent.Error -> { state.add("错误: ${ev.message}", Color(0xFFFF5555)); state.status = "❌ 出错" }
        AgentEvent.Unknown -> {}
    }
}
