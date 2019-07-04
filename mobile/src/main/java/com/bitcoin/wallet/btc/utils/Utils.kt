package com.bitcoin.wallet.btc.utils

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.bitcoin.wallet.btc.R
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


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

    fun convertToCurrencyText(number: Double): String {
        val decimalFormat = DecimalFormat("###,###,###")
        return decimalFormat.format(number)
    }

    fun isNegative(d: Double): Boolean {
        return d.compareTo(0.0) < 0
    }

    fun cleanQueryMap(options: HashMap<String, Any>?): HashMap<String, Any> {
        if (options == null) {
            return HashMap()
        }
        val optionsCopy = HashMap(options)
        for (key in options.keys) {
            if (optionsCopy[key] == null) {
                optionsCopy.remove(key)
            }
        }
        return optionsCopy
    }

    @JvmStatic
    fun onGetTimeLong(strDate: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(strDate).time
    }

    @JvmStatic
    fun onGetDate(pattern: String): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    @JvmStatic
    fun convertDate(strDob: String, patternOut: String, patternIn: String): String {
        if (!TextUtils.isEmpty(strDob)) {
            val str = strDob.trim()
            if (TextUtils.isEmpty(str)) {
                return ""
            }
            val inSdf = SimpleDateFormat(patternIn, Locale.ENGLISH)
            val outSdf = SimpleDateFormat(patternOut, Locale.getDefault())
            val nomineeDate: Date
            nomineeDate = try {
                inSdf.parse(str)
            } catch (e: ParseException) {
                val inTime = SimpleDateFormat(patternIn, Locale.getDefault())
                inTime.parse(str)
            }
            return outSdf.format(nomineeDate)
        }
        return ""
    }
}