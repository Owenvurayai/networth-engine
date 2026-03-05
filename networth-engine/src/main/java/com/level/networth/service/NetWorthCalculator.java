package com.level.networth.service;

import com.level.networth.model.*;
import com.level.networth.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.Map;

public class NetWorthCalculator {

    private final PricingService pricingService = new PricingService();
    private final ExchangeRateService exchangeRateService = new ExchangeRateService();
    private final CacheService cacheService = new CacheService();

    public void calculate(Customer customer) {

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        boolean estimatedReport = false;

        // Load cached prices
        Map<String, BigDecimal> cache = cacheService.loadCache();

        System.out.println("----------------------------------");
        System.out.println("User: " + customer.name);

        for (PortfolioItem item : customer.portfolio) {

            BigDecimal value = BigDecimal.ZERO;

            try {

                if (item.type == AssetType.STOCK || item.type == AssetType.CRYPTO) {

                    BigDecimal price;

                    try {
                        // Attempt live API
                        price = pricingService.getPrice(item.ticker);

                        // Save successful API result to cache
                        cache.put(item.ticker, price);
                        cacheService.saveCache(cache);

                    } catch (Exception apiError) {

                        // Fallback to cached value
                        price = cache.get(item.ticker);

                        if (price == null) {
                            System.out.println("Missing price for " + item.ticker + ". Calculation aborted.");
                            return;
                        }

                        estimatedReport = true;
                    }

                    BigDecimal localValue = MoneyUtils.multiply(item.quantity, price);

                    BigDecimal rate;

                    try {
                        rate = exchangeRateService.getRateToGBP(item.ccy);

                    } catch (Exception fxError) {

                        if ("GBP".equals(item.ccy)) {
                            rate = BigDecimal.ONE;
                        } else {
                            System.out.println("Missing FX rate for " + item.ccy + ". Calculation aborted.");
                            return;
                        }

                        estimatedReport = true;
                    }

                    value = MoneyUtils.toGBP(localValue, rate);

                } else {

                    BigDecimal rate;

                    try {
                        rate = exchangeRateService.getRateToGBP(item.ccy);

                    } catch (Exception fxError) {

                        if ("GBP".equals(item.ccy)) {
                            rate = BigDecimal.ONE;
                        } else {
                            System.out.println("Missing FX rate for " + item.ccy + ". Calculation aborted.");
                            return;
                        }

                        estimatedReport = true;
                    }

                    value = MoneyUtils.toGBP(item.valuation, rate);
                }

            } catch (Exception e) {
                System.out.println("Unexpected error occurred. Calculation aborted.");
                return;
            }

            if (item.type == AssetType.LIABILITY) {
                totalLiabilities = totalLiabilities.add(value);
            } else {
                totalAssets = totalAssets.add(value);
            }
        }

        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        System.out.println("Total Assets: £" + totalAssets);
        System.out.println("Total Liabilities: £" + totalLiabilities);
        System.out.println("Net Worth: £" + netWorth);
        System.out.println("Status: " + (estimatedReport ? "Estimated" : "Final"));
        System.out.println("----------------------------------");
    }
}