// Owen-Vurayai
// Unit tests for PricingService covering all four pricing scenarios:
// live API success, market closed fallback, network failure, and total failure.

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
class PricingServiceTest {

    @Mock
    private HttpClient mockClient;

    private PricingService pricingService;

    private final Path testCachePath = Path.of("src/main/resources/cached_prices.json");

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(mockClient);
    }

    // Helper — creates a typed mock response to avoid generics mismatch
    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int statusCode, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    // ─────────────────────────────────────────────
    // TEST 1: Live API returns a valid current price
    // ─────────────────────────────────────────────
    @Test
    void getPrice_liveApiReturnsCurrentPrice() throws Exception {

        HttpResponse<String> response = mockResponse(200, "{\"c\":258.23,\"pc\":262.52}");
        doReturn(response).when(mockClient).send(any(HttpRequest.class), any());

        BigDecimal price = pricingService.getPrice("AAPL");

        assertEquals(new BigDecimal("258.23"), price);
        assertTrue(pricingService.getIsApiLive());
        assertFalse(pricingService.getIsCacheAvailable());
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST 2: Market is closed — "c" is 0.0, should fall back to "pc"
    // ──────────────────────────────────────────────────────────────────────
    @Test
    void getPrice_marketClosed_fallsBackToPreviousClose() throws Exception {

        HttpResponse<String> response = mockResponse(200, "{\"c\":0.0,\"pc\":262.52}");
        doReturn(response).when(mockClient).send(any(HttpRequest.class), any());

        BigDecimal price = pricingService.getPrice("AAPL");

        assertEquals(new BigDecimal("262.52"), price);
        assertTrue(pricingService.getIsApiLive());
        assertFalse(pricingService.getIsCacheAvailable());
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST 3: Network fails — should fall back to cache
    // ──────────────────────────────────────────────────────────────────────
    @Test
    void getPrice_networkFails_fallsBackToCache() throws Exception {

        Files.writeString(testCachePath, "{\"AAPL\":255.00}");

        doThrow(new RuntimeException("Network unavailable"))
                .when(mockClient).send(any(HttpRequest.class), any());

        BigDecimal price = pricingService.getPrice("AAPL");

        assertEquals(new BigDecimal("255.0"), price);
        assertTrue(pricingService.getIsCacheAvailable());
        assertFalse(pricingService.getIsApiLive());

        Files.deleteIfExists(testCachePath);
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST 4: Both API and cache fail — should return ZERO
    // ──────────────────────────────────────────────────────────────────────
    @Test
    void getPrice_apiAndCacheBothFail_returnsZero() throws Exception {

        Files.deleteIfExists(testCachePath);

        doThrow(new RuntimeException("Network unavailable"))
                .when(mockClient).send(any(HttpRequest.class), any());

        BigDecimal price = pricingService.getPrice("AAPL");

        assertEquals(BigDecimal.ZERO, price);
        assertFalse(pricingService.getIsApiLive());
        assertFalse(pricingService.getIsCacheAvailable());
    }
}
