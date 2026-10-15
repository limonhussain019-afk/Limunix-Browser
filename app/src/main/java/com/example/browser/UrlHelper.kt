package com.example.browser

import android.net.Uri
import android.util.Patterns
import com.example.repository.SearchEngine
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UrlHelper {

    fun resolveInput(input: String, searchEngine: SearchEngine, customSearchUrl: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"

        // Explicit schemes
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("javascript:", ignoreCase = true)
        ) {
            return trimmed
        }

        // Check if looks like a domain / web URL without protocol
        val hasWhitespace = trimmed.contains(" ")
        val looksLikeDomain = !hasWhitespace && (
            Patterns.WEB_URL.matcher(trimmed).matches() ||
            (trimmed.contains(".") && !trimmed.endsWith(".") && trimmed.length >= 3)
        )

        if (looksLikeDomain) {
            return "https://$trimmed"
        }

        // Otherwise it's a search query
        val encodedQuery = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString())
        return when (searchEngine) {
            SearchEngine.GOOGLE -> "https://www.google.com/search?q=$encodedQuery"
            SearchEngine.DUCKDUCKGO -> "https://duckduckgo.com/?q=$encodedQuery"
            SearchEngine.BING -> "https://www.bing.com/search?q=$encodedQuery"
            SearchEngine.YAHOO -> "https://search.yahoo.com/search?p=$encodedQuery"
            SearchEngine.CUSTOM -> {
                if (customSearchUrl.isNotBlank()) {
                    if (customSearchUrl.contains("%s")) {
                        customSearchUrl.replace("%s", encodedQuery)
                    } else {
                        "$customSearchUrl$encodedQuery"
                    }
                } else {
                    "https://duckduckgo.com/?q=$encodedQuery"
                }
            }
        }
    }

    fun extractDisplayDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                if (host.startsWith("www.")) host.substring(4) else host
            } else {
                url
            }
        } catch (e: Exception) {
            url
        }
    }

    fun isHttps(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }

    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    const val MOBILE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 LimunixBrowser/1.0"
}
