package com.c4entertainment.mediahunter.presentation.movie_info

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.c4entertainment.mediahunter.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** The inline player with the video's name and a close button under it. */
@Composable
fun VideoPlayerHeader(
    videoId: String,
    name: String,
    startSecond: () -> Float,
    autoplay: () -> Boolean,
    onStarted: () -> Unit,
    onSecond: (Float) -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        InlineVideoPlayer(
            videoId = videoId,
            startSecond = startSecond,
            autoplay = autoplay,
            onStarted = onStarted,
            onSecond = onSecond,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.video_close))
            }
        }
    }
}

/** A small "Playing" tag over the card that is on screen above. */
@Composable
fun PlayingBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(end = 2.dp),
        )
        Text(
            stringResource(R.string.video_playing),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun InlineVideoPlayer(
    videoId: String,
    startSecond: () -> Float,
    autoplay: () -> Boolean,
    onStarted: () -> Unit,
    onSecond: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var exitFullscreen by remember { mutableStateOf<(() -> Unit)?>(null) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    val latestSecond by rememberUpdatedState(onSecond)

    fun leaveFullscreen() {
        val view = fullscreenView ?: return
        fullscreenView = null
        (view.parent as? ViewGroup)?.removeView(view)
        activity?.let {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowInsetsControllerCompat(it.window, it.window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Start or switch the video once the player is ready and whenever the id changes.
    LaunchedEffect(player, videoId) {
        val ready = player ?: return@LaunchedEffect
        if (autoplay()) {
            ready.loadOrCueVideo(lifecycle, videoId, startSecond())
            onStarted()
        } else {
            ready.cueVideo(videoId, startSecond())
        }
    }

    BackHandler(enabled = exitFullscreen != null) { exitFullscreen?.invoke() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                enableAutomaticInitialization = false
                lifecycle.addObserver(this)
                val inline = this
                addFullscreenListener(object : FullscreenListener {
                    override fun onEnterFullscreen(view: View, exit: () -> Unit) {
                        val act = activity ?: return
                        exitFullscreen = exit
                        fullscreenView = view
                        inline.visibility = View.INVISIBLE
                        (view.parent as? ViewGroup)?.removeView(view)
                        val content = act.findViewById<ViewGroup>(android.R.id.content)
                        content.addView(
                            view,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                        view.setBackgroundColor(AndroidColor.BLACK)
                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        WindowInsetsControllerCompat(act.window, act.window.decorView).apply {
                            hide(WindowInsetsCompat.Type.systemBars())
                            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    }

                    override fun onExitFullscreen() {
                        exitFullscreen = null
                        inline.visibility = View.VISIBLE
                        leaveFullscreen()
                    }
                })
                val options = IFramePlayerOptions.Builder(ctx)
                    .controls(1)
                    .fullscreen(1)
                    .rel(0)
                    .ivLoadPolicy(3)
                    .build()
                initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player = youTubePlayer
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        latestSecond(second)
                    }
                }, options)
            }
        },
        onRelease = { view ->
            leaveFullscreen()
            lifecycle.removeObserver(view)
            view.release()
        },
    )
}
