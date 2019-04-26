package com.bitcoin.wallet.btc.data

enum class FeeCategory {
    /**
     * We don't care when it confirms, but it should confirm at some time. Can be days or weeks.
     */
    ECONOMIC,

    /**
     * Under normal network conditions, confirms within the next 15 minutes. Can take longer, but this should
     * be an exception. And it should not take days or weeks.
     */
    NORMAL,

    /**
     * Confirms within the next 15 minutes.
     */
    PRIORITY
}