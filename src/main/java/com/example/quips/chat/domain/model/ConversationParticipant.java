// ConversationParticipant.java
package com.example.quips.chat.domain.model;

import com.example.quips.authentication.domain.model.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conversation_participants")
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    @JsonBackReference // Rompe la recursión con Conversation
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Campo que indica si el participante es administrador
    private Boolean isAdmin = false;

    // Constructor por defecto
    public ConversationParticipant() {}

    // Constructor para establecer el participante y su rol
    public ConversationParticipant(Conversation conversation, User user, Boolean isAdmin) {
        this.conversation = conversation;
        this.user = user;
        this.isAdmin = isAdmin;
    }
}
