package com.bitcoin.wallet.btc.ui.widget

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.bitcoin.wallet.btc.R

open class DialogBuilder(context: Context) : AlertDialog.Builder(context) {
    private val customTitle: View
    private val iconView: ImageView
    private val titleView: TextView

    init {
        this.customTitle = LayoutInflater.from(context).inflate(R.layout.dialog_title, null)
        this.iconView = customTitle.findViewById(android.R.id.icon)
        this.titleView = customTitle.findViewById(android.R.id.title)
    }

    override fun setIcon(icon: Drawable?): DialogBuilder {
        if (icon != null) {
            setCustomTitle(customTitle)
            iconView.setImageDrawable(icon)
            iconView.visibility = View.VISIBLE
        }

        return this
    }

    override fun setIcon(iconResId: Int): DialogBuilder {
        if (iconResId != null) {
            setCustomTitle(customTitle)
            iconView.setImageResource(iconResId)
            iconView.visibility = View.VISIBLE
        }

        return this
    }

    override fun setTitle(title: CharSequence?): DialogBuilder {
        if (title != null) {
            setCustomTitle(customTitle)
            titleView.text = title
        }

        return this
    }

    final override fun setTitle(titleResId: Int): DialogBuilder {
        if (titleResId != 0) {
            setCustomTitle(customTitle)
            titleView.setText(titleResId)
        }

        return this
    }

    override fun setMessage(message: CharSequence?): DialogBuilder {
        super.setMessage(message)

        return this
    }

    override fun setMessage(messageResId: Int): DialogBuilder {
        super.setMessage(messageResId)

        return this
    }

    fun singleDismissButton(dismissListener: DialogInterface.OnClickListener?): DialogBuilder {
        setNeutralButton(R.string.btn_dismiss, dismissListener)

        return this
    }

    companion object {

        fun warn(context: Context, titleResId: Int): DialogBuilder {
            val builder = DialogBuilder(context)
            builder.setIcon(R.drawable.ic_warning_grey_600_24dp)
            builder.setTitle(titleResId)
            return builder
        }
    }
}
