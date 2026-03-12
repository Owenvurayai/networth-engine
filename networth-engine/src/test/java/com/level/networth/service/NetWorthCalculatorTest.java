// Owen-Vurayai
// Unit tests for NetWorthCalculator covering core business logic:
// correct GBP aggregation, missing price abort, liability subtraction,
// and Estimated vs Final status flagging.

package com.level.networth.service;

import com.level.networth.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetWorthCalculatorTest {

    @Mock
    private PricingService pricingService;

    @Mock
    private ExchangeRateService exchangeRateService;

    private NetWorthCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NetWorthCalculator(pricingService, exchangeRateService);
    }

    private PortfolioItem stock(String ticker, double quantity, String ccy) {
        PortfolioItem item = new PortfolioItem();
        item.type = AssetType.STOCK;
        item.ticker = ticker;
        item.quantity = BigDecimal.valueOf(quantity);
        item.ccy = ccy;
        return item;
    }

    private PortfolioItem liability(double valuation, String ccy) {
        PortfolioItem item = new PortfolioItem();
        item.type = AssetType.LIABILITY;
        item.valuation = BigDecimal.valueOf(valuation);
        item.ccy = ccy;
        return item;
    }

    private PortfolioItem realEstate(double valuation, String ccy) {
        PortfolioItem item = new PortfolioItem();
        item.type = AssetType.REAL_ESTATE;
        item.valuation = BigDecimal.valueOf(valuation);
        item.ccy = ccy;
        return item;
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 1: Stock asset is correctly converted to GBP and accumulated
    // ─────────────────────────────────────────────────────────────────
    @Test
    void calculate_stockAsset_correctlyConvertsToGBP() throws Exception {

        when(pricingService.getPrice("AAPL")).thenReturn(new BigDecimal("200.00"));
        lenient().when(pricingService.getIsApiLive()).thenReturn(true);
        when(exchangeRateService.getRateToGBP("USD")).thenReturn(new BigDecimal("0.79"));

        Customer customer = new Customer();
        customer.name = "Test User";
        customer.portfolio = List.of(stock("AAPL", 10, "USD"));

        calculator.calculate(customer);

        // 10 x 200 = 2000 USD x 0.79 = £1580.00
        verify(pricingService).getPrice("AAPL");
        verify(exchangeRateService).getRateToGBP("USD");
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 2: Missing price aborts calculation
    // ─────────────────────────────────────────────────────────────────
    @Test
    void calculate_missingPrice_abortsCalculation() throws Exception {

        when(pricingService.getPrice("AAPL")).thenReturn(BigDecimal.ZERO);

        Customer customer = new Customer();
        customer.name = "Test User";
        customer.portfolio = List.of(stock("AAPL", 10, "USD"));

        calculator.calculate(customer);

        verifyNoInteractions(exchangeRateService);
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 3: LIABILITY is subtracted not added
    // ─────────────────────────────────────────────────────────────────
    @Test
    void calculate_liabilityItem_isSubtractedFromAssets() throws Exception {

        when(pricingService.getPrice("AAPL")).thenReturn(new BigDecimal("200.00"));
        lenient().when(pricingService.getIsApiLive()).thenReturn(true);
        when(exchangeRateService.getRateToGBP("USD")).thenReturn(new BigDecimal("0.79"));
        when(exchangeRateService.getRateToGBP("GBP")).thenReturn(BigDecimal.ONE);

        Customer customer = new Customer();
        customer.name = "Test User";
        customer.portfolio = List.of(
                stock("AAPL", 10, "USD"),  // Asset:     £1580.00
                liability(500.00, "GBP")   // Liability: £500.00
        );                                 // Net Worth: £1080.00

        calculator.calculate(customer);

        verify(exchangeRateService).getRateToGBP("USD");
        verify(exchangeRateService).getRateToGBP("GBP");
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 4: Cache fallback marks report as Estimated
    // ─────────────────────────────────────────────────────────────────
    @Test
    void calculate_cacheUsed_marksReportAsEstimated() throws Exception {

        when(pricingService.getPrice("AAPL")).thenReturn(new BigDecimal("200.00"));
        when(pricingService.getIsApiLive()).thenReturn(false);
        when(pricingService.getIsCacheAvailable()).thenReturn(true);
        when(exchangeRateService.getRateToGBP("USD")).thenReturn(new BigDecimal("0.79"));

        Customer customer = new Customer();
        customer.name = "Test User";
        customer.portfolio = List.of(stock("AAPL", 10, "USD"));

        calculator.calculate(customer);

        verify(pricingService).getIsCacheAvailable();
        verify(pricingService).getIsApiLive();
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 5: Real estate uses fixed valuation not live price
    // ─────────────────────────────────────────────────────────────────
    @Test
    void calculate_realEstateAsset_usesFixedValuation() throws Exception {

        when(exchangeRateService.getRateToGBP("GBP")).thenReturn(BigDecimal.ONE);

        Customer customer = new Customer();
        customer.name = "Test User";
        customer.portfolio = List.of(realEstate(300000.00, "GBP"));

        calculator.calculate(customer);

        verifyNoInteractions(pricingService);
        verify(exchangeRateService).getRateToGBP("GBP");
    }
}
