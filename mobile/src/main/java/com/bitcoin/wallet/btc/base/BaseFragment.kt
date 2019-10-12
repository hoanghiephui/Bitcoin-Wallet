package com.bitcoin.wallet.btc.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.bitcoin.wallet.btc.R
import dagger.android.support.DaggerFragment
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

abstract class BaseFragment : DaggerFragment() {
    private var disposal = CompositeDisposable()
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @LayoutRes
    abstract fun layoutRes(): Int

    abstract fun onFragmentCreated(view: View, savedInstanceState: Bundle?)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(layoutRes(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        disposal.clear()
        super.onDestroyView()
    }

    open fun onBackPressed(): Boolean = true

    fun dismiss() {
        when (parentFragment) {
            is BaseBottomSheetDialogFragment -> (parentFragment as? BaseBottomSheetDialogFragment)?.dismissDialog()
            is BaseDialogFragment -> (parentFragment as? BaseDialogFragment)?.dismissDialog()
            else -> activity?.onBackPressed()
        }
    }

    fun setupToolbar(
        title: String,
        menuId: Int? = null,
        onMenuItemClick: ((item: MenuItem) -> Unit)? = null
    ) {
        view?.findViewById<Toolbar?>(R.id.toolbar)?.apply {
            val titleText = findViewById<TextView?>(R.id.toolbarTitle)
            if (titleText != null) {
                titleText.text = title
            } else {
                setTitle(title)
            }
            setNavigationOnClickListener { dismiss() }
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

    fun setupToolbar(
        resId: Int,
        menuId: Int? = null,
        onMenuItemClick: ((item: MenuItem) -> Unit)? = null
    ) {
        setupToolbar(getString(resId), menuId, onMenuItemClick)
    }

    fun baseActivity(): BaseActivity {
        return activity as BaseActivity
    }
}