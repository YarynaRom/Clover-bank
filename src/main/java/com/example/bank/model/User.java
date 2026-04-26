package com.example.bank.model;

public class User {
    private String name;
    private String phone;

    public User(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    // Getters
    public String getName() { return name; }
    public String getPhone() { return phone; }
}