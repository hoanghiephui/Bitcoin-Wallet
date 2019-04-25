package com.bitcoin.wallet.mobile.ui.widget

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TopLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun onItemsAdded(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        super.onItemsAdded(recyclerView, positionStart, itemCount)
        if (positionStart == 0 && findFirstCompletelyVisibleItemPosition() <= itemCount)
            scrollToPosition(0)
    }
}