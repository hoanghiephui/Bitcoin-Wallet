package com.bitcoin.wallet.mobile.ui.activitys

import android.content.Intent
import android.os.Bundle
import com.bitcoin.wallet.mobile.R
import com.bitcoin.wallet.mobile.base.BaseActivity
import com.bitcoin.wallet.mobile.extension.replace
import com.bitcoin.wallet.mobile.ui.fragments.MainFragment
import com.bitcoin.wallet.mobile.ui.fragments.SettingFragment

class SettingActivity : BaseActivity() {
    override fun layoutRes(): Int {
        return R.layout.activity_setting
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Settings")
        replace(R.id.container, SettingFragment(), MainFragment.TAG)
    }

    fun onRestartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(intent.apply {
            flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }
}