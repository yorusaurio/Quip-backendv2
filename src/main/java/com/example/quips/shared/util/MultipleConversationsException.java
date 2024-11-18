package com.example.quips.shared.util;

import com.example.quips.chat.domain.model.Conversation;

import java.util.List;

public class MultipleConversationsException extends RuntimeException {
    private final List<Conversation> duplicateConversations;

    public MultipleConversationsException(String message, List<Conversation> duplicateConversations) {
        super(message);
        this.duplicateConversations = duplicateConversations;
    }

    public List<Conversation> getDuplicateConversations() {
        return duplicateConversations;
    }
}