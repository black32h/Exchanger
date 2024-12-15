package org.example;

public class Currency {
    private String code;   // Код валюти
    private String name;   // Назва валюти
    private double rate;   // Курс обміну

    public Currency(String code, String name, double rate) {
        this.code = code;
        this.name = name;
        this.rate = rate;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public double getRate() {
        return rate;
    }
}
