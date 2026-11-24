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
 * Terms of Use / Privacy Policy. Loads the live page ([LIVE_PRIVACY] / [LIVE_TERMS]) in a WebView first; if it
 * fails (offline, HTTP error, or no answer within [LIVE_TIMEOUT_MS]) it shows the bundled copy from
 * assets/legal instead. JavaScript is off; links to the other legal page open it as its own screen, and
 * other links (web, email, phone) open in their own apps.
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
                SurfaceBackground {
                    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RoundButton(Icon.BACK, size = 46.dp) { finish() }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { com.ngstudio.mazebrush.ui.components.EngravedText(title, 28.sp, com.ngstudio.mazebrush.ui.components.LocalBackdrop.current.ink.color()) }
                            Box(Modifier.padding(26.dp))
                        }
                        AndroidView(
                            factory = { ctx -> legalWebView(ctx, page) },
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(24.dp)).background(Candy.cream),
                        )
                    }
                }
            }
        }
    }

    private fun legalWebView(ctx: Context, page: String) = WebView(ctx).apply {
        val live = if (page == PAGE_PRIVACY) LIVE_PRIVACY else LIVE_TERMS
        val offline = "file:///android_asset/legal/$page.html"
        var settled = false   // the live page finished loading, or we switched to the bundled copy
        fun fallback(view: WebView) {
            if (settled) return
            settled = true
            view.stopLoading()
            view.settings.blockNetworkLoads = true
            view.loadUrl(offline)
        }
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.blockNetworkLoads = false
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                val ours = u.scheme == "file" || u.host == LIVE_HOST
                if (ours) {
                    // A link to the other legal page opens it as its own screen (right header; Back returns here).
                    val seg = u.lastPathSegment.orEmpty()
                    val other = when {
                        seg.contains("privacy", ignoreCase = true) -> PAGE_PRIVACY
                        seg.contains("terms", ignoreCase = true) -> PAGE_TERMS
                        else -> null
                    }
                    if (other != null) {
                        if (other != page) open(this@LegalActivity, other)
                        return true
                    }
                    return u.scheme != "file" && u.host != LIVE_HOST
                }
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                return true
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame && request.url.toString() == live) fallback(view)
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: android.webkit.WebResourceResponse) {
                if (request.isForMainFrame && request.url.toString() == live && errorResponse.statusCode >= 400) fallback(view)
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (url == live) settled = true
            }
        }
        // no answer in time (very slow or captive network): show the bundled copy
        postDelayed({ if (!settled) fallback(this) }, LIVE_TIMEOUT_MS)
        loadUrl(live)
    }

    companion object {
        const val EXTRA_PAGE = "page"
        const val PAGE_TERMS = "terms"
        const val PAGE_PRIVACY = "privacy"

        private const val LIVE_HOST = "game-mazegopuzzle.web.app"
        const val LIVE_PRIVACY = "https://$LIVE_HOST/privacy_policy.html"
        const val LIVE_TERMS = "https://$LIVE_HOST/terms_of_condition.html"
        private const val LIVE_TIMEOUT_MS = 8000L

        fun open(ctx: Context, page: String) = ctx.startActivity(Intent(ctx, LegalActivity::class.java).putExtra(EXTRA_PAGE, page))

        @Suppress("unused")
        private fun uri(s: String) = Uri.parse(s)
    }
}
