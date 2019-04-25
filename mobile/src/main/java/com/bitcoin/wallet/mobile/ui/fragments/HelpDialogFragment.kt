package com.bitcoin.wallet.mobile.ui.fragments

import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.mobile.extension.Bundle
import kotlinx.android.synthetic.main.dialog_help.*

class HelpDialogFragment: BaseBottomSheetDialogFragment() {
    override fun layoutRes(): Int {
        return R.layout.dialog_help
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val content = arguments?.getInt("content")
        tvView.text = Html.fromHtml(content?.let { getString(it) })
    }

    companion object {
        fun show(activity: AppCompatActivity, content: Int) {
            val fragment = HelpDialogFragment()
            fragment.arguments = Bundle {
                putInt("content", content)
            }
            fragment.show(activity.supportFragmentManager, HelpDialogFragment::class.java.simpleName)
        }
    }
}