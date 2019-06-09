package com.bitcoin.wallet.btc.model.news

import com.google.gson.annotations.SerializedName

data class DataItem(

    @field:SerializedName("images")
    val images: List<ImagesItem>? = null,

    @field:SerializedName("attribution_source")
    val attributionSource: String? = null,

    @field:SerializedName("link_url")
    val linkUrl: String? = null,

    @field:SerializedName("description")
    val description: Any? = null,

    @field:SerializedName("publication_date")
    val publicationDate: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("title")
    val title: String? = null,

    @field:SerializedName("related_assets")
    val relatedAssets: List<RelatedAssetsItem>? = null
)