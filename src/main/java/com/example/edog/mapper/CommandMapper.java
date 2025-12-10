package com.example.edog.mapper;

import com.example.edog.entity.Command;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CommandMapper {
    List<Command> getAllCommandByAccount(String account);
    Command findByAccountAndContent(String account, String content);
    int insertCommand(Command command);
    int updateMessageByAccountAndContent(String account, String content, String message);
    int deleteByAccountAndContent(String account, String content);
}