package com.example.bank.service;

import com.example.bank.model.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

@Service
public class QueueService {
    private final Queue<ClientTicket> waitingQueue = new LinkedList<>();
    private final List<ServiceWindow> windows = new ArrayList<>();
    private int ticketCounter = 1;
    private final int AVG_SERVICE_MINUTES = 10; // Середній час на 1 людину

    public QueueService() {
        windows.add(new ServiceWindow(1, Arrays.asList(ServiceType.CASH_OPERATIONS, ServiceType.TRANSFERS)));
        windows.add(new ServiceWindow(2, Arrays.asList(ServiceType.LOANS, ServiceType.MORTGAGE)));
        windows.add(new ServiceWindow(3, Arrays.asList(ServiceType.DEPOSITS, ServiceType.BANK_CARDS, ServiceType.PAYMENTS)));
    }

    public ClientTicket addClientToQueue(ServiceType serviceType, User user) {
        // Рахуємо скільки людей вже чекають на цю послугу
        long peopleAhead = waitingQueue.stream()
                .filter(t -> t.getRequestedService() == serviceType)
                .count();

        // Розраховуємо час: зараз + (кількість людей попереду * 10 хв)
        LocalDateTime estimatedTime = LocalDateTime.now().plusMinutes(peopleAhead * AVG_SERVICE_MINUTES);

        String ticketNumber = "T-" + String.format("%03d", ticketCounter++);
        ClientTicket ticket = new ClientTicket(ticketNumber, serviceType, user, estimatedTime);

        waitingQueue.add(ticket);
        assignClientsToWindows();
        return ticket;
    }

    // Метод для табло: рахуємо очікуючих для конкретного вікна
    public Map<Integer, Long> getWaitingCountPerWindow() {
        Map<Integer, Long> counts = new HashMap<>();
        for (ServiceWindow window : windows) {
            long count = waitingQueue.stream()
                    .filter(t -> window.canHandle(t.getRequestedService()))
                    .count();
            counts.put(window.getWindowNumber(), count);
        }
        return counts;
    }

    public void assignClientsToWindows() {
        for (ServiceWindow window : windows) {
            if (window.isFree()) {
                for (ClientTicket client : waitingQueue) {
                    if (window.canHandle(client.getRequestedService())) {
                        window.setCurrentClient(client);
                        waitingQueue.remove(client);
                        break;
                    }
                }
            }
        }
    }

    public void finishServing(int windowNumber) {
        windows.stream()
                .filter(w -> w.getWindowNumber() == windowNumber && !w.isFree())
                .findFirst()
                .ifPresent(w -> {
                    w.setCurrentClient(null);
                    assignClientsToWindows();
                });
    }

    // Метод Монте-Карло для генерації потоку клієнтів
    public void generateClientsMonteCarlo(int count) {
        Random random = new Random();
        List<User> generatedUsers = new ArrayList<>();

        try {
            // 1. ЗВЕРНЕННЯ ДО ЗОВНІШНЬОГО API
            RestTemplate restTemplate = new RestTemplate();
            // Запитуємо потрібну кількість людей з України
            String url = "https://randomuser.me/api/?results=" + count + "&inc=name,phone&nat=ua";

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            for (Map<String, Object> person : results) {
                Map<String, String> nameObj = (Map<String, String>) person.get("name");
                // Беремо Ім'я та Прізвище з API
                String fullName = nameObj.get("first") + " " + nameObj.get("last") + " (API)";

                // Беремо телефон з API і очищаємо від зайвих символів
                String phone = ((String) person.get("phone")).replaceAll("[^0-9]", "");
                if (phone.length() < 10) {
                    phone = "099" + (1000000 + random.nextInt(9000000));
                }

                generatedUsers.add(new User(fullName, phone));
            }
            System.out.println("Успішно згенеровано " + count + " клієнтів через API.");

        } catch (Exception e) {
            // Якщо раптом на захисті пропаде інтернет, програма не впаде, а створить резервних
            System.out.println("Помилка API: " + e.getMessage());
            for (int i = 0; i < count; i++) {
                generatedUsers.add(new User("Резервний Клієнт " + i, "0990000000"));
            }
        }

        // 2. МЕТОД МОНТЕ-КАРЛО (Розподіл послуг)
        for (User fakeUser : generatedUsers) {
            int p = random.nextInt(100);
            com.example.bank.model.ServiceType selectedService;

            if (p < 30) {
                selectedService = com.example.bank.model.ServiceType.CASH_OPERATIONS; // 30%
            } else if (p < 50) {
                selectedService = com.example.bank.model.ServiceType.PAYMENTS; // 20%
            } else if (p < 75) {
                selectedService = com.example.bank.model.ServiceType.BANK_CARDS; // 25%
            } else if (p < 90) {
                selectedService = com.example.bank.model.ServiceType.TRANSFERS; // 15%
            } else {
                com.example.bank.model.ServiceType[] otherServices = {
                        com.example.bank.model.ServiceType.LOANS,
                        com.example.bank.model.ServiceType.MORTGAGE,
                        com.example.bank.model.ServiceType.DEPOSITS
                };
                selectedService = otherServices[random.nextInt(otherServices.length)]; // 10%
            }

            // 3. Ставимо в чергу
            addClientToQueue(selectedService, fakeUser);
        }
    }

    public List<ClientTicket> getWaitingQueue() { return new ArrayList<>(waitingQueue); }
    public List<ServiceWindow> getWindows() { return windows; }
}