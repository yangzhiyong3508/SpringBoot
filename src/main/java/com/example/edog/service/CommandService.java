package com.example.edog.service;

import com.example.edog.entity.Command;
import java.util.List;

public interface CommandService {
    List<Command> getAllCommandByAccount(String account);

    int addOrUpdateCommand(Command command);

    int updateMessageByAccountAndContent(String account, String content, String message);

    int deleteByAccountAndContent(String account, String content);
}