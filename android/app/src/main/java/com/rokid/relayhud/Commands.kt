package com.rokid.relayhud

/** 新会话 / 退出 的语音口令集(含 whisper 常见同音变体),按语言匹配。 */
private val NEW_SESSION_ZH = setOf(
    "新会话", "新对话", "开新对话", "新的对话", "开始新对话",
    "新绘画", "新绘话", "新汇话", "新对画", "心会话",
)
private val NEW_SESSION_EN = setOf("new session", "new chat", "new conversation", "start over", "reset")
private val EXIT_ZH = setOf("退出", "关闭", "退下", "推出", "退出程序", "关掉")
private val EXIT_EN = setOf("exit", "quit", "close", "exit app", "quit app")
// 打开系统 WiFi 面板的口令(随时可用的 WiFi 入口,含 whisper 常见变体)
private val WIFI_ZH = setOf("网络", "无线网", "wifi", "wi-fi", "网路", "连网", "wifi设置", "打开wifi", "无线网络")
private val WIFI_EN = setOf("wifi", "wi-fi", "wifi settings", "network", "open wifi", "wi fi")

private fun norm(t: String) = t.trim().trim('。', '，', '!', '！', '.', ' ').lowercase()

fun matchesNewSession(text: String, lang: String): Boolean =
    (if (lang == "en") NEW_SESSION_EN else NEW_SESSION_ZH).contains(norm(text))

fun matchesExit(text: String, lang: String): Boolean =
    (if (lang == "en") EXIT_EN else EXIT_ZH).contains(norm(text))

fun matchesWifi(text: String, lang: String): Boolean =
    (if (lang == "en") WIFI_EN else WIFI_ZH).contains(norm(text))
