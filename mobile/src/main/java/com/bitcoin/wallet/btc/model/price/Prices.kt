package com.bitcoin.wallet.btc.model.price

data class Prices(
    val hour: Hour? = null,
    val day: Hour? = null,
    val week: Hour? = null,
    val month: Hour? = null,
    val year: Hour? = null,
    val all: Hour? = null
)