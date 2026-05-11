package com.example.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class BankApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Встановлюємо київський час за замовчуванням для всієї Java-машини
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
    }
}

