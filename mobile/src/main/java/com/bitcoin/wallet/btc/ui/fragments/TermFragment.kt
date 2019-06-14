package com.bitcoin.wallet.btc.ui.fragments

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseFragment
import com.bitcoin.wallet.btc.extension.gone
import com.bitcoin.wallet.btc.ui.activitys.MainActivity
import kotlinx.android.synthetic.main.fragment_term.*

class TermFragment : BaseFragment() {

    override fun layoutRes(): Int {
        return R.layout.fragment_term
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        webView.loadUrl("file:///android_asset/index.html")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)

                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.gone()
            }
        }
        btnAccept.setOnClickListener {
            if (baseActivity() is MainActivity) {
                (baseActivity() as MainActivity).openMain()
            }
        }
        btnCancel.setOnClickListener {
            baseActivity().finish()
        }
    }
}