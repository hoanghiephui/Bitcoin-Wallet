package com.bitcoin.wallet.mobile.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.fragment.app.FragmentActivity
import com.bitcoin.wallet.mobile.Constants
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.mobile.utils.Qr
import com.bitcoin.wallet.mobile.utils.WalletUtils
import kotlinx.android.synthetic.main.fragment_bottom_sheet_wallet_address.*
import org.bitcoinj.core.Address
import org.bitcoinj.uri.BitcoinURI

class WalletAddressBottomDialog : BaseBottomSheetDialogFragment() {
    private var address: Address? = null
    private var addressLabel: String? = null
    override fun layoutRes(): Int {
        return R.layout.fragment_bottom_sheet_wallet_address
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        if (address != null) {
            val uri = BitcoinURI.convertToBitcoinURI(address, null, addressLabel, null)
            AsyncTask.execute {
                val bitmap = BitmapDrawable(resources, Qr.bitmap(uri))
                bitmap.isFilterBitmap = false
                imgQrCode.setImageDrawable(bitmap)
            }
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