//Owen-Vurayai
// BigDecimal used throughout to avoid floating point precision loss in financial calculations
package com.level.networth.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {
    // Converts a local currency amount to GBP, rounded to 2 decimal places (standard currency precision)
    public static BigDecimal toGBP(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    // Multiplies quantity x price — rounded to 8 decimal places to preserve precision for crypto assets
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b).setScale(8, RoundingMode.HALF_UP);
    }
}
