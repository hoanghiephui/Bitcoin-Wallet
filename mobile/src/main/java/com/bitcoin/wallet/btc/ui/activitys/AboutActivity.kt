package com.bitcoin.wallet.btc.ui.activitys

import android.os.Bundle
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.replace
import com.bitcoin.wallet.btc.ui.fragments.AboutFragment

class AboutActivity : BaseActivity() {
    override fun layoutRes(): Int {
        return R.layout.activity_setting
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Abouts")
        replace(R.id.container, AboutFragment(), AboutFragment::class.java.name)
    }
}