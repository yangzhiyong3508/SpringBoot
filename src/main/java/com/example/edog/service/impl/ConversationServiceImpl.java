package com.example.edog.service.impl;

import com.example.edog.entity.Conversation;
import com.example.edog.mapper.ConversationMapper;
import com.example.edog.service.ConversationService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {

    @Resource
    private ConversationMapper mapper;

    @Override
    public List<Conversation> getConversationsByAccount(Long account) {
        return mapper.getConversationsByAccount(account);
    }
    @Override
    public boolean addConversation(Conversation conversation) {
        return mapper.insertConversation(conversation) > 0;
    }

    @Override
    public boolean deleteConversation(Long account, String reply) {
        return mapper.deleteByAccountAndAnswer(account, reply) > 0;
    }
}