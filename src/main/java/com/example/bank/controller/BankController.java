package com.example.bank.controller;

import com.example.bank.model.*;
import com.example.bank.service.QueueService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.example.bank.model.ClientTicket;
import java.util.List;
import java.util.ArrayList;

import org.springframework.web.client.RestTemplate;

@Controller
public class BankController {

    private final QueueService queueService;

    @Autowired
    public BankController(QueueService queueService) {
        this.queueService = queueService;
    }

    // 1. Головна сторінка (тепер завжди доступна)
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("services", ServiceType.values());
        return "index";
    }

    // 2. AJAX Логін (не перезавантажує сторінку)
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<String> login(@RequestParam String name, @RequestParam String phone, HttpSession session) {
        session.setAttribute("user", new User(name, phone));
        return ResponseEntity.ok("Авторизовано");
    }

    // 3. Перевірка, чи є у клієнта вже талон (щоб змінити кнопку на "Ваш талон")
    @GetMapping("/api/my-ticket")
    @ResponseBody
    public ClientTicket getMyTicket(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return null;

        // Шукаємо в черзі
        Optional<ClientTicket> inQueue = queueService.getWaitingQueue().stream()
                .filter(t -> t.getClient().getPhone().equals(user.getPhone()))
                .findFirst();
        if (inQueue.isPresent()) return inQueue.get();

        // Шукаємо у вікнах (якщо вже обслуговується)
        return queueService.getWindows().stream()
                .filter(w -> !w.isFree() && w.getCurrentClient().getClient().getPhone().equals(user.getPhone()))
                .map(ServiceWindow::getCurrentClient)
                .findFirst()
                .orElse(null);
    }

    @GetMapping("/api/state")
    @ResponseBody
    public Map<String, Object> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("windows", queueService.getWindows());

        // 1. Рахуємо людей для кожного вікна
        Map<Integer, Integer> waitingCounts = new HashMap<>();
        for (ServiceWindow window : queueService.getWindows()) {
            int wCount = 0;
            for (ClientTicket t : queueService.getWaitingQueue()) {
                if (window.canHandle(t.getRequestedService())) {
                    wCount++;
                }
            }
            waitingCounts.put(window.getWindowNumber(), wCount);
        }
        state.put("waitingCounts", waitingCounts);

        // 2. Рахуємо ВСІ послуги (навіть порожні) за допомогою LinkedHashMap
        Map<String, Integer> serviceCounts = new java.util.LinkedHashMap<>();
        for (com.example.bank.model.ServiceType type : com.example.bank.model.ServiceType.values()) {
            serviceCounts.put(type.getDescription(), 0); // Спочатку всім ставимо 0
        }

        List<Map<String, String>> nextTickets = new ArrayList<>();
        int idx = 0;

        for (ClientTicket t : queueService.getWaitingQueue()) {
            // Плюсуємо клієнта до його послуги
            String serviceName = t.getRequestedService().getDescription();
            serviceCounts.put(serviceName, serviceCounts.get(serviceName) + 1);

            // Шукаємо, які вікна можуть прийняти цього клієнта
            if (idx < 5) {
                Map<String, String> nextInfo = new HashMap<>();
                nextInfo.put("number", t.getTicketNumber());

                List<String> validWindows = new ArrayList<>();
                for (ServiceWindow w : queueService.getWindows()) {
                    if (w.canHandle(t.getRequestedService())) {
                        validWindows.add(String.valueOf(w.getWindowNumber()));
                    }
                }
                nextInfo.put("windows", String.join(", ", validWindows));

                nextTickets.add(nextInfo);
                idx++;
            }
        }

        state.put("serviceCounts", serviceCounts);
        state.put("nextTickets", nextTickets);

        return state;
    }

    // 5. Взяття талону
    @PostMapping("/api/ticket")
    @ResponseBody
    public ResponseEntity<?> takeTicket(@RequestParam String serviceName, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.badRequest().body("Будь ласка, авторизуйтесь");
        }

        boolean isAlreadyInSystem = getMyTicket(session) != null;
        if (isAlreadyInSystem) {
            return ResponseEntity.badRequest().body("Ви вже маєте активний талон!");
        }

        ServiceType type = ServiceType.valueOf(serviceName);
        ClientTicket newTicket = queueService.addClientToQueue(type, user);
        return ResponseEntity.ok(newTicket);
    }

    // 6. Завершення обслуговування працівником
    @PostMapping("/api/serve")
    @ResponseBody
    public void serveNext(@RequestParam int windowNumber) {
        queueService.finishServing(windowNumber);
    }

    // 7. Очищення сесії (щоб інша людина могла взяти талон)
    @PostMapping("/api/logout")
    @ResponseBody
    public void logout(HttpSession session) {
        session.invalidate(); // Видаляємо дані поточного користувача з пам'яті
    }

    // --- ПІДКЛЮЧЕННЯ API ПРИВАТБАНКУ ЧЕРЕЗ БЕКЕНД ---
    @GetMapping("/api/currency")
    @ResponseBody
    public String getCurrency() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Офіційне API ПриватБанку (формат JSON)
            String apiUrl = "https://api.privatbank.ua/p24api/pubinfo?json&exchange&coursid=5";
            // Сервер Java сам робить запит і повертає результат
            return restTemplate.getForObject(apiUrl, String.class);
        } catch (Exception e) {
            System.out.println("Помилка API ПриватБанку: " + e.getMessage());
            return "[]"; // Якщо немає інтернету, повертаємо порожній список
        }
    }

    // Сторінка розширеного курсу валют та конвертера
    @GetMapping("/currency")
    public String currencyPage() {
        return "currency"; // Повертає файл currency.html
    }

    // --- ПАНЕЛЬ АДМІНІСТРАТОРА ---

    // 1. Сторінка адмінки (ТЕПЕР ІЗ ЗАХИСТОМ)
    @GetMapping("/admin")
    public String adminPanel(HttpSession session) {
        // Якщо в сесії немає позначки "isAdmin", викидаємо на головну сторінку
        if (session.getAttribute("isAdmin") == null) {
            return "redirect:/";
        }
        return "admin";
    }

    // НОВИЙ МЕТОД: Перевірка логіну і пароля адміна
    @PostMapping("/api/admin/login")
    @ResponseBody
    public ResponseEntity<String> adminLogin(@RequestParam String username, @RequestParam String password, HttpSession session) {
        // Логін: admin, Пароль: 1234
        if ("admin".equals(username) && "1234".equals(password)) {
            session.setAttribute("isAdmin", true); // Даємо доступ
            return ResponseEntity.ok("Success");
        }
        return ResponseEntity.status(401).body("Невірний логін або пароль!");
    }

    // НОВИЙ МЕТОД: Вихід з адмінки
    @PostMapping("/api/admin/logout")
    @ResponseBody
    public void adminLogout(HttpSession session) {
        session.removeAttribute("isAdmin"); // Забираємо доступ
    }

    // Кнопка "Згенерувати клієнтів" (Монте-Карло)
    @PostMapping("/api/admin/generate")
    @ResponseBody
    public ResponseEntity<String> generateClients(@RequestParam int count) {
        queueService.generateClientsMonteCarlo(count);
        return ResponseEntity.ok("Згенеровано " + count + " клієнтів");
    }

    // Отримання повного журналу для таблиці
    @GetMapping("/api/admin/journal")
    @ResponseBody
    public Map<String, Object> getJournal() {
        Map<String, Object> data = new HashMap<>();
        data.put("queue", queueService.getWaitingQueue()); // Вся черга
        data.put("windows", queueService.getWindows());    // Стан вікон
        return data;
    }
}