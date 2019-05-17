package com.bitcoin.wallet.btc.base

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.getColorFromAttr
import com.facebook.ads.*
import dagger.android.support.DaggerFragment
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

abstract class BaseFragment : DaggerFragment(), NativeAdListener {
    private var disposal = CompositeDisposable()
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var mNativeAd: NativeAd
    private var mNativeBannerAd: NativeBannerAd? = null
    private var mViewType: NativeBannerAdView.Type = NativeBannerAdView.Type.HEIGHT_100
    private var mAdBackgroundColor: Int = 0
    private var mTitleColor: Int = 0
    private var mLinkColor: Int = 0
    private var mContentColor: Int = 0
    private var mCtaBgColor: Int = 0

    @LayoutRes
    abstract fun layoutRes(): Int

    abstract fun onFragmentCreated(view: View, savedInstanceState: Bundle?)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(layoutRes(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        disposal.clear()
        super.onDestroyView()
    }

    open fun onBackPressed(): Boolean = true

    fun dismiss() {
        when (parentFragment) {
            is BaseBottomSheetDialogFragment -> (parentFragment as? BaseBottomSheetDialogFragment)?.dismissDialog()
            is BaseDialogFragment -> (parentFragment as? BaseDialogFragment)?.dismissDialog()
            else -> activity?.onBackPressed()
        }
    }

    fun setupToolbar(title: String, menuId: Int? = null, onMenuItemClick: ((item: MenuItem) -> Unit)? = null) {
        view?.findViewById<Toolbar?>(R.id.toolbar)?.apply {
            val titleText = findViewById<TextView?>(R.id.toolbarTitle)
            if (titleText != null) {
                titleText.text = title
            } else {
                setTitle(title)
            }
            setNavigationOnClickListener { dismiss() }
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

    fun baseActivity(): BaseActivity {
        return activity as BaseActivity
    }

    override fun onDestroy() {
        if (::mNativeAd.isInitialized) {
            mNativeAd.destroy()
        }
        mNativeBannerAd?.destroy()
        super.onDestroy()
    }

    fun createAndLoadNativeBannerAd(id: String) {
        mNativeBannerAd = NativeBannerAd(context, id)

        // Set a listener to get notified when the ad was loaded.
        mNativeBannerAd?.setAdListener(this)

        // Initiate a request to load an ad.
        mNativeBannerAd?.loadAd()
    }

    private fun reloadAdBannerContainer() {
        val activity = activity
        if (activity != null && mNativeBannerAd != null && mNativeBannerAd?.isAdLoaded == true) {
            val mNativeAdContainer = view?.findViewById<ViewGroup>(R.id.adViewContainers)
            mNativeAdContainer?.removeAllViews()

            when (baseActivity().isDarkMode) {
                false -> {
                    mAdBackgroundColor = Color.WHITE
                    mTitleColor = COLOR_DARK_GRAY
                    mLinkColor = Color.WHITE
                    mContentColor = COLOR_LIGHT_GRAY
                    mCtaBgColor = COLOR_CTA_BLUE_BG
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
            val attributes = NativeAdViewAttributes(context)
                .setBackgroundColor(mAdBackgroundColor)
                .setTitleTextColor(mTitleColor)
                .setDescriptionTextColor(mContentColor)
                .setButtonBorderColor(mCtaBgColor)
                .setButtonTextColor(mLinkColor)
                .setButtonColor(mCtaBgColor)

            // Use NativeAdView.render to generate the ad View
            val adView = NativeBannerAdView.render(activity, mNativeBannerAd, mViewType, attributes)

            // Add adView to the container showing Ads
            mNativeAdContainer?.addView(adView, 0)
            mNativeAdContainer?.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun createAndLoadNativeAd(unitID: String) {
        // Create a native ad request with a unique placement ID
        // (generate your own on the Facebook app settings).
        // Use different ID for each ad placement in your app.
        mNativeAd = NativeAd(activity, unitID)
        if (::mNativeAd.isInitialized) {
            // Set a listener to get notified when the ad was loaded.
            mNativeAd.setAdListener(this)

            // Initiate a request to load an ad.
            mNativeAd.loadAd()
        }
    }

    private fun reloadAdContainer() {
        val activity = activity
        if (activity != null && ::mNativeAd.isInitialized && mNativeAd.isAdLoaded) {
            val mNativeAdContainer = view?.findViewById<ViewGroup>(R.id.adViewContainer)
            mNativeAdContainer?.removeAllViews()

            // Create a NativeAdViewAttributes object and set the attributes
            val attributes = NativeAdViewAttributes(baseActivity())
                .setBackgroundColor(requireContext().getColorFromAttr(R.attr.colorPrimary))
                .setTitleTextColor(requireContext().getColorFromAttr(R.attr.colorBgItemDrawer))
                .setDescriptionTextColor(requireContext().getColorFromAttr(R.attr.colorMenu))
                .setButtonBorderColor(requireContext().getColorFromAttr(R.attr.colorAccent))
                .setButtonTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                .setButtonColor(requireContext().getColorFromAttr(R.attr.colorAccent))

            // Use NativeAdView.render to generate the ad View
            val mAdView = NativeAdView.render(activity, mNativeAd, attributes)

            mNativeAdContainer?.addView(
                mAdView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (Resources.getSystem().displayMetrics.density * DEFAULT_HEIGHT_DP).toInt()
                )
            )
        }
    }

    override fun onAdLoaded(ad: Ad) {
        if (::mNativeAd.isInitialized && mNativeAd == ad) {
            reloadAdContainer()
        }
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

    companion object {
        private const val DEFAULT_HEIGHT_DP = 350
        private const val COLOR_LIGHT_GRAY = -0x6f6b64
        private const val COLOR_DARK_GRAY = -0xb1a99b
        private const val COLOR_CTA_BLUE_BG = -0xbf7f01
    }
}