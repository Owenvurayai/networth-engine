// Owen-Vurayai
// Unit tests for ExchangeRateService covering four scenarios:
// GBP shortcut, live API success, network failure with cache fallback, and total failure.

package com.level.networth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private HttpClient mockClient;

    private ExchangeRateService exchangeRateService;

    private final Path testCachePath = Path.of("src/main/resources/cached_fx_rates.json");

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(mockClient);
    }

    // Helper — only stubs body() since ExchangeRateService never checks statusCode
    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(body);
        return response;
    }

    // ─────────────────────────────────────────────────────
    // TEST 1: Currency is GBP — should return 1 immediately
    // ─────────────────────────────────────────────────────
    @Test
    void getRateToGBP_currencyIsGBP_returnsOne() throws Exception {

        BigDecimal rate = exchangeRateService.getRateToGBP("GBP");

        assertEquals(BigDecimal.ONE, rate);

        // No API call should be made for GBP
        verifyNoInteractions(mockClient);
    }

    // ─────────────────────────────────────────────────────
    // TEST 2: Live API returns a valid GBP rate
    // ─────────────────────────────────────────────────────
    @Test
    void getRateToGBP_liveApiReturnsValidRate() throws Exception {

        String json = "{\"conversion_rates\":{\"GBP\":0.79}}";
        HttpResponse<String> response = mockResponse(json);
        doReturn(response).when(mockClient).send(any(HttpRequest.class), any());

        BigDecimal rate = exchangeRateService.getRateToGBP("USD");

        assertEquals(new BigDecimal("0.79"), rate);
    }

    // ─────────────────────────────────────────────────────
    // TEST 3: Network fails — should fall back to cache
    // ─────────────────────────────────────────────────────
    @Test
    void getRateToGBP_networkFails_fallsBackToCache() throws Exception {

        Files.writeString(testCachePath, "{\"USD\":0.78}");

        doThrow(new RuntimeException("Network unavailable"))
                .when(mockClient).send(any(HttpRequest.class), any());

        BigDecimal rate = exchangeRateService.getRateToGBP("USD");

        assertEquals(new BigDecimal("0.78"), rate);

        Files.deleteIfExists(testCachePath);
    }

    // ─────────────────────────────────────────────────────
    // TEST 4: Both API and cache fail — should return ZERO
    // ─────────────────────────────────────────────────────
    @Test
    void getRateToGBP_apiAndCacheBothFail_returnsZero() throws Exception {

        Files.deleteIfExists(testCachePath);

        doThrow(new RuntimeException("Network unavailable"))
                .when(mockClient).send(any(HttpRequest.class), any());

        BigDecimal rate = exchangeRateService.getRateToGBP("USD");

        assertEquals(BigDecimal.ZERO, rate);
    }
}
