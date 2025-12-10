package com.example.edog.service.impl;

import com.example.edog.entity.Command;
import com.example.edog.mapper.CommandMapper;
import com.example.edog.service.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommandServiceImpl implements CommandService {

    @Autowired
    private CommandMapper commandMapper;

    @Override
    public List<Command> getAllCommandByAccount(String account) {
        return commandMapper.getAllCommandByAccount(account);
    }

    @Override
    public int addOrUpdateCommand(Command command) {
        Command existing = commandMapper.findByAccountAndContent(String.valueOf(command.getAccount()), command.getContent());
        if (existing != null) {
            // 已存在 → 更新 message
            return commandMapper.updateMessageByAccountAndContent(
                    String.valueOf(command.getAccount()), command.getContent(), command.getMessage());
        } else {
            // 不存在 → 插入新命令
            return commandMapper.insertCommand(command);
        }
    }

    @Override
    public int updateMessageByAccountAndContent(String account, String content, String message) {
        return commandMapper.updateMessageByAccountAndContent(account, content, message);
    }

    @Override
    public int deleteByAccountAndContent(String account, String content) {
        return commandMapper.deleteByAccountAndContent(account, content);
    }
}