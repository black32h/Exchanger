package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MonobankRates {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static long lastMonobankRequestTime = 0;
    private static final long CACHE_DURATION = 60 * 1000; // Кеш на 60 секунд
    private static List<Map<String, Object>> cachedMonobankRates = new ArrayList<>();
    private static final String API_URL = "https://api.monobank.ua/bank/currency";

    public static List<Map<String, Object>> getRates() {
        long currentTime = System.currentTimeMillis();

        // Перевірка кешу
        if (currentTime - lastMonobankRequestTime < CACHE_DURATION && !cachedMonobankRates.isEmpty()) {
            System.out.println("Повертаємо кешовані дані Monobank.");
            return cachedMonobankRates;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // Затримка на 1 секунду, якщо минуло менше 1 сек.
            if (currentTime - lastMonobankRequestTime < 1000) {
                Thread.sleep(1000); // Затримка на 1 секунду
            }

            JsonNode jsonNode = CurrencyServer.fetchJson(API_URL);

            // Перебираємо дані та додаємо валюти з актуальними курсами
            for (JsonNode node : jsonNode) {
                int currencyCodeA = node.get("currencyCodeA").asInt();
                int currencyCodeB = node.get("currencyCodeB").asInt();

                if (currencyCodeB == 980 && node.has("rateBuy") && node.has("rateSell")
                        && !node.get("rateBuy").asText().equals("N/A")
                        && !node.get("rateSell").asText().equals("N/A")) {

                    Map<String, Object> currency = new HashMap<>();
                    currency.put("bank", "Monobank");
                    currency.put("currencyCode", currencyCodeA);
                    currency.put("rateBuy", node.get("rateBuy").asDouble());
                    currency.put("rateSell", node.get("rateSell").asDouble());
                    result.add(currency);
                }
            }

            // Оновлення кешу
            cachedMonobankRates = result;
            lastMonobankRequestTime = System.currentTimeMillis();
        } catch (IOException e) {
            System.err.println("Помилка при отриманні даних з Monobank: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Відновлення статусу переривання
            System.err.println("Запит був перерваний: " + e.getMessage());
        }
        return result;
    }
}
