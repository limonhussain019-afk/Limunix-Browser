package com.example.video

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.LimunixCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun LimunixNativeVideoPlayer(
    videoUrl: String,
    videoTitle: String,
    seekDurationSeconds: Int = 10,
    onClose: () -> Unit,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLongPressFastForward by remember { mutableStateOf(false) }
    var currentAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }

    // HUD gesture indicators
    var hudIcon by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var hudText by remember { mutableStateOf("") }
    var hudProgress by remember { mutableFloatStateOf(0f) }
    var showHud by remember { mutableStateOf(false) }

    // Brightness state (0f to 1f)
    var currentBrightness by remember {
        val window = activity?.window
        val lp = window?.attributes
        val b = lp?.screenBrightness ?: -1f
        mutableFloatStateOf(if (b < 0) 0.5f else b)
    }

    // Volume state (0f to 1f)
    var currentVolumeProgress by remember {
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf(vol.toFloat() / maxVolume.toFloat().coerceAtLeast(1f))
    }

    // ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            // Reset screen orientation & brightness if altered
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    // Ticking position updater
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(showControls, isLocked) {
        if (showControls && !isLocked) {
            delay(4000)
            showControls = false
        }
    }

    BackHandler {
        if (isLocked) {
            isLocked = false
        } else {
            onClose()
        }
    }

    fun displayHud(icon: @Composable () -> Unit, text: String, progress: Float) {
        hudIcon = icon
        hudText = text
        hudProgress = progress
        showHud = true
        scope.launch {
            delay(1200)
            showHud = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("limunix_native_video_player")
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        // Media3 PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = when (currentAspectRatio) {
                        VideoAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoAspectRatio.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        VideoAspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        VideoAspectRatio.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        VideoAspectRatio.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    }
                }
            },
            update = { playerView ->
                playerView.resizeMode = when (currentAspectRatio) {
                    VideoAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoAspectRatio.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoAspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    VideoAspectRatio.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    VideoAspectRatio.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Touch & Gesture Detection Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Tap & Double Tap gestures
                .pointerInput(isLocked) {
                    if (isLocked) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                    } else {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = { offset ->
                                val x = offset.x
                                val widthPx = size.width
                                if (x < widthPx * 0.4f) {
                                    // Rewind
                                    val newPos = (exoPlayer.currentPosition - seekDurationSeconds * 1000).coerceAtLeast(0L)
                                    exoPlayer.seekTo(newPos)
                                    currentPosition = newPos
                                    displayHud(
                                        icon = { Icon(Icons.Default.Replay10, null, tint = Color.White) },
                                        text = "-${seekDurationSeconds}s",
                                        progress = newPos.toFloat() / totalDuration.coerceAtLeast(1L).toFloat()
                                    )
                                } else if (x > widthPx * 0.6f) {
                                    // Forward
                                    val newPos = (exoPlayer.currentPosition + seekDurationSeconds * 1000).coerceAtMost(totalDuration)
                                    exoPlayer.seekTo(newPos)
                                    currentPosition = newPos
                                    displayHud(
                                        icon = { Icon(Icons.Default.Forward10, null, tint = Color.White) },
                                        text = "+${seekDurationSeconds}s",
                                        progress = newPos.toFloat() / totalDuration.coerceAtLeast(1L).toFloat()
                                    )
                                } else {
                                    // Center double tap toggles play/pause
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            },
                            onLongPress = {
                                // 2x Speed Boost while holding
                                isLongPressFastForward = true
                                exoPlayer.setPlaybackSpeed(2.0f)
                                displayHud(
                                    icon = { Icon(Icons.Default.FastForward, null, tint = LimunixCyan) },
                                    text = "2.0x Boost",
                                    progress = 1.0f
                                )
                            }
                        )
                    }
                }
                // Vertical drag gesture (Left side = Brightness, Right side = Volume)
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (isLongPressFastForward) {
                                    isLongPressFastForward = false
                                    exoPlayer.setPlaybackSpeed(playbackSpeed)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                val x = change.position.x
                                val widthPx = size.width
                                val delta = -dragAmount / size.height // Drag up is negative y

                                if (x < widthPx * 0.5f) {
                                    // Left side: Brightness
                                    val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                                    currentBrightness = newBrightness
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        lp.screenBrightness = newBrightness
                                        act.window.attributes = lp
                                    }
                                    displayHud(
                                        icon = { Icon(Icons.Default.BrightnessMedium, null, tint = LimunixCyan) },
                                        text = "${(newBrightness * 100).roundToInt()}%",
                                        progress = newBrightness
                                    )
                                } else {
                                    // Right side: Volume
                                    val newVol = (currentVolumeProgress + delta).coerceIn(0.0f, 1.0f)
                                    currentVolumeProgress = newVol
                                    val targetVol = (newVol * maxVolume).roundToInt().coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    displayHud(
                                        icon = { Icon(Icons.Default.VolumeUp, null, tint = LimunixCyan) },
                                        text = "${(newVol * 100).roundToInt()}%",
                                        progress = newVol
                                    )
                                }
                            }
                        )
                    }
                }
                // Horizontal drag gesture (Center swipe scrub seek)
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val seekStepMs = (dragAmount * 150).toLong()
                            val newPos = (exoPlayer.currentPosition + seekStepMs).coerceIn(0L, totalDuration)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                            displayHud(
                                icon = {
                                    Icon(
                                        if (seekStepMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                                        null,
                                        tint = LimunixCyan
                                    )
                                },
                                text = "${formatDuration(newPos)} / ${formatDuration(totalDuration)}",
                                progress = newPos.toFloat() / totalDuration.coerceAtLeast(1L).toFloat()
                            )
                        }
                    }
                }
        )

        // Buffering Indicator
        if (isBuffering) {
            CircularProgressIndicator(
                color = LimunixCyan,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            )
        }

        // On-Screen HUD Overlay for Brightness / Volume / Seek
        AnimatedVisibility(
            visible = showHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LimunixCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    hudIcon()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = hudText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { hudProgress },
                        color = LimunixCyan,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // Main Player Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("video_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = videoTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Aspect Ratio Button
                    Box {
                        IconButton(
                            onClick = { showAspectMenu = true },
                            modifier = Modifier.testTag("video_aspect_button")
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showAspectMenu,
                            onDismissRequest = { showAspectMenu = false }
                        ) {
                            VideoAspectRatio.values().forEach { ratio ->
                                DropdownMenuItem(
                                    text = { Text(ratio.label) },
                                    onClick = {
                                        currentAspectRatio = ratio
                                        showAspectMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // PiP Button
                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier.testTag("video_pip_button")
                    ) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Picture in Picture", tint = Color.White)
                    }

                    // Lock Controls Button
                    IconButton(
                        onClick = { isLocked = !isLocked },
                        modifier = Modifier.testTag("video_lock_button")
                    ) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = if (isLocked) LimunixCyan else Color.White
                        )
                    }
                }

                // Center Play/Rewind/Forward Controls (hidden if locked)
                if (!isLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - seekDurationSeconds * 1000).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .testTag("video_rewind_button")
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "Rewind", tint = Color.White)
                        }

                        // Play/Pause
                        IconButton(
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(LimunixCyan)
                                .testTag("video_play_pause_button")
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + seekDurationSeconds * 1000).coerceAtMost(totalDuration)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .testTag("video_forward_button")
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward", tint = Color.White)
                        }
                    }
                }

                // Bottom Bar Controls (hidden if locked)
                if (!isLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Slider seek bar
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { newPos ->
                                currentPosition = newPos.toLong()
                                exoPlayer.seekTo(newPos.toLong())
                            },
                            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = LimunixCyan,
                                activeTrackColor = LimunixCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("video_seek_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatDuration(currentPosition)} / ${formatDuration(totalDuration)}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Speed Selector
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { showSpeedMenu = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Speed, null, tint = LimunixCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x") },
                                            onClick = {
                                                playbackSpeed = speed
                                                exoPlayer.setPlaybackSpeed(speed)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
