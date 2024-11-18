package com.example.quips.chat.repository;

import com.example.quips.chat.domain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Obtiene todos los mensajes de una conversación ordenados por timestamp ascendente
    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);

    // Obtiene los últimos N mensajes de una conversación ordenados por timestamp descendente
    List<Message> findTop10ByConversationIdOrderByTimestampDesc(Long conversationId);

    // Busca mensajes por contenido específico dentro de una conversación
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.content LIKE %:keyword% ORDER BY m.timestamp ASC")
    List<Message> findByConversationIdAndContentContaining(Long conversationId, String keyword);

    // Obtiene mensajes sin leer para un usuario en una conversación específica
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.receiver.id = :userId AND m.status <> 'read' ORDER BY m.timestamp ASC")
    List<Message> findUnreadMessagesByConversationIdAndUserId(Long conversationId, Long userId);
}
