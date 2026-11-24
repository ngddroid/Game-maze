package com.ngstudio.mazebrush

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.ui.components.SurfaceBackground
import com.ngstudio.mazebrush.ui.components.EngravedText
import com.ngstudio.mazebrush.ui.components.GameText
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.RoundButton
import com.ngstudio.mazebrush.ui.components.color
import com.ngstudio.mazebrush.ui.theme.Candy
import com.ngstudio.mazebrush.ui.theme.MazeTheme

/**
 * Terms of Use / Privacy Policy. Shows game-themed HTML from assets/legal in a WebView
 * (JavaScript off, no network; links open in the browser).
 */
class LegalActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        val page = intent.getStringExtra(EXTRA_PAGE) ?: PAGE_TERMS
        val title = if (page == PAGE_PRIVACY) "Privacy Policy" else "Terms of Use"
        setContent {
            MazeTheme {
                SurfaceBackground(WorldTheme.ALL[0]) {
                    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RoundButton(Icon.BACK, size = 46.dp) { finish() }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { com.ngstudio.mazebrush.ui.components.EngravedText(title, 28.sp, WorldTheme.ALL[0].ink.color()) }
                            Box(Modifier.padding(26.dp))
                        }
                        AndroidView(
                            factory = { ctx -> legalWebView(ctx, "file:///android_asset/legal/$page.html") },
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(24.dp)).background(Candy.cream),
                        )
                    }
                }
            }
        }
    }

    private fun legalWebView(ctx: Context, url: String) = WebView(ctx).apply {
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.blockNetworkLoads = true
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.scheme == "file") {
                    // A link to the other legal page opens it as its own screen (right header; Back returns here).
                    val other = when (u.lastPathSegment) {
                        "$PAGE_TERMS.html" -> PAGE_TERMS
                        "$PAGE_PRIVACY.html" -> PAGE_PRIVACY
                        else -> null
                    }
                    if (other != null && other != (intent.getStringExtra(EXTRA_PAGE) ?: PAGE_TERMS)) {
                        open(this@LegalActivity, other)
                        return true
                    }
                    return false
                }
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                return true
            }
        }
        loadUrl(url)
    }

    companion object {
        const val EXTRA_PAGE = "page"
        const val PAGE_TERMS = "terms"
        const val PAGE_PRIVACY = "privacy"

        fun open(ctx: Context, page: String) = ctx.startActivity(Intent(ctx, LegalActivity::class.java).putExtra(EXTRA_PAGE, page))

        @Suppress("unused")
        private fun uri(s: String) = Uri.parse(s)
    }
}
