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

    // ✅ 更新语音参数 (只更新 vol, voice_id, speed_ratio)
    int updateVoice(Account account);

    int updateAvatar(Account account);
    int updateUsername(Account account);
    int updateWakeWord(Account account);
}