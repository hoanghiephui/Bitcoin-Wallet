package com.bitcoin.wallet.btc.data.live

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.LiveData
import com.bitcoin.wallet.btc.BitcoinApplication

class ClipLiveData(application: BitcoinApplication) : LiveData<ClipData>(),
    ClipboardManager.OnPrimaryClipChangedListener {
    private val clipboardManager: ClipboardManager =
        application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override fun onActive() {
        clipboardManager.addPrimaryClipChangedListener(this)
        onPrimaryClipChanged()
    }

    override fun onInactive() {
        clipboardManager.removePrimaryClipChangedListener(this)
    }

    override fun onPrimaryClipChanged() {
        value = clipboardManager.primaryClip
    }

    fun setClipData(clipData: ClipData) {
        clipboardManager.primaryClip = clipData
    }
}