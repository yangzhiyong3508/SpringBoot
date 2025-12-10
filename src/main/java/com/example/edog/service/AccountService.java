package com.example.edog.service;

import com.example.edog.entity.Account;
import java.util.List;

public interface AccountService {

    List<Account> getAllAccounts();

    List<Account> getAccountsByAccount(Long account);

    Account getAccountByAccount(Long account);

    int addAccount(Account account);

    int updatePasswordByAccount(Account account);

    int deleteAccount(Long account);

    int updateVoiceByAccount(Account account);

    /** 更新头像URL */
    int updateAvatarByAccount(Account account);

    int updateUsernameByAccount(Account account);

    int updateWakeWordByAccount(Account account);
}