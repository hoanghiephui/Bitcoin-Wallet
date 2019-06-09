package com.bitcoin.wallet.btc.model.info

import com.google.gson.annotations.SerializedName

data class Data(

    @field:SerializedName("price_alerts_enabled")
    val priceAlertsEnabled: Boolean? = null,

    @field:SerializedName("symbol")
    val symbol: String? = null,

    @field:SerializedName("transaction_unit_price_scale")
    val transactionUnitPriceScale: Int? = null,

    @field:SerializedName("website")
    val website: String? = null,

    @field:SerializedName("color")
    val color: String? = null,

    @field:SerializedName("image_url")
    val imageUrl: String? = null,

    @field:SerializedName("address_regex")
    val addressRegex: String? = null,

    @field:SerializedName("description")
    val description: String? = null,

    @field:SerializedName("unit_price_scale")
    val unitPriceScale: Int? = null,

    @field:SerializedName("contract_address")
    val contractAddress: Any? = null,

    @field:SerializedName("uri_scheme")
    val uriScheme: String? = null,

    @field:SerializedName("resource_urls")
    val resourceUrls: List<ResourceUrlsItem>? = null,

    @field:SerializedName("listed")
    val listed: Boolean? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("asset_type")
    val assetType: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("related_assets")
    val relatedAssets: List<String>? = null,

    @field:SerializedName("asset_type_description")
    val assetTypeDescription: String? = null,

    @field:SerializedName("recently_listed")
    val recentlyListed: Boolean? = null,

    @field:SerializedName("slug")
    val slug: String? = null,

    @field:SerializedName("white_paper")
    val whitePaper: String? = null,

    @field:SerializedName("exponent")
    val exponent: Int? = null,

    @field:SerializedName("supported")
    val supported: Boolean? = null
)