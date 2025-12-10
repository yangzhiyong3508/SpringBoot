package com.example.edog.controller;

import com.example.edog.entity.Conversation;
import com.example.edog.service.ConversationService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/conversation")
@CrossOrigin // 允许前端跨域访问
public class ConversationController {

    @Resource
    private ConversationService service;

    /**
     * 获取账号的所有对话记录（新→旧）
     */
    @GetMapping("/list/{account}")
    public List<Conversation> getConversations(@PathVariable Long account) {
        return service.getConversationsByAccount(account);
    }

    /**
     * 添加新对话（自动生成id与create_time）
     */
    @PostMapping("/add")
    public String addConversation(@RequestBody Conversation conversation) {
        boolean ok = service.addConversation(conversation);
        return ok ? "success" : "fail";
    }

    /**
     * 删除指定账号的某条对话
     */
    @DeleteMapping("/delete")
    public String deleteConversation(@RequestParam Long account, @RequestParam String reply) {
        boolean ok = service.deleteConversation(account, reply);
        return ok ? "deleted" : "not found";
    }
}