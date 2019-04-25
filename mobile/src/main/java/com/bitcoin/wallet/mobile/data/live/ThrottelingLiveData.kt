package com.bitcoin.wallet.mobile.data.live

import android.os.Handler
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData

abstract class ThrottelingLiveData<T> @JvmOverloads constructor(private val throttleMs: Long = DEFAULT_THROTTLE_MS) :
    LiveData<T>() {
    private val handler = Handler()
    private var lastMessageMs: Long = 0

    override fun onInactive() {
        super.onInactive()
        handler.removeCallbacksAndMessages(null)
    }

    @MainThread
    protected fun triggerLoad() {
        handler.removeCallbacksAndMessages(null)
        val runnable = Runnable {
            lastMessageMs = System.currentTimeMillis()
            load()
        }
        val lastMessageAgoMs = System.currentTimeMillis() - lastMessageMs
        if (lastMessageAgoMs < throttleMs)
            handler.postDelayed(runnable, throttleMs - lastMessageAgoMs)
        else
            runnable.run()
    }

    @MainThread
    open fun load() {
        // do nothing by default
    }

    companion object {
        private const val DEFAULT_THROTTLE_MS: Long = 500
    }
}
