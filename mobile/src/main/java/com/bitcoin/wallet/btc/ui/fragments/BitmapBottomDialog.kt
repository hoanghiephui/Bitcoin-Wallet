package com.bitcoin.wallet.btc.ui.fragments

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.FragmentActivity
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BitmapBottomDialog : BottomSheetDialogFragment() {
    private var mDialog: Dialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mDialog = activity?.let { BottomSheetDialog(it, R.style.BottomDialog) }
        mDialog?.setContentView(R.layout.dialog_barcode)

        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)
        val bitmap = arguments?.getParcelable<Bitmap>("bitmap")
        val image = mDialog?.findViewById<AppCompatImageView>(R.id.imgQrCode)
        image?.setImageBitmap(bitmap)

        mDialog?.setOnShowListener {
            val bottomSheet = mDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            BottomSheetBehavior.from(bottomSheet).isFitToContents = true
        }

        return mDialog as Dialog
    }

    companion object {
        fun show(activity: FragmentActivity, bitmap: Bitmap?) {
            val fragment = BitmapBottomDialog()
            val bundle = Bundle {
                putParcelable("bitmap", bitmap)
            }
            fragment.arguments = bundle
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }
    }

}