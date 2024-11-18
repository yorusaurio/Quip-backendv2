package com.example.quips.chat.service;

import com.example.quips.chat.domain.model.Conversation;
import com.example.quips.chat.domain.model.ConversationParticipant;
import com.example.quips.chat.domain.model.Message;
import com.example.quips.authentication.domain.model.User;
import com.example.quips.chat.repository.ConversationRepository;
import com.example.quips.chat.repository.MessageRepository;
import com.example.quips.authentication.repository.UserRepository;
import com.example.quips.shared.util.MultipleConversationsException;
import jakarta.persistence.NonUniqueResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger LOGGER = Logger.getLogger(ChatService.class.getName());

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    // Crear o recuperar una conversación entre dos usuarios
    public Conversation getOrCreateConversation(Long user1Id, Long user2Id) {
        // Validar que los IDs de usuario no sean iguales
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("No se puede crear una conversación entre el mismo usuario");
        }

        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new IllegalArgumentException("User 1 not found"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new IllegalArgumentException("User 2 not found"));

        try {
            // Intentar encontrar todas las conversaciones directas entre estos dos usuarios
            List<Conversation> conversations = conversationRepository.findConversationsBetweenUsers(user1Id, user2Id);

            if (conversations.size() == 1) {
                // Si solo hay una conversación, devolverla
                return conversations.get(0);
            } else if (conversations.size() > 1) {
                // Registrar detalles de las conversaciones duplicadas
                LOGGER.severe("Se encontraron múltiples conversaciones para los usuarios " + user1Id + " y " + user2Id);
                for (Conversation conv : conversations) {
                    LOGGER.severe("Conversación duplicada: ID=" + conv.getId() + ", isGroup=" + conv.getIsGroup() +
                            ", participantes=" + conv.getParticipants().size());
                }

                // Lanzar una excepción personalizada con la lista de duplicados
                throw new MultipleConversationsException(
                        "Múltiples conversaciones encontradas entre los usuarios " + user1Id + " y " + user2Id, conversations);
            }

            // Crear una nueva conversación si no existe ninguna
            Conversation newConversation = new Conversation();
            newConversation.setIsGroup(false); // Asegura que es una conversación uno-a-uno
            newConversation.setGroupName(null); // Sin nombre de grupo

            // Establece los participantes en la nueva conversación
            ConversationParticipant participant1 = new ConversationParticipant();
            participant1.setUser(user1);
            participant1.setConversation(newConversation);

            ConversationParticipant participant2 = new ConversationParticipant();
            participant2.setUser(user2);
            participant2.setConversation(newConversation);

            newConversation.setParticipants(List.of(participant1, participant2));

            // Guarda y retorna la nueva conversación
            return conversationRepository.save(newConversation);
        } catch (Exception e) {
            LOGGER.severe("Error al procesar la conversación entre los usuarios " + user1Id + " y " + user2Id + ": " + e.getMessage());
            throw e;
        }
    }


    // Crear un grupo nuevo con un nombre y lista de participantes
    public Conversation createGroup(String groupName, Long creatorId, List<Long> participantIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        // Crear una nueva conversación (grupo)
        Conversation group = new Conversation();
        group.setGroupName(groupName);
        group.setIsGroup(true);  // Marcar como conversación grupal

        // Agregar el creador como administrador del grupo
        ConversationParticipant creatorParticipant = new ConversationParticipant();
        creatorParticipant.setUser(creator);
        creatorParticipant.setConversation(group);
        creatorParticipant.setIsAdmin(true);  // El creador es el administrador

        group.setParticipants(List.of(creatorParticipant));

        // Agregar otros participantes al grupo
        List<ConversationParticipant> participants = participantIds.stream().map(participantId -> {
            User user = userRepository.findById(participantId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            ConversationParticipant participant = new ConversationParticipant();
            participant.setUser(user);
            participant.setConversation(group);
            participant.setIsAdmin(false);  // Otros no son administradores
            return participant;
        }).collect(Collectors.toList());

        group.getParticipants().addAll(participants);

        // Guardar el grupo
        return conversationRepository.save(group);
    }


    // Enviar un mensaje dentro de una conversación existente o una nueva
    public Message sendMessage(Long senderId, Long receiverId, String content, Long conversationId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        Conversation conversation = (conversationId == null)
                ? getOrCreateConversation(senderId, receiverId)
                : conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setConversation(conversation);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setStatus("sent");

        Message savedMessage = messageRepository.save(message);

        // Notificar a los listeners de los usuarios de la conversación
        notifyListeners(receiverId, List.of(savedMessage));

        return savedMessage;
    }

    // Obtener todos los mensajes de una conversación en orden ascendente de timestamp
    public List<Message> getConversationMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    // Obtener los últimos N mensajes recientes de una conversación
    public List<Message> getRecentMessages(Long conversationId, int limit) {
        return messageRepository.findTop10ByConversationIdOrderByTimestampDesc(conversationId)
                .stream()
                .limit(limit)
                .toList();
    }

    // Buscar mensajes en una conversación por palabra clave
    public List<Message> searchMessagesInConversation(Long conversationId, String keyword) {
        return messageRepository.findByConversationIdAndContentContaining(conversationId, keyword);
    }

    // Marcar todos los mensajes de una conversación como leídos para un usuario específico
    public void markMessagesAsRead(Long conversationId, Long userId) {
        List<Message> unreadMessages = messageRepository.findUnreadMessagesByConversationIdAndUserId(conversationId, userId);
        unreadMessages.forEach(message -> message.setStatus("read"));
        messageRepository.saveAll(unreadMessages);
    }

    // Obtener mensajes no leídos de una conversación para un usuario específico
    public List<Message> getUnreadMessages(Long conversationId, Long userId) {
        return messageRepository.findUnreadMessagesByConversationIdAndUserId(conversationId, userId);
    }

    // Eliminar un mensaje por su ID
    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    // Obtener todas las conversaciones de un usuario (uno-a-uno y grupales)
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findAllByUserId(userId);
    }

    // Agregar un participante a una conversación grupal
    public Conversation addParticipantToGroup(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verificar si el usuario ya es participante
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(participant -> participant.getUser().getId().equals(userId));
        if (isParticipant) {
            throw new IllegalArgumentException("User is already a participant in the conversation");
        }

        ConversationParticipant newParticipant = new ConversationParticipant();
        newParticipant.setUser(user);
        newParticipant.setConversation(conversation);
        conversation.getParticipants().add(newParticipant);

        return conversationRepository.save(conversation);
    }

    // Eliminar un participante de una conversación grupal
    public Conversation removeParticipantFromGroup(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        conversation.getParticipants().removeIf(participant -> participant.getUser().getId().equals(userId));

        return conversationRepository.save(conversation);
    }

    // Permitir que un usuario abandone una conversación grupal
    public void leaveGroup(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        conversation.getParticipants().removeIf(participant -> participant.getUser().getId().equals(userId));

        conversationRepository.save(conversation);
    }

    // Buscar usuarios por nombre o email para agregar a un grupo
    public List<User> searchUsers(String query) {
        return userRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(query, query, query);
    }


    private final Map<Long, DeferredResult<List<Message>>> listeners = new ConcurrentHashMap<>();

    // Método para añadir un listener para Long Polling
    public void addListener(Long userId, DeferredResult<List<Message>> result) {
        listeners.put(userId, result);

        result.onCompletion(() -> listeners.remove(userId));
        result.onTimeout(() -> {
            result.setResult(Collections.emptyList());
            listeners.remove(userId);
        });
    }

    // Método para notificar a los listeners cuando hay un nuevo mensaje
    public void notifyListeners(Long userId, List<Message> newMessages) {
        DeferredResult<List<Message>> result = listeners.remove(userId);
        if (result != null) {
            result.setResult(newMessages);
        }
    }


    // Obtener información de un grupo
    public Conversation getGroupInfo(Long groupId) {
        return conversationRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }

    // Eliminar un grupo
    public void deleteGroup(Long groupId, Long adminId) {
        Conversation conversation = conversationRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Verificar que el usuario es el creador o administrador
        Optional<ConversationParticipant> adminParticipant = conversation.getParticipants().stream()
                .filter(participant -> participant.getUser().getId().equals(adminId) && participant.getIsAdmin())
                .findFirst();

        if (adminParticipant.isPresent()) {
            conversationRepository.delete(conversation);
        } else {
            throw new IllegalArgumentException("Only the group admin can delete the group");
        }
    }
}
