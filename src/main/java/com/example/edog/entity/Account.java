package com.example.edog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@TableName("Account")
public class Account {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Long account;     // 账号
    private String password;  // 密码
    @TableField("avatar_url")
    @JsonProperty("avatarUrl")
    @JsonAlias({"avatar", "avatar_url"})
    private String avatarUrl; // 头像地址（云端）
    private Integer per;      // 音色
    private Integer spd;      // 语速
    private Integer pid;      // 语言
    private Integer vol;      // 音量
    private String username;  // 用户名
    private String wakeWord;  // 唤醒词

    public Account() {}

    public Account(Long account, String password) {
        this.account = account;
        this.password = password;
    }
}