package com.booknest.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats rupee amounts for templates as {@code ${@money.format(amount)}}:
 * "599", "1,299", "799.50". Whole amounts get no decimals, amounts with paise
 * exactly two - the same rules as {@code Book.getFormattedPrice()}.
 */
@Component("money")
public class MoneyFormatter {

    public String format(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        boolean hasPaise = normalized.stripTrailingZeros().scale() > 0;

        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(hasPaise ? 2 : 0);
        format.setMaximumFractionDigits(2);
        return format.format(normalized);
    }
}
