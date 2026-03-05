package com.level.networth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CacheService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String cacheFilePath = "src/main/resources/cached_prices.json";

    public Map<String, BigDecimal> loadCache() {
        try {
            File file = new File(cacheFilePath);

            if (!file.exists()) {
                return new HashMap<>();
            }

            return mapper.readValue(
                    file,
                    new TypeReference<Map<String, BigDecimal>>() {}
            );

        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public void saveCache(Map<String, BigDecimal> cache) {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(cacheFilePath), cache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
