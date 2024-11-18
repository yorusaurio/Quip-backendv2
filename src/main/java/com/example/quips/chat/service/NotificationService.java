package com.example.quips.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private final Map<Long, DeferredResult<List<String>>> listeners = new ConcurrentHashMap<>();

    // Método para añadir un listener para Long Polling
    public void addListener(Long userId, DeferredResult<List<String>> result) {
        listeners.put(userId, result);

        // Quitar el listener cuando la solicitud se complete o expire
        result.onCompletion(() -> listeners.remove(userId));
        result.onTimeout(() -> listeners.remove(userId));
    }

    // Método para notificar a los listeners cuando hay una actualización
    public void notifyUser(Long userId, String notificationMessage) {
        DeferredResult<List<String>> result = listeners.remove(userId);
        if (result != null) {
            result.setResult(List.of(notificationMessage));  // Envía la notificación
        }
    }

    // Método para notificar a todos los usuarios (por ejemplo, en caso de un evento global)
    public void notifyAllUsers(String notificationMessage) {
        listeners.forEach((userId, result) -> {
            result.setResult(List.of(notificationMessage));
            listeners.remove(userId); // Remover después de notificar
        });
    }
}