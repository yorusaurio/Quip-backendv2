// Message.java
package com.example.quips.chat.domain.model;

import com.example.quips.authentication.domain.model.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    @JsonBackReference // Rompe la recursión con Conversation
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private String content;

    private LocalDateTime timestamp;

    // Estado del mensaje: enviado, entregado, leído
    private String status;

    // Tipo de mensaje: texto, imagen, archivo
    private String type;

    // Indicador de mensaje eliminado
    private boolean deleted = false;

    // Fecha de creación del mensaje
    private LocalDateTime createdAt;

    // Fecha de última actualización del mensaje
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        timestamp = createdAt;  // Asignamos el mismo valor al timestamp inicial
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
