package com.bitcoin.wallet.btc.base

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.R
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

abstract class BaseActivity : DaggerAppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @LayoutRes
    abstract fun layoutRes(): Int
    var isDarkMode: Boolean = false

    abstract fun onActivityCreated(savedInstanceState: Bundle?)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            isDarkMode = sharedPreferences.getBoolean("dark", false)
        }
        setTheme(if (sharedPreferences.getBoolean("dark", false)) R.style.AppTheme_Dark else R.style.AppTheme_Light)
        super.onCreate(savedInstanceState)
        val layoutRes = layoutRes()
        if (layoutRes > 0) setContentView(layoutRes)
        onActivityCreated(savedInstanceState)
    }

    fun setupToolbar(title: String, menuId: Int? = null, onMenuItemClick: ((item: MenuItem) -> Unit)? = null) {
        findViewById<Toolbar?>(R.id.toolbar)?.apply {
            val titleText = findViewById<TextView?>(R.id.toolbarTitle)
            if (titleText != null) {
                titleText.text = title
            } else {
                setTitle(title)
            }
            setNavigationOnClickListener { finish() }
            menuId?.let { menuResId ->
                inflateMenu(menuResId)
                onMenuItemClick?.let { onClick ->
                    setOnMenuItemClickListener {
                        onClick.invoke(it)
                        return@setOnMenuItemClickListener true
                    }
                }
            }
        }
    }

    fun setupToolbar(resId: Int, menuId: Int? = null, onMenuItemClick: ((item: MenuItem) -> Unit)? = null) {
        setupToolbar(getString(resId), menuId, onMenuItemClick)
    }

    val application: BitcoinApplication by lazy {
        getApplication() as BitcoinApplication
    }

    fun onShowSnackbar(title: String, callbackSnack: CallbackSnack, line: Int) {
        Snackbar.make(
            findViewById(android.R.id.content),
            title,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("OK") {
                this.dismiss()
                callbackSnack.onOke()
            }
            val text = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            text.setLines(line)
            val params = this.view.layoutParams
            this.view.layoutParams = params
        }.show()
    }

    interface CallbackSnack {
        fun onOke()
    }

    override fun setShowWhenLocked(showWhenLocked: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            super.setShowWhenLocked(showWhenLocked)
        else if (showWhenLocked)
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }
}