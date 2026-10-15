package com.example.video

enum class VideoAspectRatio(val label: String) {
    FIT("Fit"),
    CROP("Crop / Fill"),
    ZOOM("Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

data class DetectedMedia(
    val url: String,
    val title: String = "Web Video",
    val mimeType: String = "video/mp4",
    val isHls: Boolean = url.contains(".m3u8"),
    val detectedAt: Long = System.currentTimeMillis()
)
