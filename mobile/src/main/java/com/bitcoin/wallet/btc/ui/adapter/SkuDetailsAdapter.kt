package com.bitcoin.wallet.btc.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.getColorFromAttr
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.repository.localdb.AugmentedSkuDetails
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_sub.*

/**
 * This is an [AugmentedSkuDetails] adapter. It can be used anywhere there is a need to display a
 * list of AugmentedSkuDetails. In this app it's used to display both the list of subscriptions and
 * the list of in-app products.
 */
open class SkuDetailsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var skuDetailsList = emptyList<AugmentedSkuDetails>()

    override fun getItemCount() = if (skuDetailsList.isNotEmpty()) skuDetailsList.size + 1 else skuDetailsList.size

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) {
            R.layout.item_bottom_purchase
        } else {
            R.layout.item_sub
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = parent.inflate(
            R.layout.item_sub
        )
        return when (viewType) {
            R.layout.item_sub -> {
                SkuDetailsViewHolder(itemView)
            }
            else -> {
                SkuRestoreViewHolder(
                    parent.inflate(
                        R.layout.item_bottom_purchase
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_sub -> {
                if (holder is SkuDetailsViewHolder) {
                    holder.bind(getItem(position))
                }
            }
            R.layout.item_bottom_purchase -> {
                if (holder is SkuRestoreViewHolder) {

                }
            }
        }
    }

    fun getItem(position: Int) = if (skuDetailsList.isEmpty()) null else skuDetailsList[position]

    fun setSkuDetailsList(list: List<AugmentedSkuDetails>) {
        if (list != skuDetailsList) {
            skuDetailsList = list
            notifyDataSetChanged()
        }
    }

    /**
     * In the spirit of keeping simple things simple: this is a friendly way of allowing clients
     * to listen to clicks. You should consider doing this for all your other adapters.
     */
    open fun onSkuDetailsClicked(item: AugmentedSkuDetails) {
        //clients to implement for callback if needed
    }

    open fun onSkuRestoreClicked() {
        //clients to implement for callback if needed
    }

    inner class SkuRestoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
        init {
            itemView.setOnClickListener {
                onSkuRestoreClicked()
            }
        }
    }


    inner class SkuDetailsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView

        init {
            itemView.setOnClickListener {
                getItem(adapterPosition)?.let { onSkuDetailsClicked(it) }
            }
        }

        fun bind(item: AugmentedSkuDetails?) {
            item?.apply {
                itemView.apply {
                    val name = title?.substring(0, title.indexOf("("))
                    sku_title.text = name
                    sku_description.text = description
                    sku_price.text = price
                    val drawableId = getSkuDrawableId(sku, this)
                    sku_image.setImageResource(R.drawable.ic_launcher_foreground)
                    isEnabled = canPurchase
                }
            }
        }

        private fun onDisabled(enabled: Boolean, res: Context) {
            if (enabled) {
                itemView.apply {
                    sku_title.setTextColor(res.getColorFromAttr(R.attr.colorPrimary))
                    sku_description.setTextColor(res.getColorFromAttr(R.attr.colorPrimary))
                    sku_price.setTextColor(res.getColorFromAttr(R.attr.colorPrimary))
                    sku_image.colorFilter = null
                }
            } else {
                itemView.apply {
                    setBackgroundColor(res.getColorFromAttr(R.attr.colorPrimary))
                    val color = res.getColorFromAttr(R.attr.colorPrimary)
                    sku_image.setColorFilter(color)
                    sku_title.setTextColor(color)
                    sku_description.setTextColor(color)
                    sku_price.setTextColor(color)
                }
            }
        }

        /**
         * Keeping simple things simple, the icons are named after the SKUs. This way, there is no
         * need to create some elaborate system for matching icons to SKUs when displaying the
         * inventory to users. It is sufficient to do
         *
         * ```
         * sku_image.setImageResource(resources.getIdentifier(sku, "drawable", view.context.packageName))
         *
         * ```
         *
         * Alternatively, in the case where more than one SKU should match the same drawable,
         * you can check with a when{} block. In this sample app, for instance, both gold_monthly and
         * gold_yearly should match the same gold_subs_icon; so instead of keeping two copies of
         * the same icon, when{} is used to set imgName
         */
        private fun getSkuDrawableId(sku: String, view: View): Int {
            var imgName: String = when {
                sku.startsWith("gold_") -> "gold_subs_icon"
                else -> sku
            }
            val drawableId = view.resources.getIdentifier(
                imgName, "drawable",
                view.context.packageName
            )
            return drawableId
        }
    }
}