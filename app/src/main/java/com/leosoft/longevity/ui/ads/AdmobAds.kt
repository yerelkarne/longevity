package com.leosoft.longevity.ui.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

private const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

object AdUnitIds {
    // TODO: Replace test IDs with production IDs before release.
    const val nativeAdvanced: String = TEST_NATIVE_AD_UNIT_ID
    const val interstitial: String = TEST_INTERSTITIAL_AD_UNIT_ID
}

object AdMobManager {
    private var initialized = false
    private var interstitialAd: InterstitialAd? = null
    private var pageChangeCount = 0

    fun initialize(context: Context) {
        if (initialized) return
        MobileAds.initialize(context) {}
        initialized = true
        preloadInterstitial(context)
    }

    fun preloadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            AdUnitIds.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun onPageChanged(activity: Activity) {
        pageChangeCount += 1
        if (pageChangeCount % 10 != 0) return

        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                preloadInterstitial(activity)
            }
        }
        ad.show(activity)
    }
}

@Composable
fun NativeAdvancedAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdUnitIds.nativeAdvanced
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { loadedAd ->
                nativeAd?.destroy()
                nativeAd = loadedAd
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val containerColor = colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val headlineColor = colorScheme.onSurface.toArgb()
    val bodyColor = colorScheme.onSurfaceVariant.toArgb()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = containerColor, shape = RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                NativeAdView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            },
            update = { adView ->
                val ad = nativeAd ?: return@AndroidView
                adView.removeAllViews()

                val container = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(8, 8, 8, 8)
                }

                val headlineView = android.widget.TextView(context).apply {
                    setTextColor(headlineColor)
                    textSize = 17f
                    text = ad.headline
                }
                adView.headlineView = headlineView
                container.addView(headlineView)

                ad.body?.let { body ->
                    val bodyView = android.widget.TextView(context).apply {
                        setTextColor(bodyColor)
                        textSize = 14f
                        text = body
                    }
                    adView.bodyView = bodyView
                    container.addView(bodyView)
                }

                val mediaView = MediaView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        420
                    )
                }
                adView.mediaView = mediaView
                container.addView(mediaView)

                ad.callToAction?.let { cta ->
                    val ctaView = android.widget.Button(context).apply {
                        text = cta
                    }
                    adView.callToActionView = ctaView
                    container.addView(ctaView)
                }

                adView.addView(container)
                adView.setNativeAd(ad)
            }
        )
    }
}
