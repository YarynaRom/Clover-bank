package com.example.bank.model;

public enum ServiceType {
    DEPOSITS("Грошові вклади (у т.ч. різні валюти)"),
    LOANS("Кредити"),
    BANK_CARDS("Банківські/кредитні картки"),
    MORTGAGE("Іпотека"),
    SAFE_BOXES("Індивідуальні сейфи"),
    PAYMENTS("Платежі"),
    CASH_OPERATIONS("Прийом та видача готівки/пенсія"),
    TRANSFERS("Грошові перекази"),
    CURRENCY_EXCHANGE("Валютно-обмінні операції");

    private final String description;

    ServiceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}