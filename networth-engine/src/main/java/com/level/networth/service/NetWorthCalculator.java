package com.level.networth.service;

import com.level.networth.model.*;
import com.level.networth.util.MoneyUtils;

import java.math.BigDecimal;

public class NetWorthCalculator {

    private final PricingService pricingService = new PricingService();
    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    // CacheService removed — PricingService now manages its own cache internally

    public void calculate(Customer customer) {

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        boolean estimatedReport = false;

        System.out.println("----------------------------------");
        System.out.println("User: " + customer.name);

        for (PortfolioItem item : customer.portfolio) {

            BigDecimal value = BigDecimal.ZERO;

            try {

                if (item.type == AssetType.STOCK || item.type == AssetType.CRYPTO) {

                    BigDecimal price = pricingService.getPrice(item.ticker);

                    // If both live API and cache failed
                    if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
                        System.out.println("Missing price for " + item.ticker + ". Calculation aborted.");
                        return;
                    }

                    // Mark as estimated if we fell back to cache instead of live API
                    if (pricingService.getIsCacheAvailable() && !pricingService.getIsApiLive()) {
                        estimatedReport = true;
                    }

                    BigDecimal localValue = MoneyUtils.multiply(item.quantity, price);

                    BigDecimal rate = getFxRate(item.ccy);

                    if (rate == null) {
                        System.out.println("Missing FX rate for " + item.ccy + ". Calculation aborted.");
                        return;
                    }

                    if (rate.compareTo(BigDecimal.ONE) != 0) {
                        // rate was fetched live or defaulted — if defaulted, mark estimated
                    }

                    value = MoneyUtils.toGBP(localValue, rate);

                } else {

                    BigDecimal rate = getFxRate(item.ccy);

                    if (rate == null) {
                        System.out.println("Missing FX rate for " + item.ccy + ". Calculation aborted.");
                        return;
                    }

                    value = MoneyUtils.toGBP(item.valuation, rate);
                }

            } catch (Exception e) {
                System.out.println("Unexpected error occurred. Calculation aborted.");
                System.err.println("Error detail: " + e.getMessage());
                return;
            }

            if (item.type == AssetType.LIABILITY) {
                totalLiabilities = totalLiabilities.add(value);
            } else {
                totalAssets = totalAssets.add(value);
            }
        }

        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        System.out.println("Total Assets:      £" + totalAssets);
        System.out.println("Total Liabilities: £" + totalLiabilities);
        System.out.println("Net Worth:         £" + netWorth);
        System.out.println("Status: " + (estimatedReport ? "Estimated" : "Final"));
        System.out.println("----------------------------------");
    }

    // ================================
    // FX RATE HELPER — GBP defaults to 1, others abort if unavailable
    // ================================
    private BigDecimal getFxRate(String ccy) {

        try {
            return exchangeRateService.getRateToGBP(ccy);
        } catch (Exception e) {
            if ("GBP".equals(ccy)) {
                return BigDecimal.ONE; // No conversion needed
            }
            return null; // Signals caller to abort
        }
    }
}