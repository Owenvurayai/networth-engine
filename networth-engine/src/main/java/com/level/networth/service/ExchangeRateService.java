package com.level.networth.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExchangeRateService {

    private final String apiKey = System.getenv("EXCHANGE_API_KEY");
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // FX cache file
    private final Path cachePath = Path.of("src/main/resources/cached_fx_rates.json");

    public BigDecimal getRateToGBP(String currency) throws Exception {

        if ("GBP".equals(currency)) {
            return BigDecimal.ONE;
        }

        try {

            // ===== LIVE API =====
            String url = "https://v6.exchangerate-api.com/v6/"
                    + apiKey + "/latest/" + currency;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = mapper.readTree(response.body());

            double rate = root
                    .get("conversion_rates")
                    .get("GBP")
                    .asDouble();

            BigDecimal result = BigDecimal.valueOf(rate);

            // ===== SAVE TO CACHE =====
            saveToCache(currency, result);

            //System.out.println("Fetched FX rate for " + currency);

            return result;

        } catch (Exception e) {

            // ===== FALLBACK =====
            BigDecimal cached = loadFromCache(currency);

            if (cached != null) {
                //System.out.println("Using cached FX rate for " + currency);
                return cached;
            }

            //System.out.println("FX rate unavailable for " + currency);
            return BigDecimal.ZERO;
        }
    }

    // =========================
    // SAVE FX RATE TO CACHE
    // =========================
    private void saveToCache(String currency, BigDecimal rate) {

        try {

            Map<String, Double> cache = new HashMap<>();

            if (Files.exists(cachePath)) {
                cache = mapper.readValue(cachePath.toFile(), Map.class);
            }

            cache.put(currency, rate.doubleValue());

            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(cachePath.toFile(), cache);

        } catch (Exception ignored) {
        }
    }

    // =========================
    // LOAD FX RATE FROM CACHE
    // =========================
    private BigDecimal loadFromCache(String currency) {

        try {

            if (!Files.exists(cachePath))
                return null;

            Map<String, Double> cache =
                    mapper.readValue(cachePath.toFile(), Map.class);

            if (cache.containsKey(currency)) {
                return BigDecimal.valueOf(cache.get(currency));
            }

        } catch (Exception ignored) {
        }

        return null;
    }
}

                    

                    

                        
                        
                        
                  
