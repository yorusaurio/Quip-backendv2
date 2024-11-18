package com.example.quips.shared.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FirebaseInitializer {

    public FirebaseInitializer() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ClassPathResource("serviceAccountKey.json").getInputStream()))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase se inicializó correctamente.");
            }
        } catch (IOException e) {
            System.err.println("Error al inicializar Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
