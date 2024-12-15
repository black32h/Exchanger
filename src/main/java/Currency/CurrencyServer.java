package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;

public class CurrencyServer {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long CACHE_DURATION = 60 * 1000; // Кеш на 60 секунд

    private static long lastMonobankRequestTime = 0;  // Час останнього запиту до API Monobank
    private static List<Map<String, Object>> cachedMonobankRates = new ArrayList<>(); // Список для кешованих курсів Monobank

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/currencies", new CurrencyHandler());
        server.setExecutor(null);
        System.out.println("Сервер запущено на порті 8080...");
        server.start();
    }

    // Метод для отримання JSON з URL
    public static JsonNode fetchJson(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        InputStream inputStream = connection.getInputStream();
        return objectMapper.readTree(inputStream);
    }

    // Обробник запитів
    static class CurrencyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Map<String, Object>> responseList = new ArrayList<>();
                responseList.addAll(getMonobankRates());  // Отримуємо курси з Monobank
                responseList.addAll(getPrivatBankRates()); // Отримуємо курси з PrivatBank

                // Перетворюємо список в JSON
                String jsonResponse = objectMapper.writeValueAsString(responseList);

                // Відправляємо відповідь
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
            } else {
                // Якщо метод не GET, відправляємо повідомлення про помилку
                String response = "Метод не підтримується.";
                exchange.sendResponseHeaders(405, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        // Метод для отримання курсів валют з Monobank з кешуванням
        private List<Map<String, Object>> getMonobankRates() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMonobankRequestTime < CACHE_DURATION && !cachedMonobankRates.isEmpty()) {
                System.out.println("Повертаємо кешовані дані Monobank.");
                return cachedMonobankRates;
            }

            List<Map<String, Object>> result = new ArrayList<>();
            try {
                String apiUrl = "https://api.monobank.ua/bank/currency";
                if (currentTime - lastMonobankRequestTime < 1000) {
                    Thread.sleep(1000);
                }

                JsonNode jsonNode = fetchJson(apiUrl);
                for (JsonNode node : jsonNode) {
                    int currencyCodeA = node.get("currencyCodeA").asInt();
                    int currencyCodeB = node.get("currencyCodeB").asInt();

                    if (currencyCodeB == 980) { // Якщо валюта - гривня (UAH)
                        Map<String, Object> currency = new HashMap<>();
                        currency.put("bank", "Monobank");
                        currency.put("currencyCode", currencyCodeA);
                        currency.put("rateBuy", parseRate(node, "rateBuy"));
                        currency.put("rateSell", parseRate(node, "rateSell"));

                        // Додаємо валюту, якщо обидва курси є
                        if (!"N/A".equals(currency.get("rateBuy")) && !"N/A".equals(currency.get("rateSell"))) {
                            result.add(currency);
                        } else {
                            System.out.println("Пропускаємо валюту з кодом: " + currencyCodeA + " через відсутність курсів.");
                        }
                    }
                }

                // Оновлення кешу
                cachedMonobankRates = result;
                lastMonobankRequestTime = System.currentTimeMillis();
            } catch (IOException | InterruptedException e) {
                System.err.println("Помилка при отриманні даних з Monobank: " + e.getMessage());
            }
            return result;
        }

        // Метод для перевірки значення курсу та обробки "N/A"
        private String parseRate(JsonNode node, String rateKey) {
            if (node.has(rateKey) && !node.get(rateKey).asText().equals("N/A")) {
                return node.get(rateKey).asText();
            }
            return "N/A";
        }

        // Метод для отримання курсів валют з PrivatBank
        private List<Map<String, Object>> getPrivatBankRates() {
            List<Map<String, Object>> result = new ArrayList<>();
            try {
                String apiUrl = "https://api.privatbank.ua/p24api/pubinfo?json&exchange&coursid=5";
                JsonNode jsonNode = fetchJson(apiUrl);

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
}
