package com.example.edog.mapper;

import com.example.edog.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ConversationMapper {

    // 根据账号查询所有对话（按时间升序或降序）
    List<Conversation> getConversationsByAccount(@Param("account") Long account);

    // 插入新对话
    int insertConversation(Conversation conversation);

    // 删除指定账号下的某条对话
    int deleteByAccountAndAnswer(@Param("account") Long account, @Param("reply") String reply);
}