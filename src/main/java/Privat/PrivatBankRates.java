package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PrivatBankRates {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Map<String, Object>> getRates() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String apiUrl = "https://api.privatbank.ua/p24api/pubinfo?json&exchange&coursid=5";
            JsonNode jsonNode = CurrencyServer.fetchJson(apiUrl);

            // Перебираємо отримані дані
            for (JsonNode node : jsonNode) {
                Map<String, Object> currency = new HashMap<>();
                currency.put("bank", "PrivatBank");
                currency.put("currency", node.get("ccy").asText());
                currency.put("baseCurrency", node.get("base_ccy").asText());
                currency.put("rateBuy", node.get("buy").asDouble());
                currency.put("rateSell", node.get("sale").asDouble());
                result.add(currency);
            }
        } catch (IOException e) {
            System.err.println("Помилка при отриманні даних з PrivatBank: " + e.getMessage());
        }
        return result;
    }
}
