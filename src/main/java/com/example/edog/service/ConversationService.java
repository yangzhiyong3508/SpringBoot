package com.example.edog.service;

import com.example.edog.entity.Conversation;
import java.util.List;

public interface ConversationService {
    List<Conversation> getConversationsByAccount(Long account);
    boolean addConversation(Conversation conversation);
    boolean deleteConversation(Long account, String reply);
}