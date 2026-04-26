package com.example.bank.model;

import java.util.List;

public class ServiceWindow {
    private int windowNumber;
    private List<ServiceType> supportedServices; // Послуги, які може надавати це вікно
    private ClientTicket currentClient; // Клієнт, який обслуговується зараз

    public ServiceWindow(int windowNumber, List<ServiceType> supportedServices) {
        this.windowNumber = windowNumber;
        this.supportedServices = supportedServices;
    }

    public boolean canHandle(ServiceType serviceType) {
        return supportedServices.contains(serviceType);
    }

    public boolean isFree() {
        return currentClient == null;
    }

    // Getters / Setters
    public int getWindowNumber() { return windowNumber; }
    public ClientTicket getCurrentClient() { return currentClient; }
    public void setCurrentClient(ClientTicket currentClient) { this.currentClient = currentClient; }
}