package com.example.quips.chat.repository;

import com.example.quips.chat.domain.model.Conversation;
import com.example.quips.authentication.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Encuentra una conversación uno a uno entre dos usuarios
    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 " +
            "WHERE p1.user.id = :user1Id AND p2.user.id = :user2Id")
    Optional<Conversation> findGroupConversationWithUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 " +
            "WHERE p1.user.id = :user1Id AND p2.user.id = :user2Id")
    List<Conversation> findGroupConversationsBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 WHERE p1.user.id = :user1Id AND p2.user.id = :user2Id AND c.isGroup = false")
    List<Conversation> findConversationsBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 WHERE p1.user.id = :user1Id AND p2.user.id = :user2Id AND c.isGroup = false")
    List<Conversation> findDirectConversationBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);



    // Encuentra todas las conversaciones en las que participa un usuario
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :userId")
    List<Conversation> findByUserId(@Param("userId") Long userId);


    // Encuentra todas las conversaciones (uno-a-uno y grupales) en las que participa un usuario
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :userId")
    List<Conversation> findAllByUserId(Long userId);

    // Encuentra solo las conversaciones grupales en las que participa un usuario
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :userId GROUP BY c HAVING COUNT(p) > 2")
    List<Conversation> findGroupConversationsByUserId(@Param("userId") Long userId);

    // Encuentra conversaciones que contengan un mensaje con un contenido específico
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.messages m WHERE m.content LIKE %:keyword%")
    List<Conversation> findConversationsByMessageContent(String keyword);


}
