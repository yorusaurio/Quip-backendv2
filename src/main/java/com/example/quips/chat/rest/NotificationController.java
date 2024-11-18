package com.example.quips.chat.rest;

import com.example.quips.chat.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/poll")
    public DeferredResult<List<String>> pollNotifications(@RequestParam Long userId) {
        DeferredResult<List<String>> deferredResult = new DeferredResult<>(30000L); // Timeout de 30 segundos

        // Agrega el listener para el usuario en el servicio de notificaciones
        notificationService.addListener(userId, deferredResult);

        // Configurar la acción cuando expira el tiempo sin notificaciones
        deferredResult.onTimeout(() -> deferredResult.setResult(Collections.emptyList()));

        return deferredResult;
    }
}
