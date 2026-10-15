package com.example

import com.example.adblock.AdBlockManager
import com.example.adblock.BlockType
import com.example.browser.UrlHelper
import com.example.downloads.LimunixDownloadManager
import com.example.repository.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LimunixBrowserUnitTest {

    @Test
    fun testUrlResolution_withProtocol() {
        val input = "https://example.com"
        val resolved = UrlHelper.resolveInput(input, SearchEngine.DUCKDUCKGO, "")
        assertEquals("https://example.com", resolved)
    }

    @Test
    fun testUrlResolution_domainWithoutProtocol() {
        val input = "example.com"
        val resolved = UrlHelper.resolveInput(input, SearchEngine.DUCKDUCKGO, "")
        assertEquals("https://example.com", resolved)
    }

    @Test
    fun testUrlResolution_searchQuery() {
        val input = "best browser for android"
        val resolved = UrlHelper.resolveInput(input, SearchEngine.DUCKDUCKGO, "")
        assertTrue(resolved.contains("duckduckgo.com/?q="))
        assertTrue(resolved.contains("best+browser+for+android"))
    }

    @Test
    fun testUrlResolution_googleSearch() {
        val input = "kotlin tutorial"
        val resolved = UrlHelper.resolveInput(input, SearchEngine.GOOGLE, "")
        assertTrue(resolved.contains("google.com/search?q="))
        assertTrue(resolved.contains("kotlin+tutorial"))
    }

    @Test
    fun testExtractDisplayDomain() {
        assertEquals("example.com", UrlHelper.extractDisplayDomain("https://www.example.com/test?query=123"))
        assertEquals("github.com", UrlHelper.extractDisplayDomain("https://github.com/android"))
    }

    @Test
    fun testAdBlocker_blocksAds() {
        val result = AdBlockManager.checkUrl("https://adservice.google.com/ads/pagead.js")
        assertTrue(result.blocked)
        assertEquals(BlockType.AD, result.type)
    }

    @Test
    fun testAdBlocker_blocksTrackers() {
        val result = AdBlockManager.checkUrl("https://google-analytics.com/analytics.js")
        assertTrue(result.blocked)
        assertEquals(BlockType.TRACKER, result.type)
    }

    @Test
    fun testAdBlocker_allowsNormalSite() {
        val result = AdBlockManager.checkUrl("https://wikipedia.org/wiki/Android")
        assertFalse(result.blocked)
    }

    @Test
    fun testAdBlocker_whitelistBypassesBlock() {
        val whitelist = setOf("doubleclick.net")
        val result = AdBlockManager.checkUrl(
            url = "https://ad.doubleclick.net/ad.js",
            whitelist = whitelist
        )
        assertFalse(result.blocked)
    }

    @Test
    fun testAdBlocker_blacklistBlocksCustomSite() {
        val blacklist = setOf("annoyingsite.com")
        val result = AdBlockManager.checkUrl(
            url = "https://annoyingsite.com/page",
            blacklist = blacklist
        )
        assertTrue(result.blocked)
    }

    @Test
    fun testDownloadManager_fileNameSanitization() {
        val dirty = "my/unsafe?file*name:.pdf"
        val clean = LimunixDownloadManager.sanitizeFileName(dirty)
        assertEquals("my_unsafe_file_name_.pdf", clean)
    }

    @Test
    fun testDownloadManager_formatBytes() {
        assertEquals("0 B", LimunixDownloadManager.formatBytes(0))
        assertEquals("1 KB", LimunixDownloadManager.formatBytes(1024))
        assertEquals("1 MB", LimunixDownloadManager.formatBytes(1024 * 1024))
    }

    @Test
    fun testDownloadManager_mimeType() {
        assertEquals("video/mp4", LimunixDownloadManager.getMimeTypeFromFile(File("video.mp4")))
        assertEquals("application/pdf", LimunixDownloadManager.getMimeTypeFromFile(File("document.pdf")))
        assertEquals("application/vnd.android.package-archive", LimunixDownloadManager.getMimeTypeFromFile(File("app.apk")))
    }
}
