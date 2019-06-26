package com.bitcoin.wallet.btc.base

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.visible
import com.facebook.ads.*
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

abstract class BaseActivity : DaggerAppCompatActivity(), NativeAdListener {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @LayoutRes
    abstract fun layoutRes(): Int
    var isDarkMode: Boolean = false
    private var mNativeBannerAd: NativeBannerAd? = null
    var mViewType: NativeBannerAdView.Type = NativeBannerAdView.Type.HEIGHT_50
    private var mAdBackgroundColor: Int = 0
    private var mTitleColor: Int = 0
    private var mLinkColor: Int = 0
    private var mContentColor: Int = 0
    private var mCtaBgColor: Int = 0

    abstract fun onActivityCreated(savedInstanceState: Bundle?)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            isDarkMode = sharedPreferences.getBoolean("dark", false)
        }
        setTheme(if (sharedPreferences.getBoolean("dark", false)) R.style.AppTheme_Dark else R.style.AppTheme_Light)
        super.onCreate(savedInstanceState)
        val layoutRes = layoutRes()
        if (layoutRes > 0) setContentView(layoutRes)
        onActivityCreated(savedInstanceState)
    }

    fun setupToolbar(title: String, menuId: Int? = null, onMenuItemClick: ((item: MenuItem) -> Unit)? = null) {
        findViewById<Toolbar?>(R.id.toolbar)?.apply {
            val titleText = findViewById<TextView?>(R.id.toolbarTitle)
            if (titleText != null) {
                titleText.text = title
            } else {
                setTitle(title)
            }
            setNavigationOnClickListener { finish() }
            menuId?.let { menuResId ->
                inflateMenu(menuResId)
                onMenuItemClick?.let { onClick ->
                    setOnMenuItemClickListener {
                        onClick.invoke(it)
                        return@setOnMenuItemClickListener true
                    }
                }
            }
        }
    }

    fun setupToolbar(resId: Int, menuId: Int? = null, onMenuItemClick: ((item: MenuItem) -> Unit)? = null) {
        setupToolbar(getString(resId), menuId, onMenuItemClick)
    }

    val application: BitcoinApplication by lazy {
        getApplication() as BitcoinApplication
    }

    fun onShowSnackbar(title: String, callbackSnack: CallbackSnack, line: Int) {
        Snackbar.make(
            findViewById(android.R.id.content),
            title,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("OK") {
                this.dismiss()
                callbackSnack.onOke()
            }
            val text = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            text.setLines(line)
            val params = this.view.layoutParams
            this.view.layoutParams = params
        }.show()
    }

    interface CallbackSnack {
        fun onOke()
    }

    override fun setShowWhenLocked(showWhenLocked: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            super.setShowWhenLocked(showWhenLocked)
        else if (showWhenLocked)
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }

    override fun onDestroy() {
        mNativeBannerAd?.destroy()
        mNativeBannerAd = null
        super.onDestroy()
    }

    fun createAndLoadNativeBannerAd(id: String) {
        mNativeBannerAd = NativeBannerAd(this, id)

        // Set a listener to get notified when the ad was loaded.
        mNativeBannerAd?.setAdListener(this)

        // Initiate a request to load an ad.
        mNativeBannerAd?.loadAd()
    }

    private fun reloadAdBannerContainer() {
        if (mNativeBannerAd != null && mNativeBannerAd?.isAdLoaded == true) {
            val mNativeAdContainer = findViewById<ViewGroup>(R.id.adViewContainer)
            mNativeAdContainer?.removeAllViews()

            when (isDarkMode) {
                false -> {
                    mAdBackgroundColor = Color.WHITE
                    mTitleColor = BaseFragment.COLOR_DARK_GRAY
                    mLinkColor = Color.WHITE
                    mContentColor = BaseFragment.COLOR_LIGHT_GRAY
                    mCtaBgColor = BaseFragment.COLOR_CTA_BLUE_BG
                }
                true -> {
                    mAdBackgroundColor = Color.BLACK
                    mTitleColor = Color.WHITE
                    mContentColor = Color.LTGRAY
                    mLinkColor = Color.BLACK
                    mCtaBgColor = Color.WHITE
                }
            }
            // Create a NativeAdViewAttributes object and set the attributes
            val attributes = NativeAdViewAttributes(this)
                .setBackgroundColor(mAdBackgroundColor)
                .setTitleTextColor(mTitleColor)
                .setDescriptionTextColor(mContentColor)
                .setButtonBorderColor(mCtaBgColor)
                .setButtonTextColor(mLinkColor)
                .setButtonColor(mCtaBgColor)

            // Use NativeAdView.render to generate the ad View
            val adView = NativeBannerAdView.render(this, mNativeBannerAd, mViewType, attributes)

            // Add adView to the container showing Ads
            mNativeAdContainer?.addView(adView, 0)
            mNativeAdContainer?.visible()
        }
    }

    override fun onAdLoaded(ad: Ad) {
        if (mNativeBannerAd != null && mNativeBannerAd == ad) {
            reloadAdBannerContainer()
        }
    }

    override fun onError(ad: Ad, error: AdError) {
        Log.e(AudienceNetworkAds.TAG, error.errorMessage)
    }

    override fun onAdClicked(ad: Ad) {

    }

    override fun onLoggingImpression(ad: Ad) {

    }

    override fun onMediaDownloaded(ad: Ad) {

    }
}