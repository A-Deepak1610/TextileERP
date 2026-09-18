package com.textile.erp.invoice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberToWordsConverter {

    private static final String[] UNITS = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private NumberToWordsConverter() {}

    public static String convertToIndianCurrencyWords(BigDecimal amount) {
        if (amount == null) {
            return "Rupees Zero Only";
        }

        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = rounded.longValue();
        int paise = rounded.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();

        StringBuilder words = new StringBuilder("Rupees ");

        if (rupees == 0) {
            words.append("Zero");
        } else {
            words.append(convertNumber(rupees));
        }

        if (paise > 0) {
            words.append(" and ").append(convertNumber(paise)).append(" Paise");
        }

        words.append(" Only");
        return words.toString().replaceAll("\\s+", " ").trim();
    }

    private static String convertNumber(long n) {
        if (n < 0) {
            return "Minus " + convertNumber(-n);
        }
        if (n == 0) {
            return "";
        }
        if (n < 20) {
            return UNITS[(int) n];
        }
        if (n < 100) {
            return TENS[(int) (n / 10)] + ((n % 10 != 0) ? " " + UNITS[(int) (n % 10)] : "");
        }
        if (n < 1000) {
            return UNITS[(int) (n / 100)] + " Hundred" + ((n % 100 != 0) ? " " + convertNumber(n % 100) : "");
        }
        if (n < 100000) {
            return convertNumber(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " + convertNumber(n % 1000) : "");
        }
        if (n < 10000000) {
            return convertNumber(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " + convertNumber(n % 100000) : "");
        }
        return convertNumber(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " + convertNumber(n % 10000000) : "");
    }
}
