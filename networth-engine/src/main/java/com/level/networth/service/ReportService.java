// Owen-Vurayai
// Responsible for generating a plain text report file from customer net worth results.
// NetWorthCalculator handles console output — this class handles file output.
// A new timestamped report file is created each time the engine runs.

package com.level.networth.service;

import com.level.networth.model.Customer;
import com.level.networth.model.AssetType;
import com.level.networth.model.PortfolioItem;
import com.level.networth.util.MoneyUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportService {

    // Report files are saved in the reports/ directory with a timestamp in the filename
    private static final String REPORT_DIR = "reports/";

    private final PricingService pricingService;
    private final ExchangeRateService exchangeRateService;
    private final String reportFilePath;

    public ReportService() {
        this.pricingService = new PricingService();
        this.exchangeRateService = new ExchangeRateService();
        this.reportFilePath = generateReportPath();
    }

    // Constructor for testing
    public ReportService(PricingService pricingService, ExchangeRateService exchangeRateService, String reportFilePath) {
        this.pricingService = pricingService;
        this.exchangeRateService = exchangeRateService;
        this.reportFilePath = reportFilePath;
    }

    // Generates a timestamped file path e.g. reports/report_2026-03-12_08-00-00.txt
    private String generateReportPath() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return REPORT_DIR + "report_" + timestamp + ".txt";
    }

    // Entry point — writes the full report for a single customer to file
    public void writeReport(Customer customer) {

        try {

            // Create reports/ directory if it doesn't exist
            Files.createDirectories(Path.of(REPORT_DIR));

            BigDecimal totalAssets = BigDecimal.ZERO;
            BigDecimal totalLiabilities = BigDecimal.ZERO;
            boolean estimatedReport = false;
            StringBuilder report = new StringBuilder();

            report.append("----------------------------------\n");
            report.append("User: ").append(customer.name).append("\n");

            for (PortfolioItem item : customer.portfolio) {

                BigDecimal value = BigDecimal.ZERO;

                try {

                    if (item.type == AssetType.STOCK || item.type == AssetType.CRYPTO) {

                        BigDecimal price = pricingService.getPrice(item.ticker);

                        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
                            report.append("Missing price for ").append(item.ticker).append(". Calculation aborted.\n");
                            writeToFile(report.toString());
                            return;
                        }

                        if (pricingService.getIsCacheAvailable() && !pricingService.getIsApiLive()) {
                            estimatedReport = true;
                        }

                        BigDecimal localValue = MoneyUtils.multiply(item.quantity, price);
                        BigDecimal rate = getFxRate(item.ccy);

                        if (rate == null) {
                            report.append("Missing FX rate for ").append(item.ccy).append(". Calculation aborted.\n");
                            writeToFile(report.toString());
                            return;
                        }

                        value = MoneyUtils.toGBP(localValue, rate);

                    } else {

                        BigDecimal rate = getFxRate(item.ccy);

                        if (rate == null) {
                            report.append("Missing FX rate for ").append(item.ccy).append(". Calculation aborted.\n");
                            writeToFile(report.toString());
                            return;
                        }

                        value = MoneyUtils.toGBP(item.valuation, rate);
                    }

                } catch (Exception e) {
                    report.append("Unexpected error occurred. Calculation aborted.\n");
                    writeToFile(report.toString());
                    return;
                }

                if (item.type == AssetType.LIABILITY) {
                    totalLiabilities = totalLiabilities.add(value);
                } else {
                    totalAssets = totalAssets.add(value);
                }
            }

            BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

            report.append("Total Assets:      £").append(totalAssets).append("\n");
            report.append("Total Liabilities: £").append(totalLiabilities).append("\n");
            report.append("Net Worth:         £").append(netWorth).append("\n");
            report.append("Status: ").append(estimatedReport ? "Estimated" : "Final").append("\n");
            report.append("----------------------------------\n");

            writeToFile(report.toString());

        } catch (Exception e) {
            System.err.println("Report generation failed: " + e.getMessage());
        }
    }

    // Appends content to the report file — append mode so all customers go in one file
    private void writeToFile(String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFilePath, true))) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("Failed to write to report file: " + e.getMessage());
        }
    }

    private BigDecimal getFxRate(String ccy) {
        try {
            return exchangeRateService.getRateToGBP(ccy);
        } catch (Exception e) {
            if ("GBP".equals(ccy)) {
                return BigDecimal.ONE;
            }
            return null;
        }
    }

    public String getReportFilePath() {
        return reportFilePath;
    }
}
