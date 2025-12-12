package com.example.edog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private String avatarUrl; 

    // ✅ SQL: vol int(10) DEFAULT '70'
    private Integer vol;      

    // ✅ SQL: username DEFAULT '用户名'
    private String username;  

    // ✅ SQL: wake_word DEFAULT '小爱同学'
    @TableField("wake_word")
    private String wakeWord;  

    // ✅ SQL: voice_id DEFAULT '...'
    @TableField("voice_id")
    private String voiceId;

    // ✅ SQL: speed_ratio float DEFAULT '1'
    @TableField("speed_ratio")
    private Double speedRatio;

    public Account() {}

    public Account(Long account, String password) {
        this.account = account;
        this.password = password;
    }
}