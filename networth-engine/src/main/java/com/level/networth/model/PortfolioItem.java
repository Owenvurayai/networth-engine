//Owen-Vurayai
// Represents a single item in a customer's portfolio — can be a stock, crypto, property, or liability
package com.level.networth.model;

import java.math.BigDecimal;

public class PortfolioItem {

    public AssetType type;         // Category of the asset (STOCK, CRYPTO, PROPERTY, LIABILITY)
    public String ticker;         // Market symbol used to fetch live price e.g. AAPL, BTC
    public String description;    // Human-readable label e.g. "Apple Inc.", "Bitcoin"
    public BigDecimal quantity;  // Number of units held — used for STOCK and CRYPTO
    public BigDecimal valuation;  // Fixed value — used for PROPERTY and LIABILITY
    public String ccy;            // Currency the asset is denominated in e.g. USD, EUR, GBP
}
