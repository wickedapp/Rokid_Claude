package com.rokid.relayhud

/** 新会话 / 退出 的语音口令集(含 whisper 常见同音变体),按语言匹配。 */
private val NEW_SESSION_ZH = setOf(
    "新会话", "新对话", "开新对话", "新的对话", "开始新对话",
    "新绘画", "新绘话", "新汇话", "新对画", "心会话",
)
private val NEW_SESSION_EN = setOf("new session", "new chat", "new conversation", "start over", "reset")
private val EXIT_ZH = setOf("退出", "关闭", "退下", "推出", "退出程序", "关掉")
private val EXIT_EN = setOf("exit", "quit", "close", "exit app", "quit app")

private fun norm(t: String) = t.trim().trim('。', '，', '!', '！', '.', ' ').lowercase()

fun matchesNewSession(text: String, lang: String): Boolean =
    (if (lang == "en") NEW_SESSION_EN else NEW_SESSION_ZH).contains(norm(text))

fun matchesExit(text: String, lang: String): Boolean =
    (if (lang == "en") EXIT_EN else EXIT_ZH).contains(norm(text))
