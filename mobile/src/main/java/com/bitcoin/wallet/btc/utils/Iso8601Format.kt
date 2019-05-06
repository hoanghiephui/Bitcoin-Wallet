package com.bitcoin.wallet.btc.utils

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Iso8601Format constructor(formatString: String) : SimpleDateFormat(formatString, Locale.US) {

    init {
        timeZone = UTC
    }

    companion object {
        private val UTC = TimeZone.getTimeZone("UTC")

        fun newTimeFormat(): DateFormat {
            return Iso8601Format("HH:mm:ss")
        }

        fun newDateFormat(): DateFormat {
            return Iso8601Format("yyyy-MM-dd")
        }

        fun newDateTimeFormat(): DateFormat {
            return Iso8601Format("yyyy-MM-dd HH:mm:ss")
        }

        fun formatDateTime(date: Date): String {
            return newDateTimeFormat().format(date)
        }

        @Throws(ParseException::class)
        fun parseDateTime(source: String): Date {
            return newDateTimeFormat().parse(source)
        }

        fun newDateTimeFormatT(): DateFormat {
            return Iso8601Format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        }

        fun formatDateTimeT(date: Date): String {
            return newDateTimeFormatT().format(date)
        }

        @Throws(ParseException::class)
        fun parseDateTimeT(source: String): Date {
            return newDateTimeFormatT().parse(source)
        }
    }
}