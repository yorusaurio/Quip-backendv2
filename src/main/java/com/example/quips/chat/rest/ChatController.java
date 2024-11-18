package com.example.quips.chat.rest;

import com.example.quips.authentication.domain.model.User;
import com.example.quips.chat.dto.SendMessageRequest;
import com.example.quips.chat.domain.model.Conversation;
import com.example.quips.chat.domain.model.Message;
import com.example.quips.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/pollUpdates")
    public DeferredResult<List<Message>> pollUpdates(@RequestParam Long userId) {
        DeferredResult<List<Message>> deferredResult = new DeferredResult<>(30000L); // Timeout de 30 segundos

        // Agregar el listener para el usuario
        chatService.addListener(userId, deferredResult);

        // Configurar la acción a realizar cuando se alcanza el timeout
        deferredResult.onTimeout(() -> deferredResult.setResult(Collections.emptyList()));

        return deferredResult;
    }

    // Crear o obtener una conversación entre dos usuarios (uno-a-uno)
    @PostMapping("/conversation")
    public ResponseEntity<Conversation> createOrGetConversation(@RequestParam Long user1Id, @RequestParam Long user2Id) {
        Conversation conversation = chatService.getOrCreateConversation(user1Id, user2Id);

        if (conversation.getMessages() == null) {
            conversation.setMessages(new ArrayList<>());  // Establecer una lista vacía si no hay mensajes
        }

        return ResponseEntity.ok(conversation);
    }

    // Enviar un mensaje
    @PostMapping("/sendMessage")
    public ResponseEntity<Message> sendMessage(@RequestBody SendMessageRequest request) {
        Message message = chatService.sendMessage(request.getSenderId(), request.getReceiverId(), request.getContent(), request.getConversationId());
        return ResponseEntity.ok(message);
    }

    // Obtener todos los mensajes de una conversación
    @GetMapping("/conversation/{conversationId}/messages")
    public ResponseEntity<List<Message>> getConversationMessages(@PathVariable Long conversationId) {
        List<Message> messages = chatService.getConversationMessages(conversationId);
        return ResponseEntity.ok(messages);
    }

    // Obtener todas las conversaciones de un usuario
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<Conversation>> getUserConversations(@PathVariable Long userId) {
        List<Conversation> conversations = chatService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    // Obtener los últimos mensajes recientes de una conversación (limitar a N mensajes)
    @GetMapping("/conversation/{conversationId}/recentMessages")
    public ResponseEntity<List<Message>> getRecentMessages(@PathVariable Long conversationId, @RequestParam int limit) {
        List<Message> recentMessages = chatService.getRecentMessages(conversationId, limit);
        return ResponseEntity.ok(recentMessages);
    }

    // Buscar mensajes por palabra clave en una conversación
    @GetMapping("/conversation/{conversationId}/searchMessages")
    public ResponseEntity<List<Message>> searchMessages(@PathVariable Long conversationId, @RequestParam String keyword) {
        List<Message> searchResults = chatService.searchMessagesInConversation(conversationId, keyword);
        return ResponseEntity.ok(searchResults);
    }

    // Marcar mensajes como leídos en una conversación para un usuario específico
    @PutMapping("/conversation/{conversationId}/markAsRead")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable Long conversationId, @RequestParam Long userId) {
        chatService.markMessagesAsRead(conversationId, userId);
        return ResponseEntity.ok().build();
    }

    // Obtener mensajes no leídos de una conversación para un usuario específico
    @GetMapping("/conversation/{conversationId}/unreadMessages")
    public ResponseEntity<List<Message>> getUnreadMessages(@PathVariable Long conversationId, @RequestParam Long userId) {
        List<Message> unreadMessages = chatService.getUnreadMessages(conversationId, userId);
        return ResponseEntity.ok(unreadMessages);
    }

    // Eliminar un mensaje específico
    @DeleteMapping("/message/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        chatService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }


    // Agregar un participante a una conversación grupal
    @PostMapping("/conversation/{conversationId}/addParticipant")
    public ResponseEntity<Conversation> addParticipantToGroup(@PathVariable Long conversationId, @RequestParam Long userId) {
        Conversation updatedConversation = chatService.addParticipantToGroup(conversationId, userId);
        return ResponseEntity.ok(updatedConversation);
    }

    // Eliminar un participante de una conversación grupal
    @DeleteMapping("/conversation/{conversationId}/removeParticipant")
    public ResponseEntity<Conversation> removeParticipantFromGroup(@PathVariable Long conversationId, @RequestParam Long userId) {
        Conversation updatedConversation = chatService.removeParticipantFromGroup(conversationId, userId);
        return ResponseEntity.ok(updatedConversation);
    }

    // Permitir que un usuario abandone una conversación grupal
    @PostMapping("/conversation/{conversationId}/leaveGroup")
    public ResponseEntity<Void> leaveGroup(@PathVariable Long conversationId, @RequestParam Long userId) {
        chatService.leaveGroup(conversationId, userId);
        return ResponseEntity.ok().build();
    }

    // Buscar usuarios por nombre o email
    @GetMapping("/user/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        List<User> users = chatService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    // Obtener información de un grupo

    @PostMapping("/createGroup")
    public ResponseEntity<Conversation> createGroup(
            @RequestParam String groupName,
            @RequestParam Long creatorId,
            @RequestParam List<Long> participantIds) {
        Conversation group = chatService.createGroup(groupName, creatorId, participantIds);
        return ResponseEntity.ok(group);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<Conversation> getGroupInfo(@PathVariable Long groupId) {
        Conversation groupInfo = chatService.getGroupInfo(groupId);
        return ResponseEntity.ok(groupInfo);
    }

    // Eliminar un grupo (solo accesible para el administrador o creador del grupo)
    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId, @RequestParam Long adminId) {
        chatService.deleteGroup(groupId, adminId);
        return ResponseEntity.noContent().build();
    }
}
