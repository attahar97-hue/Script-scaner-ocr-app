package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AdManager handles Google AdMob Test Ads initialization and ad presentation:
 * - Banner Ads
 * - Interstitial Ads (Full screen)
 * - App Open Ads (On app launch/resume)
 *
 * NOTE: All Ad Unit IDs are Google's official AdMob sample test IDs for safe testing.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Official Google AdMob Test Ad Unit IDs
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257390703"

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    MobileAds.initialize(context) { status ->
                        Log.d(TAG, "AdMob MobileAds initialized: $status")
                        isInitialized = true
                        loadInterstitialAd(context.applicationContext)
                        loadAppOpenAd(context.applicationContext)
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Non-fatal notice during MobileAds initialization: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error initiating MobileAds setup: ${e.message}")
        }
    }

    /**
     * Preload Interstitial Ad for smooth transitions
     */
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                TEST_INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoading = false
                        Log.d(TAG, "Interstitial test ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.w(TAG, "Interstitial test ad failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Throwable) {
            isInterstitialLoading = false
            Log.w(TAG, "Non-fatal error loading Interstitial ad: ${e.message}")
        }
    }

    /**
     * Show Interstitial Ad if available, then execute completion callback
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        try {
            val currentAd = interstitialAd
            if (currentAd != null) {
                currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad dismissed")
                        interstitialAd = null
                        loadInterstitialAd(activity.applicationContext)
                        onAdDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.w(TAG, "Interstitial ad failed to show: ${error.message}")
                        interstitialAd = null
                        loadInterstitialAd(activity.applicationContext)
                        onAdDismissed()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad showed full screen")
                    }
                }
                currentAd.show(activity)
            } else {
                Log.d(TAG, "Interstitial ad not ready, executing action directly")
                loadInterstitialAd(activity.applicationContext)
                onAdDismissed()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Exception showing Interstitial ad: ${e.message}")
            onAdDismissed()
        }
    }

    /**
     * Preload App Open Ad
     */
    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null || isAppOpenLoading) return
        isAppOpenLoading = true

        try {
            val adRequest = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                TEST_APP_OPEN_AD_UNIT_ID,
                adRequest,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isAppOpenLoading = false
                        Log.d(TAG, "App Open test ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        appOpenAd = null
                        isAppOpenLoading = false
                        Log.w(TAG, "App Open test ad failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Throwable) {
            isAppOpenLoading = false
            Log.w(TAG, "Non-fatal error loading App Open ad: ${e.message}")
        }
    }

    /**
     * Show App Open Ad on startup or resume
     */
    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        try {
            val currentAd = appOpenAd
            if (currentAd != null) {
                currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "App Open ad dismissed")
                        appOpenAd = null
                        loadAppOpenAd(activity.applicationContext)
                        onAdDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.w(TAG, "App Open ad failed to show: ${error.message}")
                        appOpenAd = null
                        loadAppOpenAd(activity.applicationContext)
                        onAdDismissed()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "App Open ad shown")
                    }
                }
                currentAd.show(activity)
            } else {
                loadAppOpenAd(activity.applicationContext)
                onAdDismissed()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Exception showing App Open ad: ${e.message}")
            onAdDismissed()
        }
    }
}

/**
 * Jetpack Compose AdBanner component displaying AdMob Test Banner
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.TEST_BANNER_AD_UNIT_ID
) {
    val context = LocalContext.current
    val adView = remember {
        try {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdBanner", "Banner ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("AdBanner", "Banner ad failed to load: ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        } catch (e: Throwable) {
            Log.w("AdBanner", "Non-fatal note instantiating AdView: ${e.message}")
            null
        }
    }

    DisposableEffect(adView) {
        onDispose {
            try {
                adView?.destroy()
            } catch (_: Throwable) {}
        }
    }

    if (adView != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(vertical = 4.dp)
                .testTag("ad_banner_container"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admob_banner_view")
            )
        }
    }
}
