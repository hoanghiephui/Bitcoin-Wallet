package com.bitcoin.wallet.btc.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.fragment.app.FragmentActivity
import com.bitcoin.wallet.btc.BuildConfig
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.btc.utils.Qr
import com.bitcoin.wallet.btc.utils.WalletUtils
import com.facebook.ads.*
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.fragment_bottom_sheet_wallet_address.*
import kotlinx.android.synthetic.main.init_ads.*
import org.bitcoinj.core.Address
import org.bitcoinj.uri.BitcoinURI
import java.lang.Exception
import java.util.*

class WalletAddressBottomDialog : BaseBottomSheetDialogFragment(), AdListener {
    private var address: Address? = null
    private var addressLabel: String? = null
    private var bannerAdView: AdView? = null
    private var adView: com.google.android.gms.ads.AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var interstitialAdG: com.google.android.gms.ads.InterstitialAd? = null

    override fun layoutRes(): Int {
        return R.layout.fragment_bottom_sheet_wallet_address
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        if (address != null) {
            val uri = BitcoinURI.convertToBitcoinURI(address, null, addressLabel, null)
            val bitmap = BitmapDrawable(resources, Qr.bitmap(uri))
            bitmap.isFilterBitmap = false
            imgQrCode.setImageDrawable(bitmap)
            val label = WalletUtils.formatHash(
                address?.toString(), Constants.ADDRESS_FORMAT_GROUP_SIZE,
                Constants.ADDRESS_FORMAT_LINE_SIZE, false
            )
            addressView.bind(label.toString())
            addressView.setOnClickListener {
                activity?.let { it1 ->
                    address?.toString()?.let { it2 ->
                        copyTextToClipboard(
                            it1,
                            it2
                        )
                    }
                }
                android.widget.Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show()
            }
            btnShare.setOnClickListener {
                ShareCompat.IntentBuilder.from(activity)
                    .setType("text/plain")
                    .setText(address?.toString())
                    .startChooser()
            }
        }

        loadAdView()
        loadInterstitialFb()
        btnClose.setOnClickListener {
            dismissDialog()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        bannerAdView?.destroy()
        bannerAdView = null
        adView?.destroy()
        adView = null
        showInterstitialFb()
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        interstitialAd?.let {
            if (!it.isAdLoaded) {
                it.destroy()
            }
        }
        super.onDestroy()
    }

    override fun onAdClicked(p0: Ad?) {
    }

    override fun onError(p0: Ad?, p1: AdError?) {
        loadGoogleAdView()
    }

    override fun onAdLoaded(p0: Ad?) {
    }

    override fun onLoggingImpression(p0: Ad?) {
    }

    private fun loadAdView() {
        if (activity == null) {
            return
        }
        bannerAdView?.destroy()
        bannerAdView = null
        bannerAdView = AdView(activity, getString(R.string.fb_banner_bottom_address), AdSize.BANNER_HEIGHT_50)
        bannerAdView?.let {nonNullBannerAdView ->
            adViewContainer?.addView(nonNullBannerAdView)
            nonNullBannerAdView.setAdListener(this)
            nonNullBannerAdView.loadAd()
        }
    }

    private fun loadGoogleAdView() {
        adView?.destroy()
        adView = com.google.android.gms.ads.AdView(activity)
        val adRequest = AdRequest.Builder().build()
        adView?.let {
            it.adSize = com.google.android.gms.ads.AdSize.SMART_BANNER
            it.adUnitId = getString(R.string.ads_bottom_address)
            adViewContainer?.addView(it)
            it.loadAd(adRequest)
        }
    }

    private fun loadInterstitialFb() {
        if (interstitialAd != null) {
            interstitialAd?.destroy()
            interstitialAd = null
        }
        interstitialAd = InterstitialAd(this.activity, getString(R.string.fb_full_my_address))
        interstitialAd?.setAdListener(object : InterstitialAdListener {
            override fun onInterstitialDisplayed(p0: Ad?) {

            }

            override fun onAdClicked(p0: Ad?) {
            }

            override fun onInterstitialDismissed(p0: Ad?) {
                interstitialAd?.destroy()
                interstitialAd = null
            }

            override fun onError(p0: Ad?, p1: AdError?) {
                loadInterstitialG()
            }

            override fun onAdLoaded(p0: Ad?) {
            }

            override fun onLoggingImpression(p0: Ad?) {
            }
        })
        interstitialAd?.loadAd(EnumSet.of(CacheFlag.VIDEO))
    }

    private fun showInterstitialFb() {
        if (interstitialAd != null && interstitialAd?.isAdLoaded == true) {
            try {
                interstitialAd?.show()
            }catch (ex: Exception) {
            }
        } else {
            showInterstitialG()
        }
    }

    private fun loadInterstitialG() {
        interstitialAdG = com.google.android.gms.ads.InterstitialAd(activity)
        interstitialAdG?.adUnitId =
            if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else getString(R.string.ads_full_my_address)
        interstitialAdG?.adListener = object : com.google.android.gms.ads.AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                interstitialAdG = null
            }

            override fun onAdClosed() {
                interstitialAdG = null
            }
        }
        interstitialAdG?.loadAd(AdRequest.Builder().build())
    }

    private fun showInterstitialG() {
        if (interstitialAdG?.isLoaded == true) {
            interstitialAdG?.show()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, address: Address?, addressLabel: String?) {
            val fragment = WalletAddressBottomDialog()
            fragment.address = address
            fragment.addressLabel = addressLabel
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }

        @JvmStatic
        fun copyTextToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("text", text)
            clipboard?.setPrimaryClip(clip)
        }
    }
}