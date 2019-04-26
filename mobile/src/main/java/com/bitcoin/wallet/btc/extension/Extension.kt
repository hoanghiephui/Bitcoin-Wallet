package com.bitcoin.wallet.btc.extension

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

fun Fragment.getDrawable(@DrawableRes drawableRes: Int): Drawable? =
    ContextCompat.getDrawable(requireContext(), drawableRes)

fun Context.getColorCompat(id: Int): Int = ContextCompat.getColor(this, id)

fun Context.getDrawableCompat(id: Int): Drawable? = ContextCompat.getDrawable(this, id)
fun Any.getSimpleName() = this::class.java.simpleName

fun Boolean.isTrue(body: (() -> Unit)?): Boolean {
    if (this) body?.invoke()
    return this
}

fun Boolean.isFalse(body: (() -> Unit)?): Boolean {
    if (!this) body?.invoke()
    return this
}

fun AppCompatActivity.replace(
    containerId: Int,
    fragment: Fragment,
    tag: String? = null,
    callback: ((Fragment) -> Unit)? = null
) {
    supportFragmentManager.beginTransaction()
        .replace(containerId, fragment, tag ?: fragment.getSimpleName())
        .runOnCommit {
            callback?.invoke(fragment)
        }
        .commitNow()
}