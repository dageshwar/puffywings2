package com.example.game.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowMetrics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AnchoredAdaptiveBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_AD_UNIT_ID
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .testTag("anchored_adaptive_banner"),
        contentAlignment = Alignment.BottomCenter
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdUnitId(adUnitId)
                    val adSize = getAdaptiveAdSize(ctx)
                    setAdSize(adSize)
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

private fun getAdaptiveAdSize(context: Context): AdSize {
    val activity = context as? Activity
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density

    val adWidthPixels: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity != null) {
        val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
        windowMetrics.bounds.width().toFloat()
    } else {
        displayMetrics.widthPixels.toFloat()
    }

    val adWidth = (adWidthPixels / density).toInt().coerceAtLeast(320)
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
}
