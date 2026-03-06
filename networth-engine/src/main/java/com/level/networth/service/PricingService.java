//Owen-Vurayai
// Fetches GBP conversion rates from the ExchangeRate API.
// Falls back to cached_fx_rates.json if the live API is unavailable.
package com.level.networth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PricingService {

    private final String apiKey = System.getenv("FINNHUB_KEY");

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final Path cachePath = Path.of("src/main/resources/cached_prices.json");

    private boolean isCacheAvailable;
    private boolean isApiLive;

    public BigDecimal getPrice(String ticker) throws Exception {

        // RESET FLAGS EACH CALL
        isApiLive = false;
        isCacheAvailable = false;

        // ── STEP 1: attempt live API — only network errors caught here ──
        HttpResponse<String> response = null;
        try {

            String url = "https://finnhub.io/api/v1/quote"
                    + "?symbol=" + ticker
                    + "&token=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception networkError) {
            //System.err.println("Network error for " + ticker + ": " + networkError.getMessage());
        }

        // ── STEP 2: parse response if we got one ──
        if (response != null && response.statusCode() == 200) {

            JsonNode root = mapper.readTree(response.body());

            // Finnhub: "c" = current price, "pc" = previous close
            // "c" returns 0.0 when market is closed, so we fall back to "pc"
            double current   = root.has("c")  ? root.get("c").asDouble()  : 0.0;
            double prevClose = root.has("pc") ? root.get("pc").asDouble() : 0.0;

            double price = current != 0.0 ? current : prevClose;

            if (price != 0.0) {

                BigDecimal result = BigDecimal.valueOf(price);

                isApiLive = true;
                saveToCache(ticker, result);

                return result;
            }

            System.err.println("No valid price in API response for: " + ticker);
        }

        // ── STEP 3: fall back to cache ──
        BigDecimal cached = loadFromCache(ticker);

        if (cached != null) {
            isCacheAvailable = true;
            return cached;
        }

        return BigDecimal.ZERO;
    }

    // =========================
    // SAVE PRICE TO CACHE FILE
    // =========================
    private void saveToCache(String ticker, BigDecimal price) {

        try {

            Map<String, Double> cache = new HashMap<>();

            if (Files.exists(cachePath)) {
                cache = mapper.readValue(cachePath.toFile(),
                        new TypeReference<Map<String, Double>>() {});
            }

            cache.put(ticker, price.doubleValue());

            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(cachePath.toFile(), cache);

        } catch (Exception e) {
            System.err.println("Cache write failed for " + ticker + ": " + e.getMessage());
        }
    }

    // =========================
    // LOAD PRICE FROM CACHE
    // =========================
    private BigDecimal loadFromCache(String ticker) {

        try {

            if (!Files.exists(cachePath)) {
                System.err.println("No cache file found. Please connect to WiFi first.");
                return null;
            }

            Map<String, Double> cache = mapper.readValue(cachePath.toFile(),
                    new TypeReference<Map<String, Double>>() {});

            if (cache.containsKey(ticker)) {
                return BigDecimal.valueOf(cache.get(ticker));
            }

        } catch (Exception e) {
            System.err.println("Cache read failed: " + e.getMessage());
        }

        return null;
    }

    public boolean getIsCacheAvailable() {
        return isCacheAvailable;
    }

    public boolean getIsApiLive() {
        return isApiLive;
    }
}