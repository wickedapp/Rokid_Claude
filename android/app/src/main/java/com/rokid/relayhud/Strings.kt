package com.rokid.relayhud

/** HUD UI 文案。英文模式下全英文,无中文残留。由 config.lang 驱动。 */
data class Strings(
    val connecting: String,
    val connected: String,
    val disconnected: String,
    val closed: String,
    val ready: String,
    val recordingDot: String,
    val recordingStatus: String,
    val hint: String,
    val choiceHint: String,
    val modelUnknown: String,
    val sessionLabel: String,
    val confirm: String,
    val selectModel: String,
    val noSpeech: String,
    val newSessionMsg: String,
    val exitMsg: String,
    val submitting: String,
    val transcribing: String,
    val stopped: String,
    val micUnauthorized: String,
    val resumePrefix: String,
    val interrupted: String,
    val errored: String,
    val done: String,
    val readyThinking: String,
    val thinking: String,
    val errorPrefix: String,
)

private val ZH = Strings(
    connecting = "连接中…", connected = "已连接", disconnected = "断开,重连中…", closed = "已关闭",
    ready = "就绪", recordingDot = "● 录音中…", recordingStatus = "🎤 录音中…",
    hint = "单击说话/停止 · 双击灭屏 · 滑动翻页 · 说\"新会话\"重开 · 说\"退出\"关闭", choiceHint = "前/后滑选 · 单击定",
    modelUnknown = "模型未知", sessionLabel = "会话", confirm = "需要确认", selectModel = "选择模型",
    noSpeech = "(没听到内容)", newSessionMsg = "🆕 新会话", exitMsg = "👋 退出",
    submitting = "提交中…", transcribing = "转写中…", stopped = "⏹ 已停止", micUnauthorized = "麦克风未授权",
    resumePrefix = "续接", interrupted = "⚠️ 已中断", errored = "❌ 出错", done = "✅ 完成",
    readyThinking = "🟢 已就绪 · 思考中…", thinking = "💭 思考中…", errorPrefix = "错误: ",
)

private val EN = Strings(
    connecting = "Connecting…", connected = "Connected", disconnected = "Disconnected, reconnecting…", closed = "Closed",
    ready = "Ready", recordingDot = "● Recording…", recordingStatus = "🎤 Recording…",
    hint = "tap talk/stop · double-tap blank · swipe scroll · say \"new session\" to reset · say \"exit\" to quit", choiceHint = "swipe to choose · tap to confirm",
    modelUnknown = "no model", sessionLabel = "session", confirm = "Confirm", selectModel = "Select model",
    noSpeech = "(nothing heard)", newSessionMsg = "🆕 New session", exitMsg = "👋 Exit",
    submitting = "Submitting…", transcribing = "Transcribing…", stopped = "⏹ Stopped", micUnauthorized = "Mic not authorized",
    resumePrefix = "resuming", interrupted = "⚠️ Interrupted", errored = "❌ Error", done = "✅ Done",
    readyThinking = "🟢 Ready · thinking…", thinking = "💭 Thinking…", errorPrefix = "Error: ",
)

fun strings(lang: String): Strings = if (lang == "en") EN else ZH
