package com.bitcoin.wallet.btc.ui.activitys

import android.content.Intent
import android.os.Bundle
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.replace
import com.bitcoin.wallet.btc.ui.fragments.SettingFragment

class SettingActivity : BaseActivity() {
    override fun layoutRes(): Int {
        return R.layout.activity_setting
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Settings")
        replace(R.id.container, SettingFragment(), SettingFragment::class.java.name)
    }

    fun onRestartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(intent.apply {
            flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }
}