package com.example.edog.mapper;

import com.example.edog.entity.Account;
import java.util.List;

public interface AccountMapper {

    List<Account> selectAll();
    Account selectByAccount(Long account);
    List<Account> selectListByAccount(Long account);

    int insert(Account account);
    int updatePassword(Account account);
    int deleteByAccount(Long account);

    int updateVoice(Account account);        // ✅ 仅更新语音参数
    int updateAvatar(Account account);       // ✅ 更新头像URL
    int updateUsername(Account account);
    int updateWakeWord(Account account);     // ✅ 唤醒词单独接口
}