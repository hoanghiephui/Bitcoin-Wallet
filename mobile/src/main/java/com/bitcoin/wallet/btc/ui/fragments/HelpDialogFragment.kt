package com.bitcoin.wallet.btc.ui.fragments

import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.btc.extension.Bundle
import kotlinx.android.synthetic.main.dialog_help.*

class HelpDialogFragment : BaseBottomSheetDialogFragment() {
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
            fragment.show(
                activity.supportFragmentManager,
                HelpDialogFragment::class.java.simpleName
            )
        }
    }
}