package com.bitcoin.wallet.btc.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.fragment.app.FragmentActivity
import com.bitcoin.wallet.btc.Constants
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.btc.utils.Qr
import com.bitcoin.wallet.btc.utils.WalletUtils
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import kotlinx.android.synthetic.main.fragment_bottom_sheet_wallet_address.*
import kotlinx.android.synthetic.main.init_ads.*
import org.bitcoinj.core.Address
import org.bitcoinj.uri.BitcoinURI

class WalletAddressBottomDialog : BaseBottomSheetDialogFragment() {
    private var address: Address? = null
    private var addressLabel: String? = null
    private var bannerAdView: AdView? = null

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
    }

    override fun onDismiss(dialog: DialogInterface?) {
        bannerAdView?.destroy()
        bannerAdView = null
        super.onDismiss(dialog)
    }

    private fun loadAdView() {
        if (activity == null) {
            return
        }
        bannerAdView?.destroy()
        bannerAdView = null
        bannerAdView = AdView(activity, getString(R.string.fb_banner_barcode), AdSize.BANNER_HEIGHT_50)
        bannerAdView?.let {nonNullBannerAdView ->
            adViewContainer?.addView(nonNullBannerAdView)
            nonNullBannerAdView.loadAd()
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
            clipboard?.primaryClip = clip
        }
    }
}