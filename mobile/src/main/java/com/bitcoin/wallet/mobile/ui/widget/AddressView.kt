package com.bitcoin.wallet.mobile.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bitcoin.wallet.mobile.R
import kotlinx.android.synthetic.main.view_address.view.*

class AddressView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
    }

    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_address, this)
    }

    fun bind(address: String) {
        txtAddress.text = address
        invalidate()
    }

    fun bindTransactionId(hash: String, withIcon: Boolean = true) {

        iconImage.visibility = View.GONE

        txtAddress.text = hash
        invalidate()
    }

}