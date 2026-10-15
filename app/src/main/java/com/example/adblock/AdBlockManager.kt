package com.example.adblock

import android.net.Uri
import java.util.Locale

enum class BlockType {
    AD,
    TRACKER,
    POPUP_MALWARE,
    NONE
}

data class BlockDecision(
    val blocked: Boolean,
    val type: BlockType = BlockType.NONE,
    val ruleMatched: String = ""
)

object AdBlockManager {

    // Curated high-efficiency domain list for ads
    private val AD_DOMAINS = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "adservice.google.com",
        "adnxs.com",
        "criteo.com",
        "outbrain.com",
        "taboola.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "smartadserver.com",
        "adroll.com",
        "adcolony.com",
        "applovin.com",
        "unityads.unity3d.com",
        "chartboost.com",
        "vungle.com",
        "inmobi.com",
        "ironsrc.com",
        "adsymptotic.com",
        "bidswitch.net",
        "adform.net",
        "sovrn.com",
        "undertone.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "exoclick.com",
        "trafficjunky.com",
        "zergnet.com",
        "admob.com",
        "adblade.com",
        "adsystem.com",
        "yieldmanager.com",
        "advertising.com"
    )

    // Curated list for trackers, analytics, telemetry, fingerprinting
    private val TRACKER_DOMAINS = hashSetOf(
        "google-analytics.com",
        "analytics.google.com",
        "googletagmanager.com",
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "kochava.com",
        "flurry.com",
        "newrelic.com",
        "fullstory.com",
        "mouseflow.com",
        "crazyegg.com",
        "optimizely.com",
        "clarity.ms",
        "statcounter.com",
        "omtrdc.net",
        "demdex.net",
        "bluekai.com",
        "moatads.com",
        "iasds01.com",
        "doubleverify.com"
    )

    // Strict list for invasive elements, popups, cryptominers
    private val STRICT_DOMAINS = hashSetOf(
        "coinhive.com",
        "cryptoloot.pro",
        "jsecoin.com",
        "adsupply.com",
        "mgid.com",
        "revcontent.com",
        "adsterra.com",
        "hilltopads.net",
        "clickadu.com",
        "adcash.com",
        "yllix.com"
    )

    // Common tracking paths/keywords in URL
    private val TRACKING_PATTERNS = listOf(
        "/pixel?",
        "/tr?",
        "/telemetry",
        "/beacon",
        "fbevents.js",
        "/gtm.js",
        "/analytics.js",
        "/pagead/",
        "/ads.js"
    )

    fun checkUrl(
        url: String,
        adBlockEnabled: Boolean = true,
        trackerBlockEnabled: Boolean = true,
        popupBlockEnabled: Boolean = true,
        isStrict: Boolean = false,
        whitelist: Set<String> = emptySet(),
        blacklist: Set<String> = emptySet()
    ): BlockDecision {
        if (url.isBlank()) return BlockDecision(false)

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return BlockDecision(false)
        }

        val host = uri.host?.lowercase(Locale.ROOT) ?: return BlockDecision(false)

        // Check if explicitly blacklisted by user
        if (isDomainMatched(host, blacklist)) {
            return BlockDecision(true, BlockType.AD, "User Blacklist: $host")
        }

        // Check if whitelisted by user
        if (isDomainMatched(host, whitelist)) {
            return BlockDecision(false)
        }

        // Check strict list
        if (isStrict && isDomainMatched(host, STRICT_DOMAINS)) {
            return BlockDecision(true, BlockType.POPUP_MALWARE, host)
        }

        // Check popup/malware
        if (popupBlockEnabled && (isDomainMatched(host, STRICT_DOMAINS) || host.contains("popup") || host.contains("popcash"))) {
            return BlockDecision(true, BlockType.POPUP_MALWARE, host)
        }

        // Check ad blocker
        if (adBlockEnabled && isDomainMatched(host, AD_DOMAINS)) {
            return BlockDecision(true, BlockType.AD, host)
        }

        // Check tracker blocker
        if (trackerBlockEnabled && isDomainMatched(host, TRACKER_DOMAINS)) {
            return BlockDecision(true, BlockType.TRACKER, host)
        }

        // Pattern matching for path/query
        val lowerUrl = url.lowercase(Locale.ROOT)
        if (adBlockEnabled || trackerBlockEnabled) {
            for (pattern in TRACKING_PATTERNS) {
                if (lowerUrl.contains(pattern)) {
                    val type = if (pattern.contains("ads") || pattern.contains("pagead")) BlockType.AD else BlockType.TRACKER
                    return BlockDecision(true, type, "Pattern: $pattern")
                }
            }
        }

        return BlockDecision(false)
    }

    private fun isDomainMatched(host: String, domainSet: Set<String>): Boolean {
        if (domainSet.isEmpty()) return false
        if (domainSet.contains(host)) return true

        // Check parent domains (e.g. ad.doubleclick.net matches doubleclick.net)
        val parts = host.split(".")
        if (parts.size > 2) {
            for (i in 1 until parts.size - 1) {
                val candidate = parts.subList(i, parts.size).joinToString(".")
                if (domainSet.contains(candidate)) return true
            }
        }
        return false
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host?.lowercase(Locale.ROOT) ?: url
        } catch (e: Exception) {
            url
        }
    }
}
