package com.example.bank.model;

import java.time.LocalDateTime;

public class ClientTicket {
    private String ticketNumber;
    private ServiceType requestedService;
    private LocalDateTime arrivalTime;
    private LocalDateTime estimatedServiceTime; // Орієнтовний час обслуговування
    private User client;
    private boolean isServed;

    public ClientTicket(String ticketNumber, ServiceType requestedService, User client, LocalDateTime estimatedTime) {
        this.ticketNumber = ticketNumber;
        this.requestedService = requestedService;
        this.client = client;
        this.estimatedServiceTime = estimatedTime;
        this.arrivalTime = LocalDateTime.now();
        this.isServed = false;
    }

    // Додай гетери для всіх полів (як у попередньому кроці)
    public String getTicketNumber() { return ticketNumber; }
    public ServiceType getRequestedService() { return requestedService; }
    public User getClient() { return client; }
    public LocalDateTime getEstimatedServiceTime() { return estimatedServiceTime; }
    public boolean isServed() { return isServed; }
    public void setServed(boolean served) { isServed = served; }
}