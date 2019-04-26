package com.bitcoin.wallet.btc.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView

open class ImportListener(private val passwordView: TextView, private val button: Button) : TextWatcher,
    AdapterView.OnItemSelectedListener {

    init {
        handle()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        handle()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        handle()
    }

    override fun afterTextChanged(s: Editable) {
        handle()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    private fun handle() {
        val needsPassword = needsPassword()
        val hasPassword = passwordView.text.toString().trim { it <= ' ' }.isNotEmpty()
        val hasFile = hasFile()
        button.isEnabled = hasFile && (!needsPassword || hasPassword)
    }

    protected open fun hasFile(): Boolean {
        return true
    }

    protected open fun needsPassword(): Boolean {
        return true
    }
}
