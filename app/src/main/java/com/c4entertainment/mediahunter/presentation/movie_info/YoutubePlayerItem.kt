package com.c4entertainment.mediahunter.presentation.movie_info

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.FORCE_DARK_ON
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.c4entertainment.mediahunter.MainActivity
import com.c4entertainment.mediahunter.util.Constants.VIDEO_ID
import com.c4entertainment.mediahunter.util.Constants.VIDEO_START_DELAY
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private const val signInUrl =
    "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Dm%26hl%3Dtr%26next%3Dhttps%253A%252F%252Fm.youtube.com%252F"

@Composable
fun YoutubePlayer(
    youtubeVideoID: String,
) {

    val context = LocalContext.current
    var videoStartDelay = 0F

    lateinit var ytPlayer: YouTubePlayer

    val mainActivity = context as MainActivity

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data

                videoStartDelay = intent?.getFloatExtra(VIDEO_START_DELAY, 0F) ?: 0F
                ytPlayer.seekTo(videoStartDelay)
                ytPlayer.play()
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {

        var isLoggedIn by remember { mutableStateOf(false) } // Track login status

        if (true) {
            AndroidView(
                factory = {

                    YouTubePlayerView(it).apply {
                        val youTubePlayerView = this

                        val iFramePlayerOptions = IFramePlayerOptions.Builder(context)
                            .controls(1)
                            .fullscreen(1) // enable full screen button
                            .build()

                        // we need to initialize manually in order to pass IFramePlayerOptions to the player
                        youTubePlayerView.enableAutomaticInitialization = false

                        youTubePlayerView.addFullscreenListener(object : FullscreenListener {
                            override fun onEnterFullscreen(
                                fullscreenView: View,
                                exitFullscreen: () -> Unit
                            ) {

                                //navigate to full screen activity
                                val intent = Intent(mainActivity, FullscreenActivity::class.java)
                                intent.putExtra(VIDEO_ID, youtubeVideoID)
                                intent.putExtra(VIDEO_START_DELAY, videoStartDelay)
                                exitFullscreen()

                                launcher.launch(intent)
                            }

                            override fun onExitFullscreen() {

                            }
                        })

                        youTubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                ytPlayer = youTubePlayer
                                youTubePlayer.loadVideo(youtubeVideoID, videoStartDelay)
                            }

                            override fun onCurrentSecond(
                                youTubePlayer: YouTubePlayer,
                                second: Float
                            ) {
                                videoStartDelay = second
                            }
                        }, iFramePlayerOptions)


                        findViewTreeLifecycleOwner()?.lifecycle?.addObserver(youTubePlayerView)
                    }


                }
            )
        } else {
            AndroidView(
                factory = { context ->
                    val container = FrameLayout(context)

                    // WebView setup
                    val webView = WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            settings.isAlgorithmicDarkeningAllowed = true
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                settings.forceDark = FORCE_DARK_ON
                            }
                        }

                        loadUrl(signInUrl)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                if (request.url.toString().startsWith("https://m.youtube.com") ||
                                    request.url.toString().startsWith("https://www.youtube.com")
                                ) {
                                    Log.d("WebView", "Logged in")

                                    // Notify login success
                                    mainActivity.runOnUiThread {
                                        isLoggedIn = true
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                    }

                    container.addView(webView)

                    // Initially add the WebView only
                    container
                },
                modifier = Modifier.fillMaxSize()
            )
        }

    }
}