package com.esimmcp.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detects sold-out / closed / currently-unjoinable plan signals from page text or dialogs.
 */
public final class PlanAvailabilitySignals {

    private static final Pattern CLOSED = Pattern.compile(
            "해당\\s*요금제는\\s*마감|"
                    + "요금제\\s*마감|"
                    + "마감되었|"
                    + "판매\\s*종료|"
                    + "가입\\s*불가|"
                    + "개통\\s*불가|"
                    + "일시\\s*중단|"
                    + "sold\\s*out|"
                    + "not\\s*available|"
                    + "no\\s*longer\\s*available",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private PlanAvailabilitySignals() {
    }

    public static boolean looksUnavailable(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return CLOSED.matcher(text.toLowerCase(Locale.ROOT)).find()
                || CLOSED.matcher(text).find();
    }
}
