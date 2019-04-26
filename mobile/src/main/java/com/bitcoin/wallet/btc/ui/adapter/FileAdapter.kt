package com.bitcoin.wallet.btc.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.bitcoin.wallet.btc.R
import java.io.File

abstract class FileAdapter(context: Context) : ArrayAdapter<File>(context, 0) {
    protected val inflater: LayoutInflater = LayoutInflater.from(context)

    fun setFiles(files: List<File>) {
        clear()
        for (file in files)
            add(file)
    }

    override fun getView(position: Int, row: View?, parent: ViewGroup): View {
        var view = row
        val file = getItem(position)
        if (view == null)
            view = inflater.inflate(R.layout.spinner_item, parent, false)
        val textView = view?.findViewById<View>(android.R.id.text1) as TextView
        textView.text = file?.name

        return view
    }
}
