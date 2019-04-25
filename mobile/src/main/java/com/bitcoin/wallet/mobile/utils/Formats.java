package com.bitcoin.wallet.mobile.utils;

import com.bitcoin.wallet.mobile.Constants;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Formats {
    public static final Pattern PATTERN_MONETARY_SPANNABLE = Pattern.compile("(?:([\\p{Alpha}\\p{Sc}]++)\\s?+)?" // prefix
            + "([\\+\\-" + Constants.CURRENCY_PLUS_SIGN + Constants.CURRENCY_MINUS_SIGN
            + "]?+(?:\\d*+\\.\\d{0,2}+|\\d++))" // significant
            + "(\\d++)?"); // insignificant
    private static final Pattern PATTERN_MEMO = Pattern.compile(
            "(?:Payment request for Coinbase order code: (.+)|Payment request for BitPay invoice (.+) for merchant (.+))",
            Pattern.CASE_INSENSITIVE);
    public static int PATTERN_GROUP_PREFIX = 1; // optional
    public static int PATTERN_GROUP_SIGNIFICANT = 2; // mandatory
    public static int PATTERN_GROUP_INSIGNIFICANT = 3; // optional

    @Nullable
    public static String[] sanitizeMemo(final @Nullable String memo) {
        if (memo == null)
            return null;

        final Matcher m = PATTERN_MEMO.matcher(memo);
        if (m.matches() && m.group(1) != null)
            return new String[]{m.group(1) + " (via Coinbase)"};
        else if (m.matches() && m.group(2) != null)
            return new String[]{m.group(2) + " (via BitPay)", m.group(3)};
        else
            return new String[]{memo};
    }
}
