package com.bitcoin.wallet.btc.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.repository.NetworkState
import com.bitcoin.wallet.btc.repository.Status
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_network_state.*

class NetworkViewHolder(
    itemView: View,
    private val retryCallback: () -> Unit
) : RecyclerView.ViewHolder(itemView), LayoutContainer {
    override val containerView: View?
        get() = itemView

    init {
        retryLoadingButton.setOnClickListener { retryCallback() }
    }

    fun bindTo(networkState: NetworkState?, position: Int, isShowButton: Boolean) {
        //loading and retry
        retryLoadingButton.visibility =
            if (networkState?.status == Status.FAILED && isShowButton) View.VISIBLE else View.GONE
        loadingProgressBar.visibility = if (networkState?.status == Status.RUNNING) View.VISIBLE else View.GONE
        //error message
        errorMessageTextView.visibility = if (networkState?.msg != null) View.VISIBLE else View.GONE
        networkState?.msg?.let {
            if (it.contains(":")) {
                errorMessageTextView.text = it.substring(it.indexOf(": ") + 1)
            } else {
                errorMessageTextView.text = it
            }
        }
        if (position <= 0) {
            viewState.layoutParams =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            viewState.requestLayout()
        } else {
            viewState.layoutParams =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            viewState.requestLayout()
        }
    }

    companion object {
        fun create(parent: ViewGroup, retryCallback: () -> Unit): NetworkViewHolder {
            return NetworkViewHolder(parent.inflate(R.layout.item_network_state), retryCallback)
        }
    }
}