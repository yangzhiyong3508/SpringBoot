package com.example.edog.entity;

public class Command {
    private Long id;         // 主键
    private Long account;    // 用户账号
    private String content;  // 命令内容
    private String message;  // 发送给设备的内容

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccount() {
        return account;
    }
    public void setAccount(Long account) {
        this.account = account;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}