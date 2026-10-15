package com.example.browser

data class ReaderArticle(
    val title: String,
    val byline: String = "",
    val siteName: String = "",
    val contentHtml: String,
    val wordCount: Int,
    val estimatedMinutes: Int
)

enum class ReaderTheme(val bgHex: Long, val textHex: Long, val label: String) {
    LIGHT(0xFFFBFBFB, 0xFF1A1A1A, "Light"),
    SEPIA(0xFFF4ECD8, 0xFF5B4636, "Sepia"),
    DARK(0xFF18191A, 0xFFE4E6EB, "Dark"),
    OLED(0xFF000000, 0xFFCCCCCC, "OLED Black")
}

object ReaderModeExtractor {

    // JavaScript injected to extract article content
    const val JS_EXTRACT_ARTICLE = """
        (function() {
            try {
                var title = document.title || "";
                var h1 = document.querySelector('h1');
                if (h1 && h1.innerText) title = h1.innerText;

                var articleNode = document.querySelector('article') ||
                                  document.querySelector('main') ||
                                  document.querySelector('[role="main"]') ||
                                  document.querySelector('.post-content') ||
                                  document.querySelector('.entry-content') ||
                                  document.querySelector('.article-body');

                if (!articleNode) {
                    var paragraphs = Array.from(document.querySelectorAll('p'))
                        .filter(p => p.innerText.trim().length > 60);
                    if (paragraphs.length >= 3) {
                        var bodyText = paragraphs.map(p => '<p>' + p.innerHTML + '</p>').join('');
                        window.LimunixBridge.onReaderContent(title, bodyText, paragraphs.length);
                        return;
                    }
                    window.LimunixBridge.onReaderContent("", "", 0);
                    return;
                }

                // Clone node to clean up
                var clone = articleNode.cloneNode(true);
                var removeTags = ['script', 'style', 'iframe', 'form', 'nav', 'header', 'footer', 'aside', '.ad', '.ads', '.advertisement'];
                removeTags.forEach(function(selector) {
                    clone.querySelectorAll(selector).forEach(function(el) { el.remove(); });
                });

                var content = clone.innerHTML;
                var wordCount = (clone.innerText || "").split(/\s+/).length;
                window.LimunixBridge.onReaderContent(title, content, wordCount);
            } catch(e) {
                window.LimunixBridge.onReaderContent("", "", 0);
            }
        })();
    """

    // JavaScript to detect HTML5 video sources
    const val JS_DETECT_VIDEO = """
        (function() {
            try {
                var videos = document.querySelectorAll('video');
                for (var i = 0; i < videos.length; i++) {
                    var v = videos[i];
                    var src = v.currentSrc || v.src;
                    if (!src) {
                        var sourceTag = v.querySelector('source');
                        if (sourceTag) src = sourceTag.src;
                    }
                    if (src && src.indexOf('blob:') !== 0 && src.indexOf('data:') !== 0) {
                        window.LimunixBridge.onMediaDetected(src, document.title || "Web Video");
                        return;
                    }
                }
            } catch(e) {}
        })();
    """
}
