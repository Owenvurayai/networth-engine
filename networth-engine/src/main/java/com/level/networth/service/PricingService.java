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

public class PricingService {

    private final String apiKey = System.getenv("CURRENCY_API_KEY");
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path cachePath = Path.of("src/main/resources/cached_prices.json");
    private boolean isCacheAvalable;
    private boolean isApiLive;

    public BigDecimal getPrice(String ticker) throws Exception {
        try {
            // ===== LIVE API CALL =====
            String url = "https://api.currencyapi.com/v3/latest?apikey="
                    + apiKey + "&base_currency=" + ticker;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            // check if the status of the response is valid 
            if (response.statusCode() == 200) {
                isApiLive=true;
                JsonNode root = mapper.readTree(response.body());
                double price = root
                        .get("data")
                        .get("USD")
                        .get("value")
                        .asDouble();

             
                BigDecimal result = BigDecimal.valueOf(price);

                // ===== SAVE TO CACHE =====
                saveToCache(ticker, result); 
                //System.out.println("Fetched live price for " + ticker);
              return result;
               
            }
             // If status is not 200, fall through to cache
            throw new RuntimeException("Unexpected status code: " + response.statusCode());

        } catch (Exception e) {
            // ===== FALLBACK TO CACHE =====
            BigDecimal cached = loadFromCache(ticker);
            if (cached != null) {
                //System.out.println("Using cached price for " + ticker);
                isCacheAvalable = true;
                return cached;
            }
            //System.out.println("No cached price for " + ticker);
            
            return BigDecimal.ZERO;
        }
         
    }

    // =========================
    // SAVE PRICE TO CACHE FILE
    // =========================
    // ✅ Fixed: removed stray Path parameter — cachePath is already a class field
    private void saveToCache(String ticker, BigDecimal price) {
        try {
            Map<String, Double> cache = new HashMap<>();
            if (Files.exists(cachePath)) {
                cache = mapper.readValue(cachePath.toFile(), Map.class);
            }
            cache.put(ticker, price.doubleValue());
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(cachePath.toFile(), cache);
        } catch (Exception ignored) {
        }
    }

    // =========================
    // LOAD PRICE FROM CACHE
    // =========================
    private BigDecimal loadFromCache(String ticker)throws Exception {
        try {
        if (!Files.exists(cachePath)){
                throw new Exception("Please connect your pc to the wifi....");
           }
            Map<String, Double> cache = mapper.readValue(cachePath.toFile(), Map.class);
            if (cache.containsKey(ticker)) {
                return BigDecimal.valueOf(cache.get(ticker));
            }

        }catch(Exception e){
          throw new Exception(e.getMessage());
        }
                   
        return null;
    }
    
    public boolean getIsCacheAvalable()
    {
     return isCacheAvalable;
    }
    
    public boolean getIsApiLive()
    {
     return isApiLive;
    }
}   