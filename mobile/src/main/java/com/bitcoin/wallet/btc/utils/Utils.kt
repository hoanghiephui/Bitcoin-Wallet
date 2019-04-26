package com.bitcoin.wallet.btc.utils

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.bitcoin.wallet.btc.R

object Utils {
    fun onOpenLink(activity: Activity, url: String, color: Int) {
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(ContextCompat.getColor(activity, color)).setShowTitle(true)
        builder.setStartAnimations(activity, R.anim.slide_in_right, R.anim.slide_out_left)
        builder.setExitAnimations(activity, R.anim.slide_in_left, R.anim.slide_out_right)
        builder.setCloseButtonIcon(
            BitmapFactory.decodeResource(activity.resources, R.drawable.ic_clear)
        )
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(activity, Uri.parse(url))
    }
}