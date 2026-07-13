package com.rokid.relayhud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class StringsTest {
    @Test fun zhAndEnDiffer() {
        assertEquals("就绪", strings("zh").ready)
        assertEquals("Ready", strings("en").ready)
        assertEquals("Confirmation required", strings("en").confirm)
        assertEquals("Select model", strings("en").selectModel)
    }
    @Test fun unknownLangFallsBackEnglish() {
        assertEquals("Ready", strings("fr").ready)
    }
    @Test fun newSessionMatch() {
        assertTrue(matchesNewSession("new session", "en"))
        assertTrue(matchesNewSession("新会话", "zh"))
        assertTrue(!matchesNewSession("hello", "en"))
    }
    @Test fun exitMatch() {
        assertTrue(matchesExit("quit", "en"))
        assertTrue(matchesExit("退出", "zh"))
        assertTrue(!matchesExit("continue", "en"))
    }
    @Test fun wifiMatch() {
        assertTrue(matchesWifi("网络", "zh"))
        assertTrue(matchesWifi("wifi", "en"))
        assertTrue(!matchesWifi("hello", "en"))
    }
    @Test fun offlineHint() {
        assertEquals("Enter: scan network · Back: exit", strings("en").offlineHint)
        assertEquals("Enter：扫描网络 · Back：退出", strings("zh").offlineHint)
    }
    @Test fun onlySimplifiedChineseSystemLocalesUseChinese() {
        assertEquals("就绪", stringsForLocale(Locale.SIMPLIFIED_CHINESE).ready)
        assertEquals("就绪", stringsForLocale(Locale.forLanguageTag("zh-Hans-SG")).ready)
        assertEquals("Ready", stringsForLocale(Locale.TRADITIONAL_CHINESE).ready)
        assertEquals("Ready", stringsForLocale(Locale.JAPANESE).ready)
    }
    @Test fun langSwitchMatchesBothLanguages() {
        assertTrue(matchesLangSwitch("切换语言"))
        assertTrue(matchesLangSwitch("切语言"))
        assertTrue(matchesLangSwitch("中英切换"))
        assertTrue(matchesLangSwitch("switch language"))
        assertTrue(matchesLangSwitch("change language"))
        assertTrue(matchesLangSwitch("toggle language"))
        assertTrue(matchesLangSwitch("Switch Language."))
    }
    @Test fun langSwitchDoesNotMatchTaskSentences() {
        assertTrue(!matchesLangSwitch("把这段代码的语言换成 Python"))
        assertTrue(!matchesLangSwitch("switch the language of this file to rust"))
        assertTrue(!matchesLangSwitch("hello"))
    }
    @Test fun scannerStrings() {
        assertEquals("请对准 Wi-Fi 二维码", strings("zh").scanHint)
        assertEquals("Aim at a Wi-Fi QR code", strings("en").scanHint)
        assertEquals("网络已保存", strings("zh").wifiSaved)
        assertEquals("网络未保存", strings("zh").wifiNotSaved)
        assertEquals("Camera permission denied", strings("en").cameraDenied)
    }
    @Test fun configStrings() {
        assertEquals("连接到", strings("zh").connectTo)
        assertEquals("Connect to", strings("en").connectTo)
        assertEquals("Enter 确认 · Back 取消", strings("zh").confirmHint)
        assertEquals("配置已应用，正在重连", strings("zh").configApplied)
        assertEquals("无法识别此二维码", strings("zh").unknownQr)
        assertEquals("Unrecognized QR code", strings("en").unknownQr)
    }
    @Test fun terminalFooterIsOnlyReplyAction() {
        assertEquals("Enter 回复", strings("zh").enterToReply)
        assertEquals("Enter To Reply", strings("en").enterToReply)
    }
}
